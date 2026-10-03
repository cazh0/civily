# Civily

Unofficial [NationStates](https://www.nationstates.net/) client for Android. Kotlin + Compose.
Successor to Stately; no shared code.

| Tree | What |
|---|---|
| `civily-app/` | Civily, `dev.cazh0.civily`. `:app` ships · `:benchmark` measures. |
| `Stately/` | Java predecessor. Frozen until parity. [Its README](Stately/README.md). |

## Build

From `civily-app/`. Android SDK via `local.properties` or `ANDROID_HOME`. Gradle fetches JDK 21.

```bash
./gradlew :app:testDebugUnitTest
```

Every gate. Green before push.

```bash
./gradlew :app:assembleDebug
```

## Measure

Device on cable. Release builds only.

```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest
```

Cold start + frames.

```bash
./gradlew :app:generateReleaseBaselineProfile
```

Rewrites the checked-in profile. When: `RULES.md` §4.9.

## Documents

| File | For |
|---|---|
| `CLAUDE.md` | How Claude works here. |
| `ARCHITECTURE.md` | Where things go. Start here. |
| `RULES.md` | Law. |
| `DESIGN_RULES.md` | Visual law. |
| `BACKLOG.md` | Open work. |
| `DECISIONS.md` | Why. |

Anything else: the code.

## License

Apache 2.0, `LICENSE`.
