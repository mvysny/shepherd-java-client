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
