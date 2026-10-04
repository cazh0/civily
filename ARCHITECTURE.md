# Civily — Architecture

Placement. Shapes. Naming. Boundaries.
Law → `RULES.md` · open work → `BACKLOG.md` · reasons → `DECISIONS.md`. On placement, this file wins.

---

## 0. Documents

Seven files, one job each. A sentence in the wrong file = defect.

| File | Holds | Never | Changes when |
|---|---|---|---|
| `CLAUDE.md` | How Claude works. | Placement, status, reasons. | The method changes. |
| `README.md` | What it is · build · measure · document map. | Anything stated elsewhere. | The entry point changes. |
| `ARCHITECTURE.md` | Placement, shapes, naming, boundaries. | Status, reasons, open work, values in code. | A boundary or shape changes. |
| `RULES.md` | Law. | Structure, commands, status, reasons. | A rule changes. |
| `DESIGN_RULES.md` | Visual law. | Token values, inventory, screen descriptions. | A visual rule changes. |
| `BACKLOG.md` | Open work, ranked, a done-when each · one unranked section for what waits on the game. | Done work, history, reasons. | Work opens or closes. |
| `DECISIONS.md` | Why. Reversals. | Open work, rules, instructions. | A choice is made or reversed. |

1. Each fact once. Others reference it.
2. No document describes code. The code and its `// Why:` are the description. A stale document in
   an AI's context is worse than none.
3. A rule without a gate = preference. A decision without a reason = a rule in the wrong file.
4. Status only in `BACKLOG.md`. No "not built yet", "known gap" or current measurement elsewhere.
5. History only in `git log`.
6. How Claude works → `CLAUDE.md`. What the software may do → `RULES.md`.
7. Existence = placement. §7 may name something absent and where its work lives today; what remains
   → `BACKLOG.md`.
8. `Stately/README.md` belongs to the frozen app. Outside this set. Never edited.

## 1. Structural rules

| # | Rule | Gate |
|---|---|---|
| A1 | Imports point down: `nav` → `feature` → `ui` → `data` → `core`. Never up a tier. `data` imports no Compose. The package root holds the composition root, above every tier; only `feature` and `nav` import from it (`graph`), `R` and `BuildConfig` aside. | `ArchitectureTest` A1 |
| A2 | Failure is a value. A repository returns `Outcome<T>`: never throws, never null for "broke". Every `CivilyError` carries its user-facing string. | `ArchitectureTest` A2 |
| A3 | One way out. API → `NsClient`. Images → `AppGraph.imageLoader`. One `OkHttpClient`. | `ArchitectureTest` A3 |
| A4 | On-screen colours, sizes, durations only in `ui/theme`. Exceptions: `DESIGN_RULES.md` §1.1. | `ArchitectureTest` A4 |
| A5 | Every user-visible string is a resource in `res/values/`. | `ArchitectureTest` A5 |
| A6 | Route strings built only in `Routes`. Navigation only via `NavActions`. | `ArchitectureTest` A6 |
| A7 | Placement by reach. A file in `core/text/` or `ui/component/` is reached by ≥2 features, or by `data/`; reach counts only calls through those two folders. A path `RULES.md` names is exempt (`Population`). | `ArchitectureTest` A7 |

A gate lands only once the code satisfies it, and counts only once mutation proves it: break the
code, watch it go red.

## 2. Tree

```
civily-app/                    Gradle root
├── app/                       :app, the only shipping module
│   └── src/
│       ├── main/kotlin/dev/cazh0/civily/
│       │   ├── *.kt           AppGraph · CivilyApp · MainActivity: the composition root (A1)
│       │   ├── core/          no feature knowledge
│       │   │   ├── net/       NsClient · NsUrl · RateLimiter · UserAgent
│       │   │   ├── session/   Session · Accounts · SessionStore (sole credential copy)
│       │   │   ├── result/    Outcome · CivilyError · LoadState
│       │   │   └── text/      pure formatting + parsing, reached per A7; bbcode/ = BbParser + tree
│       │   ├── data/          NsXml + one package per resource
│       │   ├── feature/       one package per feature; a family inside it → its own subpackage
│       │   ├── nav/           Routes · NavActions · CivilyNavHost
│       │   └── ui/
│       │       ├── theme/     every on-screen colour, size, duration; the theme
│       │       └── component/ composables reached per A7
│       ├── main/res/          strings · census names · newspaper strips (drawable-nodpi)
│       ├── release/generated/ Baseline Profile, a checked-in build input
│       └── test/              JVM tests; resources/ = whole captured responses
└── benchmark/                 :benchmark, com.android.test, ships nothing
Stately/                       frozen Java app
fastlane/                      Stately store metadata
```

