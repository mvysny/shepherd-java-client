# Shepherd Java Client

## What this is

Provides a nice Java API library and a Java CLI (Command-line interface) client
for [Vaadin Shepherd](https://github.com/mvysny/shepherd)
and [Vaadin Shepherd Traefik](https://github.com/mvysny/shepherd-traefik)

## Promises

- **One client for both Shepherds.** The same library, CLI and web UI drive Shepherd Kubernetes and Shepherd Traefik; what a backend can't do is declared, not forked.
- **A nice API for Java callers too.** `shepherd-java-api` is published to Maven Central and reads naturally from plain Java, not just Kotlin.
- **Works without a Shepherd.** The library and the web UI can be developed and demoed on a laptop with nothing installed.

## Design docs

| File | Owns | Loaded |
|---|---|---|
| `README.md` | the pitch, config file reference, how to add a project, host maintenance | — |
| `CONTRIBUTING.md` | the release steps | — |
| `AGENTS.md` (this) | promises, invariants, the module map, conventions, commands | every turn |
| `design/architecture.md` | how the pieces compose — wiring, dependency direction, the create/update flows; normative | lazy |
| `design/decisions.md` | why this and not that — `D_` entries, FAQ-shaped | lazy |
| doc comments | what one symbol does and why it is shaped so | at the symbol |

Every fact lives in exactly one of these; the others link to it.

## Invariants

- **Every create/update runs `ShepherdClient.validate` before touching disk, Jenkins or containers.** Skip it and the memory quota overcommits the host.
- **The project JSON under `/etc/shepherd/java/projects/` is the source of truth.** Kubernetes YAML, Docker state and the Jenkins job are derived from it; the flows are in architecture.md.
- **A Jenkins job's name is the `ProjectId`.** Builds, logs and the queue are looked up by it.
- **What a backend can't do is a `ClientFeatures` flag**, enforced in `validate` and read by the UI — never a type check on the `RuntimeContainerSystem`. See `D_runtime_container_system`.
- **`gitRepo.url` is immutable after creation.** `updateProject` rejects the change; delete and recreate instead.
- **`shepherd-java-api` depends on no other module and no Vaadin.** It is the published artifact.

## Module map

- `shepherd-java-api` — the published library: `ShepherdClient`, Jenkins client, the runtime container backends, config files.
- `shepherd-cli` — thin `kotlinx-cli` wrapper over `ShepherdClient`; commands in `shepherd-cli/src/main/kotlin/Main.kt`.
- `shepherd-web` — Vaadin Flow admin UI on Karibu-DSL and Vaadin Boot; the Docker image also ships the CLI.

## Conventions

- **Kotlin/JVM 21; `explicitApi()` in `shepherd-java-api`.** Every public symbol carries an explicit `public`.
- **Java callers are first-class.** `@JvmOverloads` / `@JvmStatic` on public API; `ProjectJavaAPITest` exercises it from Java.
- **JSON through kotlinx.serialization; HTTP through `java.net.http.HttpClient`; subprocesses through `exec()` (zt-exec).** No Jackson, no OkHttp.
- **Diagnostics through slf4j**, `simplelogger.properties` in the apps; stdout is the CLI's output only.
- **Tests: JUnit with `kotlin.test`, no mocking framework.** `FakeShepherdClient` is the fake; UI tests extend `AbstractAppTest` (Karibu Testing).
- **Tests needing Jenkins, Docker or Kubernetes use Testcontainers** and a local Docker daemon; they are slow.
- **Web UI: Karibu-DSL + Vaadin Boot, no Spring.** Global services through the `Services` singleton; auth via `vaadin-simple-security` + `UserRegistry`.
- **Dependency versions live in `gradle/libs.versions.toml`**, never in a subproject build file.
- **Version lives only in the root `build.gradle.kts` `allprojects` block.** Release steps are in `CONTRIBUTING.md`.

## Commands

- `./gradlew build` — default `clean build`: tests, then `design/verify_design_tripwires.sh` via `check`; CI runs `./gradlew --no-daemon --no-watch-fs` on push (`.github/workflows/gradle.yml`), plus the tripwire in a job of its own.
- `./gradlew :shepherd-java-api:test --tests "com.github.mvysny.shepherd.api.ProjectTest"` — one test class.
- `./gradlew build -Pvaadin.productionMode` — production web build, what the `Dockerfile` runs.
- `./gradlew :shepherd-cli:build` — CLI zip at `shepherd-cli/build/distributions/*.zip`.
- `./gradlew :shepherd-cli:installDist`, then `shepherd-cli/build/install/shepherd-cli/bin/shepherd-cli <command>` — run the CLI.
- Run `com.github.mvysny.shepherd.web.MainKt` with program arg `dummy` — web UI on `FakeShepherdClient`, admin `mavi@vaadin.com` / `admin`. Without `dummy` it needs `/etc/shepherd/java/config.json`.

## Skills this project follows

- **Component-oriented UI:** Vaadin routes and components are self-sufficient and call `ShepherdClient` directly, no MVP layers; the `cop` skill has the rules.
- **Karibu Testing:** UI tests drive real components server-side without a browser; the `karibu-testing` skill has the patterns.

## Maintenance of this file

Loaded every turn; cap 34 KB, a module's own `AGENTS.md` 10 KB. Over it, in this order:
delete what has no home — status, history, class lists, what the code already says; trim
each line to its fact plus one clause and send the explanation home — why →
`design/decisions.md`, how across symbols → `design/architecture.md`, how in one symbol →
its doc comment, what upstream does → `design/research.md`; only then a module's own
`AGENTS.md`, peripheral modules first, never the core. Never paraphrase a lazy entry into a
line here. `design/verify_design_tripwires.sh` checks the caps and the cites.
