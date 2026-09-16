# Decisions

Why this project is the way it is and not otherwise — FAQ-shaped: each entry is a question and
its current answer. Rewrite the answer when it changes; delete the entry when nobody asks any
more. An entry is earned by what it would cost to reverse — half the code base — or by research
the next person would otherwise redo (cited as its `R_`). Not an entry: windows → panels
"because that's the trend", this red over that red, `get_foo` over `is_foo?`, the testing library,
the CI host, a version bump — a comment at the site of the choice, or nothing; nothing about
`design/` itself. Cite by slug, `D_<slug>`, never by position; `grep '^## D_' design/decisions.md`
is the index. The first entry is the ruler: every later one trims to its length — which is how
long this file gets, so keep it short. When you have written an entry, re-read it against the one
above, check it says nothing the doc comments already say, and cut what is left over.

---

## D_runtime_container_system — Why one Jenkins-based client with a pluggable runtime rather than a client per Shepherd?

Shepherd Kubernetes and Shepherd Traefik build the same way — a Jenkins job per project running
`<shepherdHome>/shepherd-build <id>` — and differ only in how the built image runs. So
`JenkinsBasedShepherdClient` owns everything shared (the project JSON, validation, the quota, the
Jenkins job, the "rebuild, restart or nothing" update logic) and delegates the running part to a
`RuntimeContainerSystem`, picked by `Config.containerSystem` in `LocalFS.createClient()`. What a
backend can't do (private repos, custom domains, services) is a `ClientFeatures` value that
`validate` enforces and the web UI reads to hide fields. Why not a `ShepherdClient` per platform:
the update logic and quota would be written twice and drift, and the CLI and web UI would need to
know which one they hold. Why not `if (backend is …)` checks in callers: every new backend would
be a sweep through the UI. The cost we carry: a capability that is not a yes/no flag strains
`ClientFeatures`.
