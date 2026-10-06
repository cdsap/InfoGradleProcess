# AGENTS.md

Instructions for coding agents working in `cdsap/InfoGradleProcess`.

## Project

Gradle plugin (`io.github.cdsap.gradleprocess`) that reports Gradle JVM process info (via `jstat` / `jinfo`) either:

- to the **console** at end of build, or
- into a **Develocity** Build Scan as custom values

Keep configuration-cache compatibility. Prefer small, scoped changes.

## Layout

Sources live in the `:plugin` module (`plugin/`), not the root project.

- `plugin/src/main/kotlin/.../InfoGradleProcessPlugin.kt` — entrypoint for `io.github.cdsap.gradleprocess`, applied from settings or a build script
- `plugin/src/main/kotlin/.../InfoGradleProcessProjectPlugin.kt` — project-only alias (`.project` id)
- `plugin/src/main/kotlin/.../InfoGradleProcessReporting.kt` — shared Develocity vs console wiring for both entrypoints
- `plugin/src/main/kotlin/.../InfoGradleProcessBuildService.kt` — console reporting build service
- `plugin/src/main/kotlin/.../DevelocityWrapperConfiguration.kt` — Develocity / Build Scan reporting
- `plugin/src/main/kotlin/.../GradleProcessCollector.kt` — shared `ConsolidateProcesses` collector (use this; do not re-inline consolidation in the two reporting paths)
- `plugin/src/main/kotlin/.../output/ConsoleOutput.kt`, `output/DevelocityValues.kt` — presentation only
- `plugin/src/main/kotlin/.../Constants.kt` — process name constants
- Tests under `plugin/src/test/kotlin/io/github/cdsap/gradleprocess/`

## Commands

Java 17. Default verification:

```bash
./gradlew test
```

Focused examples:

```bash
./gradlew test --tests io.github.cdsap.gradleprocess.GradleProcessCollectorTest
./gradlew test --tests io.github.cdsap.gradleprocess.InfoGradleProcessPluginTest
```

Do not commit `build/` or `.gradle/`. Clean them from the worktree before finishing if created.

## Working rules

- Investigate existing code before editing; keep diffs strictly scoped to the issue.
- Process collection belongs in `GradleProcessCollector` (or a clear successor). Console and Develocity paths should call the shared collector, not duplicate `ConsolidateProcesses` wiring.
- Add or update regression tests for behavior changes.
- Do not push, open/merge PRs, or touch GitHub issue state unless explicitly asked.
- Do not read or modify credentials, tokens, `.env`, or publishing secrets.
- Avoid unrelated refactors, dependency bumps, or formatting sweeps.
