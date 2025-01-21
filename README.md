# Shepherd Java Client

Provides a nice Java API library and a Java CLI (Command-line interface) client
for [Vaadin Shepherd](https://github.com/mvysny/shepherd).

Requires Java 17+.

The library is available in Maven Central. To use, add this to your `build.gradle`:
```groovy
dependencies {
    implementation("com.github.mvysny.shepherd:shepherd-java-api:0.3")
}
```

To use, simply instantiate an implementation of `ShepherdClient`:

```kotlin
val client: ShepherdClient = LinuxShepherdClient()  // or FakeShepherdClient()
```

`LinuxShepherdClient` requires Shepherd to be installed on this machine. However,
for development purposes, it's better to use `FakeShepherdClient` which doesn't
require anything to be installed on the dev machine, while providing reasonable
fake data.

Requires a configuration file to be placed in `/etc/shepherd/java/config.json`, example contents:

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
          "url": "http://localhost:8080",
          "username": "admin",
          "password": "admin"
        }
}
```

The `memoryQuotaMb` is the memory available both for project runtime and for project builds.
Every project's runtime memory + the build memory is guaranteed by Shepherd; if a project would be created
that overflows this quota, the project creation is prohibited. Calculate the quota value
as follows: Take the total host machine memory, subtract memory for Jenkins usage (by default 512mb), for Kubernetes itself (say 1000mb),
possibly 500mb for the future shepherd-ui project, and finally subtract memory for OS usage (say 200mb).

## shepherd-cli

The [shepherd-cli](shepherd-cli) project provides a command-line client for Shepherd.
Simply build the CLI via `./gradlew shepherd-cli:build`, then scp
the `shepherd-cli/build/distributions/*.zip` to the target machine which runs Shepherd,
then unzip and run the `shepherd-cli` binary.

Shepherd CLI requires Java 17+.

`shepherd-cli create` requires the project descriptor json. It's really simple,
here's a very simple example for the [vaadin-boot-example-gradle](https://github.com/mvysny/vaadin-boot-example-gradle) project:

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

A more complex example:

```json
{
  "id": "jdbi-orm-vaadin-crud-demo",
  "description": "JDBI-ORM example project",
  "gitRepo": {
    "url": "https://github.com/mvysny/jdbi-orm-vaadin-crud-demo",
    "branch": "master",
    "credentialsID": "c4d257ce-0048-11ee-a0b5-ffedf9ffccf4"
  },
  "owner": {
    "name": "Martin Vysny",
    "email": "mavi@vaadin.com"
  },
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

### Updating a project

The project config json files are located at `/etc/shepherd/java/projects/PROJECT_ID.json`.
Do not edit the file in-place: copy it to `/root/`, edit it there, then run `./shepherd-cli update -f /root/file.json` then delete it from /root/.
That way, Shepherd can track what has been changed, and can restart the project VM quickly if need be.

# Adding Your Project To Shepherd

That's easy:

1. Create the project JSON as above
2. Create a Dockerfile as explained below.
3. Run `./shepherd-cli create -f file.json` to create the project.
4. Done - the project is now being built in Jenkins; when the build succeeds, it will be
   deployed in Kubernetes.

## Dockerfile

Shepherd expects the following from your project:

1. It must have `Dockerfile` at the root of its git repo.
2. The Docker image can be built via the `docker build -t test/xyz:latest .` command;
   The image can be run via `docker run --rm -ti -p8080:8080 -m256m test/xyz` command.

Generally, all you need is to place an appropriate `Dockerfile` to the root of your project's git repository.
See the following projects for examples:

1. Gradle+Embedded Jetty packaged as zip: [vaadin-boot-example-gradle](https://github.com/mvysny/vaadin-boot-example-gradle),
   [vaadin14-boot-example-gradle](https://github.com/mvysny/vaadin14-boot-example-gradle),
   [karibu-helloworld-application](https://github.com/mvysny/karibu-helloworld-application),
   [beverage-buddy-vok](https://github.com/mvysny/beverage-buddy-vok),
   [vok-security-demo](https://github.com/mvysny/vok-security-demo)
2. Maven+Embedded Jetty packaged as zip: [vaadin-boot-example-maven](https://github.com/mvysny/vaadin-boot-example-maven)
3. Maven+Spring Boot packaged as executable jar: [Liukuri](https://github.com/vesanieminen/ElectricityCostDashboard),
   [my-hilla-app](https://github.com/mvysny/my-hilla-app), [vaadinplus](https://github.com/anezthes/vaadinplus), [TextField Formatter Zen](https://github.com/vaadin-component-factory/textfieldformatter-zen/)
   [Vaadin Hilla Spring PetClinic](https://github.com/jcgueriaud1/spring-petclinic-vaadin-flow)

Please try building and running the app on your own machine first - debugging
any Docker-related issues is far easier on dev machine than remotely on Shepherd.

**IMPORTANT**: pay attention to the `-m256m` switch - this sets the hard limit on how much memory
the container may use.  If JVM asks for more, it will be hard-killed by the Linux OOM-killer,
without any warning or any log message (only host OS dmesg will log this). Make sure to have your Dockerfile run Java
with the `-Xmx???m` VM argument; that way the app will crash with OutOfMemoryException which should be visible in the logs.
The `-Xmx` value should be a bit lower value than the hard limit, to give a bit of room for JVM itself.

## Private Repositories & Credentials

Private repositories may need private SSH key to access them. For example:

* A private GitHub repository `foo-repo`. A GitHub user `foo-user` is created and a private SSH key is generated for him.
  The `foo-user` then needs to be invited to the private repository, in order to gain read-only access.
  Shepherd will have knowledge of the private SSH key. That way, Shepherd can impersonate the `foo-user` and can access `foo-repo`.

Every credential has a unique identifier which needs to be passed in via the `credentialsID` in the config json file.
Before that, the credential needs to be registered:

* Either the Shepherd admin can do that directly, by creating the credential in Jenkins directly,
* or the users can create their own credentials, via the Java functions offered by the `ShepherdClient` Java class (TBD)

The credentials are all ultimately stored in Jenkins, at *Dashboard / Manage Jenkins / Credentials / System / Global credentials*.
Every credential should contain information on which files it contains (e.g. `/root/.ssh/id_rsa`) and where it is used
(e.g. GitHub user `foo-user`).

## Enabling Additional Services

### PostgreSQL database

Adding a persistent postgresql database is easy:

1. Add the `Postgres` additional service to the project descriptor JSON: `"additionalServices": [{"type": "Postgres"}]`
2. Configure your app to connect to the `jdbc:postgresql://postgres:5432/postgres` URL, with the `postgres` username and `mysecretpassword` password.

### Vaadin Offline Key

For Vaadin Pro/Prime components you'll need a Vaadin License.

The license can be obtained at [My Licenses](https://vaadin.com/myaccount/licenses). You'll need the "Server license key",
NOT the "Offline development license key" since the Machine ID changes unpredictably in CI/CD Docker environment.

Once you have the key, edit the project in v-herd admin console and add the build argument
`offlinekey` with the value of the license key itself.

> Note: the name of the build argument can be anything, but you need to correctly refer to it in your `Dockerfile.`

Then, in your `Dockerfile`, you'll fetch the license key from the build argument into an environment variable:
```dockerfile
ARG offlinekey
ENV VAADIN_OFFLINE_KEY=$offlinekey
```
To test, build your app via
```bash
$ docker build -t test/xyz:latest --build-arg offlinekey=the_license_key .
```
