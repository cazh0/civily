# Civily — Technical Specification

**This file commands. Not explain.** Feature surface is `README.md`.

## 0. Repository layout

Two trees. Rules below apply to both unless a row says otherwise.

| Tree | What | Status |
|---|---|---|
| `Stately/` | Java + Views app, 31.8k LOC. Shipping as `com.lloydtorres.stately`. | Frozen reference. Bug fixes only. |
| `civily-app/` | Kotlin + Compose rebuild from zero, shipping as `dev.cazh0.civily`. | Active. Own `README.md` documents its structure. |

The rebuild is **Civily**, not a new version of Stately: separate application id, separate
User-Agent, no shared code. `Stately/` keeps its name because it is Stately.

The rewrite was explicitly instructed, which is what lifts it out of §3. Old tree is deleted
only when the new one reaches parity — not before, and not as a side effect of anything else.

## 1. Priorities (strict order)

1. **Certainty** — don't assume, don't guess. Search if confused. Ask if confused.
2. **Stability** — no crashes, no swallowed errors. Every failure visible to user.
3. **Performance** — §4.
4. **Lightness** — fewest deps, smallest APK, least thread work.
5. **Simplicity** — least code. Every file/function obvious in purpose.
6. **UX** — fewest taps, glanceable, Material-consistent.

## 2. Engineering rules (violations block merge)

| Rule | Mandate |
|---|---|
| **R1 — Zero band-aid fixes** | Every defect fixed at root cause. No try/catch swallowing symptom, no null guard for a null that shouldn't exist, no commented-out code, no `// TODO: revisit`, no `@SuppressLint`/`@SuppressWarnings` silencing legit warning. If proper fix too big, open tracked issue and **leave broken behaviour visible** — never hide. Bar: reader six months later understands *why* from code alone. |
| **R2 — Clean, fast, maintainable** | (a) **clean** — no dead code, no duplicated logic, no abstraction w/o ≥2 concrete callers; (b) **fast** — no I/O, parsing or DB on main thread; no blanket `notifyDataSetChanged`; in `civily-app/`, no scrolling container that is not lazy and no work in a composable that could have been done once (a derived string, a parsed span, a formatter); no leaked Context/listener/callback; no regression vs §4; (c) **maintainable** — every public method obvious from signature, every non-obvious decision explained w/ `// Why:`. |
| **R3 — Commit-title acknowledgement** | After every AI-assisted response proposing/applying changes, state one Conventional Commits title (`type(scope): subject`, ≤72 chars). Title only, no body. |
| **R4 — Spec mirrors code** | Change altering anything stated here updates this file **same commit**. |

## 3. Prohibited without explicit instruction

- New dependency — except to replace a dead one. New module. Further architecture migration beyond the instructed `civily-app/` rewrite and the instructed `:benchmark` module (§6).
- Any endpoint other than nationstates.net. Analytics, crash SDKs, ads.
- Undocumented NS shards.
- Bypassing the rate limiter or the credential store — `DashHelper.addRequest` / `PinkaHelper` in `Stately/`, `RateLimiter` / `SessionStore` in `civily-app/`. In `civily-app/` both are reachable only through `NsClient`; opening a connection anywhere else is the violation.
- Credentials in logs, in Intent extras, in backups, or off-device. A password may be held only for the duration of the request that exchanges it for a token — never in a field, never in saved instance state, never persisted.
- Colour / radius / size / duration literals outside `res/values` (`Stately/`) or `ui/theme`
  (`civily-app/`). In `civily-app/` durations live in `Motion` for the same reason sizes live in
  `Dimens`: an animation length typed at a call site is a design decision nobody can find again.
- Decoding, parsing or mapping outside a background dispatcher. `NsClient` backgrounds only the network call, so every repository method wraps its body in `withContext(Dispatchers.Default)`. BBCode is parsed there, never in a composable.
- Defeating a check the game put up to keep scripts out. Border Patrol — the 403 interstitial demanding JavaScript and cookies — is a decision by NationStates about who may read its HTML pages. No browser-impersonating User-Agent, no headless browser, no challenge solver, no clearance cookie lifted from anywhere. A page behind it is a page this client does not have.
- Regex-based handling of NationStates markup. It nests and it is frequently malformed; `BbParser` is the one entry point.
- Rendering a raw API value to the user. Ids go through `NsId.toName`, counts through `Numbers`, populations through `Population`. A screen printing `nation.population` directly is the defect, not the formatter's absence.
- An irreversible action reachable in one tap. It takes two separately-aimed actions — choose, then commit — and the commit control must say what it does ("Enact this legislation", not "OK"). A modal that repeats the sentence already on the button is a third tap without a third thought; do not add one.
- Dead-ending on a failure. Every failed state carries a way forward; an expected empty result is not styled as an error.
- New DB tables.
- Hardcoding the rate limit. The API documents `RateLimit-Limit` / `-Remaining` / `-Reset` and instructs scripts to use them; the 50-per-30s constant is a bootstrap default only.
- Ignoring `Retry-After` on a 429. Repeated lockouts escalate from one window to fifteen minutes or longer.
- Single-shot POST for any Private Command except `issue`. They are two-step: `mode=prepare` returns a token, `mode=execute` spends it, and a token is good once.
- Growing `SparkleHelper` — already a 1664-line god class. No equivalent may appear in `civily-app/`.

