# Architecture

How the pieces compose — what no single symbol can say and what would be expensive to overturn:
wiring and dependency direction, the lifecycle / data-flow story, the flows a newcomer needs, where
to start reading. **Normative: the code conforms.** Change this file first, then the code.
Not here: why (`decisions.md` — cite the `D_`), what upstream does (`research.md` — cite the `R_`),
one symbol's behaviour (its doc comment), the module map (`AGENTS.md`). Only the sections with
content; the worked example in each is the ruler. Cap 12 KB — over it, research or doc-comment
content has crept in.

---

## Wiring

- Dependencies point down: `shepherd-web` and `shepherd-cli` → `shepherd-java-api`. Both front-ends see only the `ShepherdClient` interface.
- `LocalFS.createClient()` is the one production wiring point: it reads `/etc/shepherd/java/config.json` and builds `JenkinsBasedShepherdClient` over `SimpleJenkinsClient` plus the `RuntimeContainerSystem` named by `Config.containerSystem` (`D_runtime_container_system`).
- `KubernetesRuntimeContainerSystem` writes YAML under `/etc/shepherd/k8s/` and drives `kubectl`; `TraefikDockerRuntimeContainerSystem` drives `docker` directly. Both through `exec()`.
- The Jenkins job's shell step runs `<shepherdHome>/shepherd-build <id>`, a script owned by the Shepherd / Shepherd Traefik repo, not by this project.
- `shepherd-web` holds the client in `Services`: `Main` with `dummy` calls `Services.newFake()` (`FakeShepherdClient`, temp dir); otherwise `Bootstrap.contextInitialized` calls `Services.newReal(LocalFS())`. Tests call `Services.newFake()`.

## Flows

**Create a project** (`JenkinsBasedShepherdClient.createProject`):

1. Refuse a reserved or existing `ProjectId`; `validate` checks limits, quota and `ClientFeatures`.
2. Write `/etc/shepherd/java/projects/<id>.json` — the source of truth from here on.
3. `RuntimeContainerSystem.createProject` prepares the runtime side (YAML, networks).
4. `SimpleJenkinsClient.createJob`, then `build` — the first build deploys the app.

**Update a project** (`updateProject`):

1. Load the old JSON; refuse a changed `gitRepo.url`; `validate`.
2. Overwrite the JSON; `RuntimeContainerSystem.updateProjectConfig` rewrites runtime config and says whether a restart is needed; `updateJob`.
3. Pick the cheapest action: not running yet → build; `SimpleJenkinsClient.needsProjectRebuild` → build; restart needed → `restartProject`; else nothing.

## Where to start reading

`shepherd-java-api/src/main/kotlin/ShepherdClient.kt` for the contract every front-end uses, then `JenkinsBasedShepherdClient.kt` for how it is fulfilled.
