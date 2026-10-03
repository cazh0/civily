# Civily — Rules

Law. Violation blocks merge.
Structure → `ARCHITECTURE.md` · status → `BACKLOG.md` · reasons → `DECISIONS.md` · visual law →
`DESIGN_RULES.md`, sovereign in its domain.
Changes only when a rule changes or is added. Code cites sections (`RULES §3`): never renumber; a new
section goes last.

---

## 0. Trees

| Tree | Ships as | Rule |
|---|---|---|
| `civily-app/` | `dev.cazh0.civily` | Everything below. |
| `Stately/` | `com.lloydtorres.stately` | Frozen. Bug fixes only. |

Civily ≠ Stately's next version: own app id, own User-Agent, no shared code. The rewrite was
instructed, which exempts it from §3. `Stately/` is deleted only at parity — never earlier, never as
a side effect. A rule applies to `civily-app/` unless it names `Stately/`.

## 1. Priorities

Strict order; higher wins a conflict. Work order is `BACKLOG.md`'s, not this list's.

1. **Certainty** — no assuming, no guessing. Confused → search or ask.
2. **Stability** — no crash, no swallowed error. Every failure visible.
3. **Performance** — §4.
4. **Lightness** — fewest deps, smallest APK, least thread work.
5. **Simplicity** — least code. Every file and function obvious.
6. **UX** — fewest taps, glanceable, Material-consistent.

## 2. Engineering

| Rule | Mandate |
|---|---|
| **R1 Root cause only** | No catch swallowing a symptom · no guard for a null that cannot exist · no commented-out code · no `TODO: revisit` · no `@Suppress` hiding a true warning. Fix too big → `BACKLOG.md` row, breakage left visible. |
| **R2 Clean · fast · maintainable** | (a) No dead code, no duplicated logic, no abstraction under 2 callers. (b) No I/O, parsing or disk on the main thread · every scroller lazy · nothing in a composable that could run once · no leaked Context, listener or callback · no §4 regression. (c) Public signatures self-evident · every non-obvious decision carries `// Why:`. |
| **R3 One commit title** | An AI-assisted change ends in ONE `type(scope): subject`, ≤72 chars, stating what changed. Who commits: `CLAUDE.md`. |
| **R4 Docs mirror code** | A document sentence the code contradicts = defect, same weight as a failing test. Fix one, same commit. |
| **R5 A name is a claim** | Class, resource, shard, route, import: open its definition before it ships. |
| **R6 Gate or preference** | A structural rule without a build-failing check is a preference. A permanently non-blocking check is no check. |

## 3. Prohibited without explicit instruction

- New dependency, except to replace a dead one. New module. Migration beyond the instructed rewrite
  and `:benchmark` (§6).
- Any host but nationstates.net. Analytics, crash SDKs, ads.
- Undocumented shards.
- Bypassing the rate limiter or credential store: `DashHelper.addRequest` / `PinkaHelper`
  (`Stately/`), `RateLimiter` / `SessionStore` (`civily-app/`, reachable only via `NsClient`).
  Connections open only in `NsClient` (API) and `AppGraph.imageLoader` (artwork), on one
  `OkHttpClient`.
- Credentials in logs, Intent extras, backups, or off-device. A password lives only for the request
  that trades it for a token: never a field, never saved state, never persisted.
- Colour, radius, size or duration literals outside `res/values` (`Stately/`) or `ui/theme`
  (`civily-app/`). Sizes → `Dimens` · durations → `Motion`.
- Decoding, parsing or mapping off a background dispatcher. `NsClient` backgrounds only the network
  call → every repository method that decodes wraps its body in `withContext(Dispatchers.Default)`.
  BBCode is parsed there, never in a composable.
- Defeating a check the game set against scripts. Border Patrol (403 demanding JavaScript + cookies):
  no browser User-Agent, no headless browser, no solver, no borrowed clearance cookie. Behind it =
  not ours.
- Regex on NationStates markup. `BbParser` is the sole entry for all NationStates content,
  game-written prose included.
- A raw API value on screen. Ids → `NsId.toName` · counts → `Numbers` · populations → `Population`.
- An irreversible act in one tap. Two aimed acts: choose, then commit. The commit says what it does
  ("Enact this legislation", never "OK"). No confirm modal.