| Thing | Goes in |
|---|---|
| Formatting or parsing, no Android, reached per A7 | `core/text/` + test |
| Formatting or parsing, no Android, one feature | `feature/<feature>/` + test |
| Wire shape | `data/<resource>/…Dto.kt`, `@Serializable` |
| Screen shape, where mapping does real work | `data/<resource>/<Type>.kt`, mapped in the repository |
| Request | `data/<resource>/…Repository.kt`, shards in its companion |
| Screen state | `feature/<feature>/…ViewModel.kt` |
| Composable, one feature | `feature/<feature>/` |
| Composable, reached per A7 | `ui/component/` |
| On-screen colour, size, duration | `ui/theme/` |
| Long-lived object | `by lazy` in `AppGraph` |
| Route | `Routes` |

## 3. Data flow

```
Composable   collectAsStateWithLifecycle(); renders state, raises events
   ↕
ViewModel    MutableStateFlow<LoadState<T>>, filled by launchLoad
   ↕
Repository   withContext(Default): request → decode → map → Outcome<T>
   ↕
NsClient     rate limit · credentials · Dispatchers.IO · Outcome<Response>
   ↕ HTTPS
nationstates.net
```

- A ViewModel is built by its own `factory(…)` from repositories off `context.graph`, the UI's only
  path to `AppGraph`.
- A DTO goes straight to the screen when mapping would only copy fields. A screen type exists only
  for real work: entities, BBCode, `"0"` → null.
- Credentials attach only in `NsClient`. The password exists in three places: the sign-in field ·
  the `signIn` parameter chain · one `X-Password` header. Stored: one autologin token per nation, in
  `SessionStore`'s own preferences file.

## 4. Shapes

Three screen shapes. No others.

| Shape | For | Built from | Screens |
|---|---|---|---|
| Read | one thing, loaded and shown | `LoadState<T>` + `LoadStateScaffold` | nation · region · RMB · issues |
| Read, own chrome | chrome that stays while content loads | `LoadState<T>` + `Scaffold` + `LoadStateContent` | World Assembly |
| Act | a fourth state: mid-answer, mid-sign-in, mid-search | own sealed state + `Scaffold` | issue detail · sign-in · home |

- `LoadStateScaffold` owns back + collapsing title. `LoadStateContent` owns pull-to-refresh + the
  failure branch and its way forward. `Ready.refreshing` keeps content through a reload. No screen
  reimplements any of it.
- A per-feature state type only for a genuine fourth state.
- Section = one feature on another's screen. Home mounts `AccountsSection`; it mounts
  `IssuesLinkRow`. Import the section composable + its ViewModel. Never the repository, never
  internals.
- A repository takes `NsClient` (+ `SessionStore` if it needs the session), names its shards in its
  companion, returns `Outcome`. Catch boundaries: `RULES.md` §5.

## 5. Naming

| Thing | Form | Example |
|---|---|---|
| Concept | one noun, from folder to class to string | `region`: `data/region` · `RegionRepository` · `feature/region` · `RegionScreen` |
| Package | lower, the concept's noun | `feature/nation` · `feature/issues` |
| Wire shape | `<Thing>Dto` | `NationDto` |
| Screen shape | the noun | `Nation` |
| Repository | `<Thing>Repository` | `RegionRepository` |
| Screen · state | `<Thing>Screen` · `<Thing>ViewModel` | `RegionScreen` · `RegionViewModel` |
| Section | `<Thing>Section` / `<Thing>Row` | `AccountsSection` |
| String resource | snake, prefixed by role or surface | `label_endorsements` · `action_back` · `rmb_from_embassy` |
| Test | `<Subject>Test`; method states the behaviour | `NationDtoTest` |
| Nation, region id | `NsId`, `snake_lower` | `the_north_pacific` |

The game's words are the app's. Scales, policies, ratings, classifications: named as NationStates
names them, in code and on screen.

## 6. Adding a screen

1. `data/<thing>/`: `@Serializable` DTO + repository → `Outcome`. Documented shards only, in the
   companion.
2. Parser test on real XML, missing-element case included.
3. `feature/<thing>/`: ViewModel via `launchLoad` + composable in a §4 shape.
4. Strings into `res/values/strings.xml` first.
5. `Routes` constant + builder; destination in `CivilyNavHost`, reached via `NavActions`.
6. Left open → `BACKLOG.md`. A choice someone would reopen → `DECISIONS.md`.

`CivilyJourney` walks it → regenerate the Baseline Profile.

## 7. Where today's code goes

Existence = placement (§0.7). A row moves whole when one of its source files is next edited for
another reason, or as the blocking part of a gate. Never a task on its own. A test moves with its
subject.

| Today | Goes to |
|---|---|
| `lookup`: `feature/lookup` · `LookupScreen` · `LookupViewModel` · `LookupState` · `Routes.LOOKUP` · strings named `lookup` | `home`: `feature/home` · `HomeScreen` · `HomeViewModel` · `HomeState` · `Routes.HOME` · strings named `home` |
