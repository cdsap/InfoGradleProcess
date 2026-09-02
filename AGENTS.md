# AGENTS.md

Instructions for coding agents working in `cdsap/InfoGradleProcess`.

## Project

Gradle plugin (`io.github.cdsap.gradleprocess`) that reports Gradle JVM process info (via `jstat` / `jinfo`) either:

- to the **console** at end of build, or
- into a **Develocity** Build Scan as custom values

Keep configuration-cache compatibility. Prefer small, scoped changes.

## Layout

- `InfoGradleProcessPlugin.kt` — entrypoint; chooses Develocity vs console path
- `InfoGradleProcessBuildService.kt` — console reporting build service
- `DevelocityWrapperConfiguration.kt` — Develocity / Build Scan reporting
- `ProcessInfoCollector.kt` — shared `ConsolidateProcesses` collector (use this; do not re-inline consolidation in the two reporting paths)
- `output/ConsoleOutput.kt`, `output/DevelocityValues.kt` — presentation only
- `Constants.kt` — process name constants
- Tests under `src/test/kotlin/io/github/cdsap/gradleprocess/`

## Commands

Java 17. Default verification:

```bash
./gradlew test
```

Focused examples:

```bash
./gradlew test --tests io.github.cdsap.gradleprocess.ProcessInfoCollectorTest
./gradlew test --tests io.github.cdsap.gradleprocess.InfoGradleProcessPluginTest
```

Do not commit `build/` or `.gradle/`. Clean them from the worktree before finishing if created.

## Working rules

- Investigate existing code before editing; keep diffs strictly scoped to the issue.
- Process collection belongs in `ProcessInfoCollector` (or a clear successor). Console and Develocity paths should call the shared collector, not duplicate `ConsolidateProcesses` wiring.
- Add or update regression tests for behavior changes.
- Do not push, open/merge PRs, or touch GitHub issue state unless explicitly asked.
- Do not read or modify credentials, tokens, `.env`, or publishing secrets.
- Avoid unrelated refactors, dependency bumps, or formatting sweeps.
