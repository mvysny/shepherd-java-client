package com.github.mvysny.shepherd.api

import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.*

/**
 * A very simple Kubernetes client, retrieves stuff by running the kubectl binary.
 * @property kubectl the kubectl binary
 * @property yamlConfigFolder where the kubernetes yaml config files for projects are stored. Shepherd scripts expects
 * this to be `/etc/shepherd/k8s`.
 * @property defaultDNS where the Shepherd is running, e.g. `v-herd.eu`.
 */
public class SimpleKubernetesClient @JvmOverloads constructor(
    private val kubectl: Array<String> = arrayOf("microk8s", "kubectl"),
    private val yamlConfigFolder: Path = Path("/etc/shepherd/k8s"),
    private val defaultDNS: String
) {
    /**
     * Runs `kubectl get pods` and returns all names. May return an empty list
     * if no resources are registered for given [namespace] yet.
     */
    private fun kubeCtlGetPods(namespace: String): List<String> {
        val stdout = exec(*kubectl, "get", "pods", "--namespace", namespace)
        val stdoutLines = stdout.lines().filter { it.isNotBlank() }
        if (stdoutLines.size == 1 && stdoutLines[0].startsWith("No resources found")) {
            return listOf()
        }
        return stdoutLines
            .drop(1)
            .map { it.splitByWhitespaces().first() }
    }

    private val ProjectId.kubernetesNamespace: String get() = "shepherd-${id}"
    private val Project.kubernetesNamespace: String get() = id.kubernetesNamespace

    /**
     * Returns the logs of given pod.
     */
    private fun kubeCtlGetLogs(podName: String, namespace: String): String =
        exec(*kubectl, "logs", podName, "--namespace", namespace)

    /**
     * Returns null if no main pod is registered yet. That can happen when the first Jenkins
     * build haven't finished successfullly yet.
     */
    private fun getMainPodName(id: ProjectId): String? {
        // main deployment is always called "deployment"
        val podNames = kubeCtlGetPods(id.kubernetesNamespace)
        return podNames.firstOrNull { it.startsWith("deployment-") }
    }

    /**
     * Returns run logs for given project. Returns an empty string if the pod haven't been running yet.
     */
    public fun getRunLogs(id: ProjectId): String {
        val podName = getMainPodName(id) ?: return ""
        return kubeCtlGetLogs(podName, id.kubernetesNamespace)
    }

    private fun getConfigYamlFile(id: ProjectId): Path = yamlConfigFolder / "${id.id}.yaml"

    /**
     * Removes all kubernetes resources for given project and deletes the kubernetes config yaml file for the project.
     * Doesn't throw exception if `mkctl delete -f` fails; in such case the project kubernetes config yaml file is
     * kept on the filesystem so that it can be deleted manually.
     */
    public fun deleteIfExists(id: ProjectId) {
        val f = getConfigYamlFile(id)
        if (f.exists()) {
            log.info("Deleting kubernetes objects for $f, please wait. May take up to 1 minute.")
            try {
                exec(*kubectl, "delete", "-f", f.toString())
                log.info("Deleting Kubernetes config yaml file $f")
                f.deleteExisting()
            } catch (e: Exception) {
                log.error("Failed to mkctl delete ${id.id}", e)
            }
        } else {
            log.warn("$f doesn't exist, not deleting project objects from Kubernetes")
        }
    }

    private fun kubeCtlTopPod(podName: String, namespace: String): ResourcesUsage {
        // mkctl top pod returns this:
        //POD                           CPU(cores)   MEMORY(bytes)
        //deployment-59b67fd4c5-2sdmw   2m           126Mi
        // parse and return the second line
        val stdout: String = try {
            exec(*kubectl, "top", "pod", podName, "--namespace", namespace)
        } catch (e: ExecException) {
            // if the pod is dead, this will fail with:
            // microk8s kubectl top pod deployment-66ff6f8fd8-g6b6t --namespace shepherd-viewing-plan failed with exit code 1: Error from server (NotFound): podmetrics.metrics.k8s.io "shepherd-viewing-plan/deployment-66ff6f8fd8-g6b6t" not found
            if (e.exitValue == 1 && e.output.contains("Error from server (NotFound): podmetrics.metrics.k8s.io")) {
                return ResourcesUsage.zero
            }
            throw e
        }
        val lastLine = stdout.lines().last { it.isNotBlank() } .trim()
        return parseTopPod(lastLine)
    }

    /**
     * Returns the current project CPU/memory usage. Returns zero metrics
     * if the pod haven't been running yet, or is currently stopped.
     */
    public fun getMetrics(id: ProjectId): ResourcesUsage {
        val podName = getMainPodName(id) ?: return ResourcesUsage.zero
        return kubeCtlTopPod(podName, id.kubernetesNamespace)
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
    nginx.ingress.kubernetes.io/rewrite-target: /${'$'}3
    nginx.ingress.kubernetes.io/proxy-cookie-path: / /${'$'}1
    nginx.ingress.kubernetes.io/configuration-snippet: |
      rewrite ^(/$projectId)${'$'} ${'$'}1/ permanent;
    nginx.ingress.kubernetes.io/proxy-redirect-from: https://$defaultDNS/  # Spring Security redirects to /login
    nginx.ingress.kubernetes.io/proxy-redirect-to: https://$defaultDNS/${'$'}1/
    nginx.ingress.kubernetes.io/proxy-read-timeout: ${project.publication.ingressConfig.proxyReadTimeoutSeconds}s
    nginx.ingress.kubernetes.io/client-max-body-size: ${project.publication.ingressConfig.maxBodySizeMb}m
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

        for (additionalDomain in project.publication.additionalDomains) {
            yaml += "\n" + getCustomDomainIngressYaml(additionalDomain, project.publication.https, project.id)
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
     * @param dns e.g. `v-herd.eu` or `yourdomain.com`
     */
    internal fun getCustomDomainIngressYaml(dns: String, https: Boolean, pid: ProjectId): String {
        val name = dnsToValidKubernetesIngressId(dns)
        val namespace = pid.kubernetesNamespace
        val clusterIssuer = if (https) {
            """
  annotations:
    cert-manager.io/cluster-issuer: lets-encrypt"""
        } else ""
        val tls = if (https) {
            """
  tls:
    - hosts:
      - $dns
      secretName: $name-tls"""
        } else ""

        return """
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: $name
  namespace: $namespace$clusterIssuer
spec:$tls
  rules:
    - host: "$dns"
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: service
                port:
                  number: 8080
        """.trim()
    }

    /**
     * Writes the file `/etc/shepherd/k8s/PROJECT_ID.yaml`, overwriting anything
     * that was there before.
     * @return true if a change was detected: either there was no kubernetes file before,
     * or the old contents differ from the new contents.
     */
    public fun writeConfigYamlFile(project: Project): Boolean {
        val configYamlFile = getConfigYamlFile(project.id)
        log.info("Writing Kubernetes yaml config file to $configYamlFile")
        val yaml = getKubernetesYamlConfigFile(project)
        val oldContents = if (configYamlFile.exists()) configYamlFile.readText() else ""
        configYamlFile.writeText(yaml)
        return oldContents != yaml
    }

    /**
     * Returns the current docker image from which the main app pod is running, e.g.
     * `localhost:32000/shepherd/vaadin-boot-example-gradle@sha256:39ae60d59434a3acb8b6c559ec40efb14ee62177996965418aa9b1f946d514ab`.
     * Returns null if the main app pod has not been deployed yet.
     */
    public fun getCurrentDockerImage(projectId: ProjectId): String? {
        val podName = getMainPodName(projectId) ?: return null
        val image = exec(*kubectl, "get", "pod", podName, "--namespace", projectId.kubernetesNamespace,
            "-o", "jsonpath='{.spec.containers[*].image}'")
        return image.takeIf { it.isNotBlank() }
    }

    public companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(SimpleKubernetesClient::class.java)

        private val notAllowedIdChars = "[^0-9a-z]".toRegex()
        internal fun dnsToValidKubernetesIngressId(dns: String): String {
            val result = dns.lowercase().replace(notAllowedIdChars, "-")
            return "ingress-${result.trimEnd('-')}"
        }

        /**
         * Parses the line produced by the `mkctl top pod` command. Example:
         * ```
         * deployment-59b67fd4c5-2sdmw   2m           126Mi
         * ```
         */
        internal fun parseTopPod(line: String): ResourcesUsage {
            val values = line.trim().splitByWhitespaces()
            require(values.size == 3) { "Invalid top line: '$line', parsed: $values" }
            return ResourcesUsage(
                memoryMb = values[2].removeSuffix("Mi").toInt(),
                cpu = values[1].removeSuffix("m").toFloat() / 1000
            )
        }
    }
}
