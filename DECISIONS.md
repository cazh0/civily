# Civily — Decisions

Why. One line each. Not a changelog. A decision that becomes a rule moves to `RULES.md`; its reason
stays here.

---

## Scope

- **A rebuild, not Stately's next version** — own id, own User-Agent, no shared code: both install side by side, and `Stately/` stays the API reference until parity.
- **Border Patrol declined** — `/page=show_dilemma/` answers 403 to anything not a browser, anonymous desktop included. Walking past a wall the game built costs more than the cutouts behind it. Enacting goes through `c=issue`; aftermath papers print without photos. The photo parser and geometry stay tested on a captured page (T28).
- **No RMB likes** — the documented API has only `rmbpost`; Stately used undocumented `/page=ajax3/` (`RULES.md` §3). The count is a text readout: a pill invited a press that could never work.
- **No size adjective in the summary** ("a massive nation") — no public shard carries it; every listed shard was tried live, `nstats` and `legislation` included. Invented thresholds = a guess printed as fact.
- **Postcards carry no caption** — `UNLOCKS` gives a filename code. Stately's 321 titles are a hand-written table, not the API's, a decade stale. No text → no caption, no content description.
- **An unknown reclassify code is dropped and logged** — Stately defaults it to civil rights, printing a change that never happened.
- **A census id past the list reads "Scale N"** — honest, never hidden.
- **No WA badges or votes on the nation screen** — every sample returned them empty: nothing to test against (T24).
- **The ambient plate is one colour, not a blur** — `Modifier.blur` needs API 31; minSdk is 21.
- **No chart library** — the one chart is three arcs and two circles on a `Canvas`.

## Network and session

- **Sign-in requests `ping`** — public shards alone succeed with any password; a private shard forces authentication, and the docs name `ping` for exactly that. No `X-Autologin` in the reply = rejected, whatever the status.
- **A token and PIN per nation** — NationStates sessions are per nation, so a switch only changes which pair is sent. A login would cancel the replaced session, browser tab included.
- **A rejected token leaves no nation active, even with others stored** — silently becoming another nation = reading another nation's issues. The card opens on "Choose a nation" and never asks for a password the device holds a token for.
- **`allowBackup` off** — a backup would send the autologin tokens to Google.
- **The issues button makes its own request, `unread` + `nextissuetime`** — a count and an instant, against every issue's prose and options, on a row reloaded at every landing, including just after an answer, when the old count is certainly wrong. The count is stamped with its nation, because a switch costs no request.
- **Timeouts 15 s connect · 30 s read · 60 s call, not Stately's 120 s** — the limiter paces requests, so a stall is a failure, not backpressure.
- **Images share the API's `OkHttpClient`** — otherwise Coil opens a second pool and the first flag pays a fresh TLS handshake. The client holds no `Cache`, as Coil requires of a shared one.
- **Image cache headers ignored** — they expire flags in hours; a flag changes only when redrawn.

## Architecture

- **Navigation gated on the current entry, not the lifecycle** — a popped screen takes taps through its exit; one late tap pushed an issue detail with no list beneath and crashed the shared `IssuesViewModel`. `RESUMED` would wait out every arrival animation; "current" is true at once. It also stops a double tap pushing twice.
- **`LoadState` shared, `launchLoad` an extension, not a base class** — read screens share exactly three states; their ViewModels differ in everything else, and a base class would weld them.
- **A DTO reaches the screen directly when mapping only copies** — a mapper with no work is a second definition of one shape. Sign-in and search read `NationDto`; only the nation screen gets `Nation`.
- **The graph is hand-wired** — it fits one screen; Hilt = an annotation processor on every build and edges in generated code.
- **Entities decoded exactly twice** — CDATA leaves the XML unescaped, and NationStates escapes stored HTML on the way out: `&amp;amp;#43457;` is `꧁`. A third pass eats text an author escaped on purpose. A numeric C1 reference → the Windows-1252 character meant. A Private Use code point = NationStates' icon font, not shipped → dropped, never drawn as tofu.
- **`@@nation@@` and `%%region%%` are tags only in happenings** — a factbook may contain `%%`; reading it as a link eats half the sentence.
- **Emphasis in a composed sentence uses control characters, not `<b>` + `fromHtml`** — the sentence carries the player-typed demonym, and an `&` in it would parse as an entity and eat the rest.

## Performance

