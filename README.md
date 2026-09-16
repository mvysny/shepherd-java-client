# Shepherd Java Client

Provides a nice Java API library and a Java CLI (Command-line interface) client
for [Vaadin Shepherd](https://github.com/mvysny/shepherd)
and [Vaadin Shepherd Traefik](https://github.com/mvysny/shepherd-traefik)

Requires Java 21+.

## The library

The library is in Maven Central:

```kotlin
dependencies {
    implementation("com.github.mvysny.shepherd:shepherd-java-api:0.5")
}
```

Create a `ShepherdClient`:

```kotlin
val client: ShepherdClient = LocalFS().createClient()  // or FakeShepherdClient()
```

`LocalFS().createClient()` needs Shepherd installed on this machine and reads
`/etc/shepherd/java/config.json`. For development, use `FakeShepherdClient`: it needs nothing
installed and serves fake data.

## Configuration

`/etc/shepherd/java/config.json`:

```json
{
  "memoryQuotaMb": 14102,
  "concurrentJenkinsBuilders": 2,
  "maxProjectRuntimeResources": {
    "memoryMb": 512,
    "cpu": 1
  },
  "maxProjectBuildResources": {
    "memoryMb": 2500,
    "cpu": 2
  },
  "jenkins": {
    "url": "http://jenkins:8080",
    "username": "admin",
    "password": "admin"
  },
  "hostDNS": "mydomain.me",
  "shepherdHome": "/opt/shepherd-traefik",
  "containerSystem": "traefik-docker"
}
```

* `memoryQuotaMb`: memory for all project runtimes and builds together. Shepherd guarantees every project
  its runtime + build memory, and refuses a project that would overflow the quota. Compute it as host memory
  minus Jenkins (512 MB by default), minus Kubernetes itself if used (~1000 MB), minus Shepherd-Web (~500 MB),
  minus the OS (~200 MB).
* `concurrentJenkinsBuilders`: must match the `# of executors` configured in Jenkins.
* `jenkins`: defaults to `http://localhost:8080`, `admin`/`admin`. With Traefik, Jenkins runs in Shepherd's private
  Docker network, so use `http://jenkins:8080`; with Kubernetes both run directly on the host, so keep `localhost`.
* `hostDNS`: where Shepherd runs, e.g. `v-herd.eu` (the default).
* `shepherdHome`: `/opt/shepherd` (the default) for Shepherd Kubernetes, `/opt/shepherd-traefik` for Shepherd Traefik.
* `containerSystem`: `kubernetes` (the default) or `traefik-docker`.
* `googleSSOClientId` (Shepherd-Web only): enables Google SSO login with this client ID;
  see [vaadin-google-oauth](https://mvysny.github.io/vaadin-google-oauth/).
* `ssoOnlyAllowEmailsEndingWith` (Shepherd-Web only): e.g. `@vaadin.com` allows only those e-mails; null or empty allows all.

## shepherd-cli

Build it via `./gradlew :shepherd-cli:build`, scp `shepherd-cli/build/distributions/*.zip` to the Shepherd
machine, unzip and run `bin/shepherd-cli --help` for the commands
(`list`, `show`, `create`, `update`, `delete`, `logs`, `builds`, `buildlog`, `restart`, `shutdown`, ...).
The Shepherd-Web Docker image ships the CLI too.

# Adding Your Project To Shepherd

1. Write the project JSON (below).
2. Add a `Dockerfile` to your project (below).
3. Run `shepherd-cli create -f file.json`, or create the project in Shepherd-Web.

Jenkins then builds the project and, when the build succeeds, deploys it.

## Project JSON

A minimal example, for [vaadin-boot-example-gradle](https://github.com/mvysny/vaadin-boot-example-gradle):

```json
{
  "id": "vaadin-boot-example-gradle",
  "description": "vaadin-boot-example-gradle",
  "gitRepo": {
    "url": "https://github.com/mvysny/vaadin-boot-example-gradle",
    "branch": "master"
  },
  "owner": {
    "name": "Martin Vysny",
    "email": "mavi@vaadin.com"
  },
  "runtime": {
    "resources": {
      "memoryMb": 256,
      "cpu": 1.0
    }
  },
  "build": {
    "resources": {
      "memoryMb": 2048,
      "cpu": 2.0
    }
  }
}
```

All the options:

```json
{
  "id": "jdbi-orm-vaadin-crud-demo",
  "description": "JDBI-ORM example project",
  "webpage": "https://github.com/mvysny/jdbi-orm-vaadin-crud-demo",
  "gitRepo": {
    "url": "https://github.com/mvysny/jdbi-orm-vaadin-crud-demo",
    "branch": "master",
    "credentialsID": "c4d257ce-0048-11ee-a0b5-ffedf9ffccf4"
  },
  "owner": {
    "name": "Martin Vysny",
    "email": "mavi@vaadin.com"
  },
  "additionalAdmins": ["someone@vaadin.com"],
  "runtime": {
    "resources": {
      "memoryMb": 256,
      "cpu": 1.0
    },
    "envVars": {
      "JDBC_URL": "jdbc:postgresql://postgres-service:5432/postgres",
      "JDBC_USERNAME": "postgres",
      "JDBC_PASSWORD": "mysecretpassword"
    }
  },
  "build": {
    "resources": {
      "memoryMb": 2048,
      "cpu": 2.0
    },
    "buildArgs": {
      "offlinekey": "q3984askdjalkd9823"
    },
    "dockerFile": "vherd.Dockerfile"
  },
  "publication": {
    "publishOnMainDomain": false,
    "https": true,
    "additionalDomains": [
      "demo.jdbiorm.eu"
    ],
    "ingressConfig": {
      "maxBodySizeMb": 2,
      "proxyReadTimeoutSeconds": 360
    }
  },
  "additionalServices": [
    {
      "type": "Postgres"
    }
  ]
}
```

`gitRepo.url` can't be changed after creation; delete and recreate the project instead.

### Updating a project

Project JSONs live at `/etc/shepherd/java/projects/PROJECT_ID.json`. Don't edit them in place: copy the file
elsewhere, edit the copy, run `shepherd-cli update -f copy.json`, then delete the copy. That way Shepherd
sees what changed and restarts the project only when needed.

## Dockerfile

Shepherd builds the `Dockerfile` at the root of your git repo (or the one named by `build.dockerFile`).
Before submitting, make sure this works on your machine — debugging Docker is far easier locally:

```bash
docker build -t test/xyz:latest .
docker run --rm -ti -p8080:8080 -m256m test/xyz
```

**IMPORTANT**: `-m256m` is a hard memory limit, matching `runtime.resources.memoryMb`. A JVM exceeding it is
killed by the Linux OOM-killer with no log message. Run Java with `-Xmx` a bit below the limit, so the app fails
with a visible `OutOfMemoryError` instead.

Examples:

1. Gradle + embedded Jetty, zip: [vaadin-boot-example-gradle](https://github.com/mvysny/vaadin-boot-example-gradle),
   [karibu-helloworld-application](https://github.com/mvysny/karibu-helloworld-application),
   [beverage-buddy-vok](https://github.com/mvysny/beverage-buddy-vok),
   [vok-security-demo](https://github.com/mvysny/vok-security-demo)
2. Maven + embedded Jetty, zip: [vaadin-boot-example-maven](https://github.com/mvysny/vaadin-boot-example-maven)
3. Maven + Spring Boot, executable jar: [vaadin-spring-karibu-testing](https://github.com/mvysny/vaadin-spring-karibu-testing),
   [Liukuri](https://github.com/vesanieminen/ElectricityCostDashboard),
   [my-hilla-app](https://github.com/mvysny/my-hilla-app),
   [vaadinplus](https://github.com/anezthes/vaadinplus),
   [TextField Formatter Zen](https://github.com/vaadin-component-factory/textfieldformatter-zen/),
   [Spring PetClinic Vaadin](https://github.com/jcgueriaud1/spring-petclinic-vaadin-flow)

## Private Repositories

A private repo needs credentials, typically an SSH key: e.g. create a GitHub user `foo-user` with its own SSH key
and invite it to the private repo with read-only access.

The Shepherd admin registers the credential in Jenkins, at *Dashboard / Manage Jenkins / Credentials / System / Global
credentials*; describe in it what it contains (e.g. `/root/.ssh/id_rsa`) and where it's used (e.g. GitHub user
`foo-user`). Then put the credential's ID into `gitRepo.credentialsID`.

## PostgreSQL

Shepherd Kubernetes only. Add `"additionalServices": [{"type": "Postgres"}]` to the project JSON, then connect to
`jdbc:postgresql://postgres-service:5432/postgres` as `postgres` / `mysecretpassword`. Only your project can
access the database.

# Tips and Tricks

## Vaadin Offline Key

Vaadin Pro/Prime components need a license at **build time**; `VAADIN_OFFLINE_KEY` as a runtime env var is not enough.

Get the "Server license key" at [My Licenses](https://vaadin.com/myaccount/licenses) — NOT the "Offline development
license key", since the Machine ID changes unpredictably in CI Docker. Add it as a build argument, e.g. `offlinekey`
(any name works, as long as the `Dockerfile` uses the same one), and read it in the `Dockerfile`:

```dockerfile
ARG offlinekey
ENV VAADIN_OFFLINE_KEY=$offlinekey
```

Test locally via `docker build -t test/xyz:latest --build-arg offlinekey=the_license_key .`.
[vaadinplus](https://github.com/anezthes/vaadinplus) has a working example.

## Build Cache

Cache the Maven/Gradle repository and `~/.vaadin` between builds:

```dockerfile
RUN --mount=type=cache,target=/root/.m2 --mount=type=cache,target=/root/.vaadin ./mvnw -C -e clean package -Pproduction
```

```dockerfile
RUN --mount=type=cache,target=/root/.gradle --mount=type=cache,target=/root/.vaadin ./gradlew clean build -Pvaadin.productionMode --no-daemon
```

# Maintenance

Keep the host up-to-date via `apt`, but never reboot while Jenkins is building: shut Shepherd down gracefully first.

## Shepherd Traefik

1. Log in to Shepherd-Web, go to `/admin`, click "Shut Down", and refresh until it reads "Shepherd is shut down".
2. `sudo reboot`

Jenkins runs in Docker, isn't updated by `apt` and is reachable only by Shepherd-Web, so keeping it current isn't important.

### Updating the Shepherd-Web container

```bash
cd /opt/shepherd-traefik
docker compose pull
docker compose up -d --no-deps shepherd
```

Docker recreates the container from the newest image and joins it to the `admin.int` network.
Don't update Traefik this way: `docker compose` drops its network bindings and Traefik can no longer route to the apps.

## Shepherd Kubernetes (old)

`apt` updates Jenkins frequently, which is unsafe mid-build:

1. `shepherd-cli shutdown` — waits until no build runs.
2. `sudo apt update && sudo apt dist-upgrade`
3. An updated Jenkins restarts and resumes taking jobs; if a reboot is needed, run `shepherd-cli shutdown` again, then `sudo reboot`.
