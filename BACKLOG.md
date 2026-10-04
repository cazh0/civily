# Civily — Backlog

Open work, ranked. Done work → the code and `git log`.

**Order:** severe defect (`CLAUDE.md` §2) · rule broken today · rule unenforced · Stately parity
(`Stately/` cannot be deleted before it) · product gaps · measurement.
**Ranked:** changes behaviour or removes a live defect. Waiting on the game → last section, unranked.
Inside a section: a row sits above one that is harder to do right while it stays wrong.

---

## P0 — Rules hold by habit

| # | Task | Done when |
|---|---|---|
| T3 | Nothing runs the gates on its own | A push cannot land red: CI job or pre-push hook, the owner's pick. |

## P1 — Stately parity

Parity = a player moving over loses nothing Stately gave them.

| # | Task | Done when |
|---|---|---|
| T4 | No telegrams | Read, write, reply, organise, inside the telegram limit that sits on top of the global one. |
| T5 | No notifications | New issues and telegrams notify with the app closed, polling with `X-Pin`, never logging in per poll. |
| T6 | No WA voting | A member votes from the WA screen through two-step `prepare` → `execute`, which arrives with this. |
| T7 | RMB read-only, newest 50 only | A member posts; older pages load. |
| T8 | No endorsing, no moving | Endorse and un-endorse a nation; move to a region. |
| T9 | Rankings are readouts | A row opens its census history (`mode=history`). |
| T10 | No activity feed | Happenings of the nation, its region and followed nations, as one chronology. |
| T11 | No World screen | Featured region, featured census and breaking news, as Stately shows them. |
| T12 | No region polls | A poll reads; a member votes. |
| T13 | No dossier | Nations and regions added to and read from the dossier. |
| T14 | One theme | The reader picks a theme and light/dark. Stately has five. |

## P2 — Product gaps

| # | Task | Done when |
|---|---|---|
| T15 | Rich text drops 5 tags | `[table]` `[img]` `[size]` `[align]` `[box]` render: each a parser case, a renderer case, a test. |
| T16 | A census scale reads "Unknown" | Every returned scale carries its real name. |
| T17 | Sign-in predates home's card language | It reads as the same app as home. |
| T18 | Nation grid is phone-first | A wide screen gets more columns, not wider tiles. |
| T19 | Loading is a spinner | A loading screen holds the shape of what is coming. |
| T20 | No accessibility pass | TalkBack order useful · every control labelled · every screen survives 200% font. |
| T21 | `SessionStore`, `NsClient` untested (need a `Context`) | Reading, writing and dropping a stored account is tested. |

## P3 — Measurement

| # | Task | Done when |
|---|---|---|
| T22 | 3 rows of `RULES.md` §4 unmeasured | Transition time, longest main-thread block, release APK size: each has a number. |
| T33 | Profile journey flakes: `scrollPane` reads a node gone stale (`StaleObjectException`, `CivilyJourney.kt:118`, 1 of 2 runs) | The journey re-finds the pane instead of holding it; three runs in a row pass. |
| T23 | Frame benchmark reports a count, not durations | A run reports per-frame durations. Today `FrameTimingMetric` gives `frameCount` only here, and `benchmark` 1.5.0-beta01 does not build. |

---

## Waiting on the game — preconditions, unranked

Doable only once NationStates sends what no capture holds yet. Never the task.

| # | Condition | Satisfied when |
|---|---|---|
| T24 | No WA badges or GA/SC votes on the nation screen | A response with non-empty `wabadges` / `gavote` / `scvote` is captured as a fixture. |
| T25 | Nothing knows Z-Day | A `zombie` response from Z-Day is captured. |
| T26 | `REMOVED_POLICIES` never seen live | An answer cancelling a policy is captured; Canceled Policies is tested on it. |
| T27 | Reclassify types `1`, `2`, `govt` unconfirmed (only `0` seen) | Each is confirmed by a captured answer. |
| T28 | Aftermath papers lack photos | The API offers the `legislation-papers` cutouts outside Border Patrol. Then one mapper. |
