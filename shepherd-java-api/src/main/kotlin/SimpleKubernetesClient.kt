package com.github.mvysny.shepherd.api

import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.writeText

/**
 * A very simple Kubernetes client, retrieves stuff by running the kubectl binary.
 * @property kubectl the kubectl binary
 * @property yamlConfigFolder where the kubernetes yaml config files for projects are stored. Shepherd scripts expects
 * this to be `/etc/shepherd/k8s`.
 * @property defaultDNS where the Shepherd is running, defaults to `v-herd.eu`.
 */
public class SimpleKubernetesClient @JvmOverloads constructor(
    private val kubectl: Array<String> = arrayOf("microk8s", "kubectl"),
    private val yamlConfigFolder: Path = Path("/etc/shepherd/k8s"),
    private val defaultDNS: String = "v-herd.eu"
) {
    /**
     * Runs `kubectl get pods` and returns all names.
     */
    private fun kubeCtlGetPods(namespace: String): List<String> {
        val stdout = exec(*kubectl, "get", "pods", "--namespace", namespace)
        return stdout.lines()
            .drop(1)
            .map { it.split(' ').first() }
    }

    private val ProjectId.kubernetesNamespace: String get() = "shepherd-${id}"
    private val Project.kubernetesNamespace: String get() = id.kubernetesNamespace

    /**
     * Returns the logs of given pod.
     */
    private fun kubeCtlGetLogs(podName: String, namespace: String): String =
        exec(*kubectl, "logs", podName, "--namespace", namespace)

    public fun getRunLogs(id: ProjectId): String {
        // main deployment is always called "deployment"
        val podNames = kubeCtlGetPods(id.kubernetesNamespace)
        val podName = podNames.firstOrNull { it.startsWith("deployment-") }
        requireNotNull(podName) { "No deployment pod for ${id.id}: $podNames" }

        return kubeCtlGetLogs(podName, id.kubernetesNamespace)
    }

    private fun getConfigYamlFile(id: ProjectId): Path = yamlConfigFolder / "${id.id}.yaml"

    public fun deleteIfExists(id: ProjectId) {
        val f = getConfigYamlFile(id)
        if (f.exists()) {
            exec(*kubectl, "delete", "-f", f.toString())
        } else {
            log.warn("$f doesn't exist, not deleting project objects from Kubernetes")
        }
    }

    /**
     * Creates new Kubernetes YAML config file for the [project].
     *
     * The value of [imageAndHash] depends on your scenario:
     *
     * * If this file is going to be written to [yamlConfigFolder] then use the default value of `"<<IMAGE_AND_HASH>>"`.
     *   The way this works is that Jenkins will, after the project has been built, call the
     *   `shepherd-build` script will calls the `shepherd-apply` script, which then copies the YAML file, updates the IMAGE_AND_HASH
     *   placeholder with the actual image+hash uploaded in Kubernetes registry and calls `mkctl apply -f`.
     * * However, if you want to update the app configuration quickly without launching Jenkins build, then
     *   you can skip the Jenkins build and the script invocation, write this file to a temp folder and `mkctl apply -f` it yourself,
     *   to restart the project quickly.
     *
     * @param imageAndHash which Docker image+hash to use, either `"<<IMAGE_AND_HASH>>"` or
     * a valid Docker image+hash uploaded in Kubernetes registry, e.g. `localhost:32000/test/vaadin-boot-example-gradle@sha256:62d6de89ced35ed07571fb9190227b08a7a10c80c97eccfa69b1aa5829f44b9a`
     *
     */
    internal fun getKubernetesYamlConfigFile(project: Project, imageAndHash: String = "<<IMAGE_AND_HASH>>"): String {
        val projectId: String = project.id.id
        val namespace = project.kubernetesNamespace
        val maxMemory = "${project.runtime.resources.memoryMb}Mi"
        val maxCpu = "${(project.runtime.resources.cpu * 1000).toInt()}m"
        val env = if (project.runtime.envVars.isNotEmpty()) {
            "\n        env:\n" + project.runtime.envVars.entries.joinToString("\n") { (k, v) -> "        - name: $k\n          value: \"$v\"" }
        } else {
            ""
        }

        var yaml = """
#
# Microk8s resource config file for $projectId
#

apiVersion: v1
kind: Namespace
metadata:
  name: $namespace
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: deployment
  namespace: $namespace
spec:
  selector:
    matchLabels:
      app: pod
  template:
    metadata:
      labels:
        app: pod
    spec:
      containers:
      - name: main
        image: $imageAndHash$env
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "64Mi"
            cpu: 0
          limits:
            memory: "$maxMemory"  # https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/
            cpu: "$maxCpu"
---
apiVersion: v1
kind: Service
metadata:
  name: service
  namespace: $namespace
spec:
  selector:
    app: pod
  ports:
    - port: 8080
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ingress-main
  namespace: $namespace
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /\${'$'}3
    nginx.ingress.kubernetes.io/proxy-cookie-path: / /\${'$'}1
    nginx.ingress.kubernetes.io/configuration-snippet: |
      rewrite ^(/$projectId)\${'$'} \${'$'}1/ permanent;
spec:
  tls:
  - hosts:
    - $defaultDNS
  rules:
    - host: $defaultDNS
      http:
        paths:
          - path: /($projectId)(/|${'$'})(.*)
            pathType: Prefix
            backend:
              service:
                name: service
                port:
                  number: 8080
        """.trim()

        for (service in project.additionalServices) {
            yaml += "\n" + getPostgresqlYaml(service, project)
        }

        return yaml
    }

    private fun getPostgresqlYaml(service: Service, project: Project): String {
        require(service.type == ServiceType.Postgres)
        val namespace = project.kubernetesNamespace

        return """
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: $namespace
spec:
  accessModes: [ReadWriteOnce]
  resources: { requests: { storage: 512Mi } }
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgresql-deployment
  namespace: $namespace
spec:
  selector:
    matchLabels:
      app: postgres-pod
  template:
    metadata:
      labels:
        app: postgres-pod
    spec:
      volumes:
        - name: postgres-vol
          persistentVolumeClaim:
            claimName: postgres-pvc
      containers:
        - name: postgresql
          image: postgres:15.2
          ports:
            - containerPort: 5432
          env:
            - name: POSTGRES_PASSWORD
              value: mysecretpassword
          resources:
            requests:
              memory: "2Mi"
              cpu: 0
            limits:
              memory: "128Mi"
              cpu: "500m"
          volumeMounts:
            - name: postgres-vol
              mountPath: /var/lib/postgresql/data
---
apiVersion: v1
kind: Service
metadata:
  name: postgres-service  # this will also be the DNS name of the VM running this service.
  namespace: $namespace
spec:
  selector:
    app: postgres-pod
  ports:
    - port: 5432
        """.trim()
    }

    /**
     * Writes the file `/etc/shepherd/k8s/PROJECT_ID.yaml`, overwriting anything
     * that was there before.
     */
    public fun writeConfigYamlFile(project: Project) {
        val yaml = getKubernetesYamlConfigFile(project)
        getConfigYamlFile(project.id).writeText(yaml)
    }

    public companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(SimpleKubernetesClient::class.java)
    }
}