- **Only release timings count** — debug: ~700 ms cold start, missed vsyncs. Release: ~190 ms, none. Debug has no R8, no inlining, no AOT.
- **The Baseline Profile earns its ~120 KB** — Pixel 8 Pro, ten runs: 283 ms median with it, 297 ms with no AOT. It matters more the slower the CPU.
- **Profile generation sits outside the build** — a build needing a phone is not a build. Checked in; regenerated when the journey changes.
- **`androidx.benchmark` pinned at 1.4.1** — 1.3.3 cannot parse this platform's `gfxinfo`; 1.5.0-beta01 breaks the app's compilation on Gradle 8.11.1.
- **Daemon JVM pinned to 21** — AGP rejects newer JVMs; unpinned, the build depends on the JDK on PATH.
- **Transitions 110 ms in, 80 ms out** — the 700 ms default each way is time spent watching a finished screen. A fade costs one alpha layer, no per-frame relayout.
- **`FactGrid` packed by hand** — a lazy grid inside a same-axis scroll has no bounded height, and a dozen facts gain nothing from laziness. Sizing a row to its tallest tile is the one intrinsic measurement; Compose names intrinsics as the answer to measuring a child twice.
- **The plate sampler sets `allowHardware(false)`** — from API 26 a hardware bitmap has no CPU-readable pixels and sampling it throws. The one sampler asks for what it needs; no catch.

## Issues and newspapers

- **Issues are newspapers, in the game's three papers** — broadsheet, tabloid, berliner. The paper is a function of the issue id: the same page in the list, the detail, tomorrow. Three designs over a four-city cycle realign every 12th issue, so every design meets every city.
- **The newspaper ignores font scale** — its type is sized from its width; a masthead reflowed at 200% stops being a masthead. Long names shrink to fit.
- **Set in the device's `sans-serif-condensed`, not the frame's face** — a display font costs tens of KB against the APK row. Two faces at one point size differ in cap height, hence type stated as cap height.
- **A piled tabloid keeps its headline size and prints on deeper paper** — as `newspaperStack.svg` does. Beyond six lines it is ellipsised, visibly.
- **No front page above the aftermath** — its headlines are the papers; one more above them says it twice.
- **Aftermath order: talking point · reclassification · headlines · new policies · canceled policies · postcards · trends** — a sentence and the word it moved are one thought; then the answer's own voice; then law; then keepsake; then numbers. A section with nothing is absent.
- **A reclassification is never coloured** — `FreedomLadder` gives direction, `FreedomRating` gives worth, and they disagree: World Benchmark → Excessive *rose*. "Rose" in warning red contradicts itself.
- **A cancelled policy prints without its banner** — two identical cards under two headings is where enacting and repealing get confused.
- **The wait for the next issue is the empty state, and it ticks** — seconds on the issues screen, minutes on the home button. It reloads at zero, keyed on the awaited instant, so it is never a poll.
- **No confirm dialog on enact** — the commit inside the chosen option says what it does; a dialog repeating it is a third tap with no third thought, and trains dismissal.

## Nation

- **Seven tabs in the game's order** — Overview, Policies, People, Government, Economy, Rankings, Happenings, as Stately mirrors. The pinned bar lets a reader leave Rankings without scrolling up.
- **A freedom rating is coloured by its word, never its score** — swept live, the top of every ladder turns bad: maximum civil rights is "Frightening", maximum political freedom "Corrupted". Stately's `score / 7` ramp paints both green. An unknown rung gets no tint.
- **Slices keep enum order and colour; the legend shows percentages** — unlabelled wedges are told apart by colour, and sorting would recolour per nation. The site and Stately hide the numbers behind a tap; the numbers are the point.
- **"Animal Attack" becomes the nation's animal** — the API names every mauling that way; the site relabels it too.
- **No budget total** — the API gives the split, not the size. Stately's GDP × share "Total" is a different quantity.
- **Rankings in scale order, percentile as the pill** — a reader looks for one scale, and a reordering list must be read, not scanned. "Top 3%" needs no world count.
- **Composed sentences change between refreshes** — the game itself rotates `NOTABLE` and `ADMIRABLE`.
- **Eight moved statistics** — one answer nudges ~30 scales; the rest is rounding. Badge sprites have no documented address → a tile with the scale's initial.

## Design

- **The "Your nation" card is the switcher** — the card already pressed for the dashboard; with one nation it looks unchanged. ≤5 keeps the open card a glance; alphabetical keeps the next row to press where it was.
- **The issues badge is `primary`, not `error`** — red means broken; an issue waiting is why the player came.
- **List flags crop to fill** — a fitted flag floats in empty plate. A chip is for recognition; the whole flag is one tap away.
- **The plate is mixed from the flag, and the picture decides how** — a fixed pale plate = a lit slab at night; a theme plate swallows dark-on-transparent flags. Opaque flag → surface + a wash of its colour. Transparent → a plate pushed away from the artwork's brightness: legibility beats blending.
- **Posts are not cards** — fifty outlined cards give fifty posts equal weight: a spreadsheet, not a conversation.
