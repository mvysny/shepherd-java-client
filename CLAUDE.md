# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

A Java/Kotlin client library + CLI + Web UI for [Vaadin Shepherd](https://github.com/mvysny/shepherd) and [Vaadin Shepherd Traefik](https://github.com/mvysny/shepherd-traefik) — a self-hosted PaaS that builds apps on Jenkins and runs them on Kubernetes or plain Docker+Traefik. Published to Maven Central as `com.github.mvysny.shepherd:shepherd-java-api`.

## Build / test / run

All commands use the Gradle wrapper. JDK 21 required (see `build.gradle.kts`; CI uses Temurin 21).

- Full build + tests: `./gradlew build` (default task chain is `clean build`)
- Run all tests: `./gradlew test`
- Run a single test class: `./gradlew :shepherd-java-api:test --tests "com.github.mvysny.shepherd.api.ProjectTest"`
- Build CLI zip: `./gradlew :shepherd-cli:build` — output at `shepherd-cli/build/distributions/*.zip`
- Build web in production mode: `./gradlew build -Pvaadin.productionMode` (what the Dockerfile uses)
- Run web app in dev mode: run `com.github.mvysny.shepherd.web.MainKt` with program arg `dummy` — this flips `devMode = true`, wires `FakeShepherdClient`, and pre-creates admin `mavi@vaadin.com` / password `admin`. Without `dummy` it calls `LocalFS().createClient()`, which requires `/etc/shepherd/java/config.json` on the host.
- Run CLI: after `./gradlew :shepherd-cli:installDist`, run `shepherd-cli/build/install/shepherd-cli/bin/shepherd-cli <command>`. Commands: `list`, `show`, `logs`, `create -f x.json`, `update -f x.json`, `delete -y`, `metrics`, `builds`, `buildlog`, `stats`, `restart`, `shutdown` (see `shepherd-cli/src/main/kotlin/Main.kt`).

Some tests spin up real containers via Testcontainers (`JenkinsContainer`, Kubernetes/Docker tests under `shepherd-java-api/src/test/kotlin/containers/`); these need a working local Docker daemon and can be slow.

## Release flow

See `CONTRIBUTING.md`. Summary: edit `version` in root `build.gradle.kts` (drop `-SNAPSHOT`), commit + tag, `./gradlew clean build publish closeAndReleaseStagingRepositories`, then build+push Docker image `mvysny/shepherd-java:<ver>` and `:latest`, then bump version back to next `-SNAPSHOT`. Version lives only in the root `build.gradle.kts` `allprojects { version = ... }` block.

## Module layout

Three Gradle subprojects (see `settings.gradle.kts`). Dependencies flow strictly downward:

- **`shepherd-java-api`** — the public Kotlin library. Uses `kotlin { explicitApi() }`, so every new public API needs an explicit `public` modifier. This is the only module published to Maven Central (`configureMavenCentral("shepherd-java-api")` in its `build.gradle.kts`). No Vaadin dependency here.
- **`shepherd-cli`** — thin `kotlinx-cli` wrapper around `ShepherdClient`. Packaged as a zip/tar distribution via the `application` plugin.
- **`shepherd-web`** — Vaadin Flow web UI built with Karibu-DSL and Vaadin Boot (embedded Jetty, no Spring). Also `application`-plugin-packaged; the Dockerfile produces a single image serving the web UI on 8080 and exposing the CLI via `docker exec`.

Shared version catalog in `gradle/libs.versions.toml` — update Vaadin/Kotlin/etc. there, not in subproject build files.

## Core architecture (shepherd-java-api)

The central abstraction is `ShepherdClient` (`shepherd-java-api/src/main/kotlin/ShepherdClient.kt`) — CRUD over projects plus build/run log/metrics access. Two implementations:

- `FakeShepherdClient` — in-memory, for dev mode and unit tests. Stores projects in a temp dir.
- `JenkinsBasedShepherdClient` — production impl. Composes two collaborators:
  - `SimpleJenkinsClient` — talks to Jenkins REST API for create/update/build/log/queue. Job name == `ProjectId`.
  - `RuntimeContainerSystem` — pluggable runtime backend with two concrete impls: `KubernetesRuntimeContainerSystem` and `TraefikDockerRuntimeContainerSystem`. Selection is driven by `Config.containerSystem` (`"kubernetes"` or `"traefik-docker"`), wired in `LocalFS.createClient()`.

Project config lives on disk as JSON (kotlinx.serialization): `ProjectConfigFolder` (`/etc/shepherd/java/projects/<id>.json`) is the source of truth for project definitions; the runtime-container system owns the derived Kubernetes YAML / Docker Compose state. `ConfigFolder.loadConfig()` reads the global `/etc/shepherd/java/config.json` into `Config`.

`JenkinsBasedShepherdClient.updateProject` encodes the non-obvious "what kind of restart does this change need" logic: full Jenkins rebuild only if `BuildSpec.buildArgs`/`dockerFile` changed (`SimpleJenkinsClient.needsProjectRebuild`), else just re-apply container config, else no-op. `gitRepo.url` is immutable after creation.

`ShepherdClient.validate(project)` (free function at the bottom of `JenkinsBasedShepherdClient.kt`) enforces the memory/CPU quota, reserved-ID rules (`admin`, `*-admin`), and `ClientFeatures` (private repos, custom domains, HTTPS-on-custom-domains, supported service types — backends declare what they can do).

## shepherd-web specifics

- `Main.main` with arg `dummy` → dev mode with `FakeShepherdClient`; otherwise production wiring via `LocalFS().createClient()` in `Bootstrap.contextInitialized`.
- Global services go through `Services` object (`client`, `userRegistry`) — a singleton initialized once at boot. Tests call `Services.newFake()` from `AbstractAppTest`.
- Auth is DIY: `vaadin-simple-security` + local `UserRegistry` (JSON file at `<configFolder>/java/webadmin-users.json`). Optional Google SSO toggled by `Config.googleSSOClientId`.
- UI tests use Karibu Testing. `AbstractAppTest` discovers routes once in `@BeforeAll`, then each test sets up `MockVaadin` and logs in via `UserLoginService`. Extend that class when adding new UI tests.