## 4. Performance targets

| Metric | Target |
|---|---|
| Cold start to first screen | < 1.5s |
| Activity / fragment transition | < 300ms |
| List scroll | 60fps, zero jank frames |
| Single main-thread block | < 16ms |
| Release APK size | No larger than previous version |

`civily-app/` measures the first and third of these, in `:benchmark` — a Macrobenchmark module
added on explicit instruction, which ships nothing and runs against a connected device. Cold start
is 283ms median on a Pixel 8 Pro with the Baseline Profile it now carries. The other three rows are
still unmeasured, and nothing runs any of it automatically: there is no CI, so no change is stopped
by getting slower. Figures and method are in `civily-app/README.md`.

The APK row has one recorded exception, taken knowingly: shipping a Baseline Profile costs ~120KB
of `profileinstaller` and profile data, against a cold start it measurably improves. Nothing else
may grow the APK by accident.

`Stately/` measures none of these.

## 5. Robustness

- `Stately/`: network callbacks check `isAdded()` and that the activity isn't finishing before touching UI — this code has a history of fragment-state crashes.
- `civily-app/`: request results land in a ViewModel-owned `StateFlow`, collected with `collectAsStateWithLifecycle`. No callback may hold a view. This is what closes the crash class above; a screen that reintroduces a view-holding callback is the defect.
- Only the destination the user is looking at may navigate. A popped screen stays composed — and keeps taking taps — for the length of its exit transition, so a tap arriving a frame late is delivered by a screen that has already left the back stack; acting on it pushes a destination whose parent is gone. In `civily-app/` every navigation goes through `NavActions`, which drops the event unless its `NavBackStackEntry` is still the current one. The test is the entry, not its lifecycle: `RESUMED` waits out the arrival transition as well, which makes the permission a function of animation length. A screen wiring `navController` straight into a callback is the defect.
- Catching is allowed only at parse/IO boundaries, and must both log **and** surface a visible failure state. Silent failure = defect. In `civily-app/`, IO is `NsClient.execute`, XML is `decodeNsXml`, and the issue-site HTML path logs at the `IssuesRepository` handoff from `IssueHtmlParser` to user-visible `Outcome`.
- No static references to Activity/View Context.
- A rejected API session request drops the session. `X-Autologin` is valid until the nation's password changes and an expired PIN falls back to it, so a 401/403 on an authenticated API request means the token is dead for good. Site form pages are not API shards: a 403 there can be page/form state after an otherwise valid session, so it never forgets the account. Before the irreversible issue POST, a site-page/form parse failure falls back to the documented `c=issue` command so the feature keeps working; after the issue POST is sent, result fetch/parse failure becomes `IssueResultUnknown` and refreshes the issue list instead of claiming the legislation failed. That fallback is the live path: `/page=show_dilemma/` answers 403 with the site's Border Patrol bot check. Satisfying that check is prohibited — see §3.
- Authenticate with `X-Pin` wherever a session exists. `X-Password` and `X-Autologin` each perform a *login*, and a login cancels the previous session — including the user's own browser tab. Two logins in quick succession return 409. Background polling in particular must never re-login per poll.
- Unit tests mandatory on parsing, rate limiting, and BBCode/HTML.
  - `Stately/` — `dto/*`, `DashHelper`, `SparkleHelper`, `MuffinsHelper`. None exist.
  - `civily-app/` — `RateLimiterTest`, `NationDtoTest`, `RegionDtoTest`, `AutologinTokenTest`, `PercentCodecTest`, `NsUrlTest`, `NsIdTest`, `PopulationTest`, `AssemblyDtoTest`, `BbParserTest`, `BbColorTest`, `HtmlEntitiesTest`, `RelativeTimeTest`, `RmbDtoTest`, `IssueDtoTest`, `IssueHtmlParserTest`, `NsTextTest`, `InitialsTest`, `NewspaperTest`, `IssueResultDtoTest`, `PercentTest`, `AccountsTest`, `FlagAmbienceTest`, `FreedomRatingTest`, `MagnitudeTest`, `CountdownTest`, `IssueBadgeDtoTest` exist
    (232 tests). Every new DTO ships a parser test including a missing-element case.
    `IssueHtmlParserTest` runs against a whole captured aftermath page, `src/test/resources/issues/`,
    not an excerpt: the parser's job is to find the papers inside the site's own chrome.
- One PR, one concern.

## 6. Scope boundaries

Out of scope: DI framework, multi-module split beyond the instructed `:benchmark`, offline cache
layer, in-app translations (README's rule), any non-NationStates endpoint.

`:benchmark` is the one module besides `:app`, added on explicit instruction to measure §4 and to
record the Baseline Profile. It is `com.android.test`, it ships nothing, and no product code may
depend on it or exist for its benefit — a journey is written against the words on screen, never
against a test tag added to make it easier.

`civily-app/` wires its object graph by hand in `AppGraph`. Reach for a DI framework only when that file stops fitting on one screen.

**Absence is decision, not oversight (R1).** Gaps in the rewrite are listed in `civily-app/README.md`; nothing there is stubbed to look like it works.
