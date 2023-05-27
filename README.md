# Shepherd Java Client

Provides a nice Java API library and a Java CLI (Command-line interface) client
for [Vaadin Shepherd](https://github.com/mvysny/shepherd).

Requires Java 17+.

To use, simply instantiate an implementation of `ShepherdClient`:

```kotlin
val client: ShepherdClient = LinuxShepherdClient()  // or FakeShepherdClient()
```

`LinuxShepherdClient` requires Shepherd to be installed on this machine. However,
for development purposes, it's better to use `FakeShepherdClient` which doesn't
require anything to be installed on the dev machine, while providing reasonable
fake data.

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
    "additionalDomains": [
      "demo.jdbiorm.eu"
    ]
  },
  "additionalServices": [
    {
      "type": "Postgres"
    }
  ]
}
```

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
2. The Docker image can be built via the `docker build --no-cache -t test/xyz:latest .` command;
   The image can be run via `docker run --rm -ti -p8080:8080 test/xyz` command.

Generally, all you need is to place an appropriate `Dockerfile` to the root of your project's git repository.
See the following projects for examples:

1. Gradle+Embedded Jetty packaged as zip: [vaadin-boot-example-gradle](https://github.com/mvysny/vaadin-boot-example-gradle),
   [vaadin14-boot-example-gradle](https://github.com/mvysny/vaadin14-boot-example-gradle),
   [karibu-helloworld-application](https://github.com/mvysny/karibu-helloworld-application),
   [beverage-buddy-vok](https://github.com/mvysny/beverage-buddy-vok),
   [vok-security-demo](https://github.com/mvysny/vok-security-demo)
2. Maven+Embedded Jetty packaged as zip: [vaadin-boot-example-maven](https://github.com/mvysny/vaadin-boot-example-maven)
3. Maven+Spring Boot packaged as executable jar: [Liukuri](https://github.com/vesanieminen/ElectricityCostDashboard),
   [my-hilla-app](https://github.com/mvysny/my-hilla-app).
