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
