package com.github.mvysny.shepherd.api

import com.github.mvysny.dynatest.DynaTest
import kotlin.test.expect

class SimpleKubernetesClientTest : DynaTest({
    group("getKubernetesYamlConfigFile") {
        test("simple") {
            expect("""
#
# Microk8s resource config file for vaadin-boot-example-gradle
#

apiVersion: v1
kind: Namespace
metadata:
  name: shepherd-vaadin-boot-example-gradle
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: deployment
  namespace: shepherd-vaadin-boot-example-gradle
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
        image: <<IMAGE_AND_HASH>>
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "64Mi"
            cpu: 0
          limits:
            memory: "256Mi"  # https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/
            cpu: 1.0
---
apiVersion: v1
kind: Service
metadata:
  name: service
  namespace: shepherd-vaadin-boot-example-gradle
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
  namespace: shepherd-vaadin-boot-example-gradle
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /\${'$'}3
    nginx.ingress.kubernetes.io/proxy-cookie-path: / /\${'$'}1
    nginx.ingress.kubernetes.io/configuration-snippet: |
      rewrite ^(/vaadin-boot-example-gradle)\${'$'} \${'$'}1/ permanent;
spec:
  tls:
  - hosts:
    - v-herd.eu
  rules:
    - host: v-herd.eu
      http:
        paths:
          - path: /(vaadin-boot-example-gradle)(/|${'$'})(.*)
            pathType: Prefix
            backend:
              service:
                name: service
                port:
                  number: 8080
            """.trim()) {
                SimpleKubernetesClient().getKubernetesYamlConfigFile(fakeProject)
            }
        }
        test("env variables") {
            val p = fakeProject.copy(runtime = fakeProject.runtime.copy(envVars = mapOf("FOO" to "BAR", "SPRING_DATASOURCE_URL" to "jdbc:postgresql://liukuri-postgres:5432/postgres")))
            expect("""
#
# Microk8s resource config file for vaadin-boot-example-gradle
#

apiVersion: v1
kind: Namespace
metadata:
  name: shepherd-vaadin-boot-example-gradle
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: deployment
  namespace: shepherd-vaadin-boot-example-gradle
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
        image: <<IMAGE_AND_HASH>>
        env:
        - name: FOO
          value: "BAR"
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:postgresql://liukuri-postgres:5432/postgres"
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "64Mi"
            cpu: 0
          limits:
            memory: "256Mi"  # https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/
            cpu: 1.0
---
apiVersion: v1
kind: Service
metadata:
  name: service
  namespace: shepherd-vaadin-boot-example-gradle
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
  namespace: shepherd-vaadin-boot-example-gradle
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /\${'$'}3
    nginx.ingress.kubernetes.io/proxy-cookie-path: / /\${'$'}1
    nginx.ingress.kubernetes.io/configuration-snippet: |
      rewrite ^(/vaadin-boot-example-gradle)\${'$'} \${'$'}1/ permanent;
spec:
  tls:
  - hosts:
    - v-herd.eu
  rules:
    - host: v-herd.eu
      http:
        paths:
          - path: /(vaadin-boot-example-gradle)(/|${'$'})(.*)
            pathType: Prefix
            backend:
              service:
                name: service
                port:
                  number: 8080
            """.trim()) {
                SimpleKubernetesClient().getKubernetesYamlConfigFile(p)
            }
        }
    }
})