- A dead-end failure. Every failed state offers a way forward. An expected empty result is never
  styled as an error.
- New database tables.
- A hardcoded rate limit. `RateLimit-Limit` / `-Remaining` / `-Reset` govern; 50 per 30 s is a
  bootstrap default only.
- Ignoring `Retry-After` on a 429. Repeat lockouts escalate to 15 min or more.
- A single-shot POST for any Private Command but `issue`. Two steps: `mode=prepare` → token →
  `mode=execute`. A token is good once.
- Growing `SparkleHelper` (`Stately/`, 1,664 lines). No equivalent in `civily-app/`.

## 4. Performance

| Metric | Target |
|---|---|
| Cold start → first screen | < 1.5 s |
| Screen transition | < 300 ms |
| List scroll | 60fps, zero jank frames |
| Main-thread block | < 16 ms |
| Release APK | ≤ previous version |

No regression ships. Measured on release builds only; a debug timing is not a defect. One recorded
APK exception: the Baseline Profile, ~120 KB. Nothing else grows it.

1. Every scroller is a lazy list. Prose enters via `richTextItems`, a paragraph per item.
2. A block carries its own bottom gap. Never the list's `verticalArrangement`.
3. Nothing derived twice: remembered against its input; a formatter built once per thread.
4. Intrinsic measurement only where a row needs its tallest child.
5. No file read on the main thread, the session store's first read included.
6. Nothing ticks unseen. A clock is gated on the lifecycle and wakes when its display changes, never
   on a fixed interval.
7. A decoded bitmap never outlives what was taken from it.
8. Every cache size stated, never defaulted.
9. The Baseline Profile is recorded by `:benchmark`, never hand-written, checked in, regenerated when
   the journey changes.

## 5. Robustness

- `Stately/`: network callbacks check `isAdded()` and that the activity is not finishing before
  touching UI. That tree has a history of fragment-state crashes.
- Results land in a ViewModel-owned `StateFlow`, collected with `collectAsStateWithLifecycle`. No
  callback holds a view. This closes that crash class; reintroducing a view-holding callback = defect.
- Only the visible destination navigates. Every navigation goes via `NavActions`, which drops the
  event unless its `NavBackStackEntry` is current. The test is the entry, never its lifecycle.
  `navController` wired straight into a callback = defect.
- Catch only at a parse or IO boundary; log AND surface a visible failure. Boundaries:
  `NsClient.execute` · `decodeNsXml` · `IssuesRepository`'s handoff from `IssueHtmlParser`.
- No static Activity or View Context.
- Rejected API session → session dropped: a 401/403 on an authenticated API request means the token
  is dead. A site form page is not an API shard: a 403 there never forgets the account.
- Sign-out forgets one nation and never promotes another. Any removal, by the user or a rejected
  token, leaves no nation active.
- A sent issue answer is never reported as failed. Before the POST: site-form failure → `c=issue`.
  After: unreadable result → `IssueResultUnknown`, list refreshed.
- `X-Pin` wherever a session exists. `X-Password` and `X-Autologin` log in; a login cancels the prior
  session, browser tab included; two quick logins → 409. Switching nations = local write, never a
  login. Background work never logs in per request.
- API text arrives damaged. `NsText.repair` runs on every body before decoding. `HtmlEntities`
  decodes exactly twice and leaves unknown entities as written.
- Unit tests mandatory on parsing, rate limiting, BBCode/HTML. New DTO → parser test with a
  missing-element case. A parser that must skip sections the app does not model → tested on a whole
  captured response in `src/test/resources/`, never an excerpt.
- One PR, one concern.

## 6. Scope

Absence = decision. Open gaps → `BACKLOG.md` · deliberate absences → `DECISIONS.md`. Nothing stubbed
to look working.

- **Out:** DI framework · module beyond `:app` + `:benchmark` · offline cache layer · any
  non-NationStates endpoint.
- **English only.** The game's content is English only; no translations. Dates and numbers in
  `Locale.US`.
- **minSdk 21.** Every supported device stays supported. Raising it = a decision with its reason in
  `DECISIONS.md`.
- **`:benchmark` ships nothing.** `com.android.test`. No product code depends on it or exists for it.
  A journey targets on-screen words, never an added test tag.
- **The graph is wired by hand in `AppGraph`.** A DI framework is considered only when that file
  outgrows one screen.
