---
description: Sweep the repository for drift. Three tasks worth doing, plus the three after them.
---

Measure everything. Report little. Assert nothing unmeasured. A check that reads no files = a finding,
not a pass.

## Measure

From `civily-app/`: `./gradlew :app:testDebugUnitTest`. Read its output; never re-measure by hand what
it reports.

- **Gates** — an `ARCHITECTURE.md` §1 rule with no gate · a gate never seen red · a gate reading a
  constant instead of the tree. A defect class that shipped green → name the gate that would have
  caught it; writing it outranks the three slots.
- **Rule against rule** — two rules that cannot both be obeyed · a gate enforcing less than its text.
- **Documents** — content `ARCHITECTURE.md` §0 forbids · contradicted by code · an eighth document ·
  a code citation (`RULES §n`, a document name) to a section or file that no longer says it · a
  copied number gone stale.
- **What no gate reads** — a shard requested, never read · a DTO field never mapped · a string
  resource never used · an inert control · a screen claiming what it does not do · a name resolving to
  nothing. Open the definition: a name is not evidence.
- **Rules nothing checks yet** — a literal under `feature/` · an import up a tier · a connection
  outside `NsClient` and `AppGraph` · a literal string in a composable · a decode on the caller's
  thread · `navController` wired straight into a callback.
- **The gap** — `ARCHITECTURE.md` against the tree and §7. One Health line, never a slot.

## Report

≤40 lines. Plain words. No nested tables. `file:line` only where the fix goes.

```
## Verdict
<one sentence: what is true, and the largest gap>

## Do these three
1. <sentence>  →  <command or file>
2. <sentence>  →  <…>
3. <sentence>  →  <…>

## Then these three
4. <line>   5. <line>   6. <line>

## Health
Gates      <n> live, <n> dormant, <n> red — <the red one, plainly>
Gap        <described, absent>
Documents  <sound | n problems>
Safety     <anything a player's credentials or nation choice depends on>

## Also found
- <≤6, ranked, one line each>

## Chores
<one line, comma-separated: uncommitted work, stashes, lint, dead config>

## Backlog
New: T<n> <name> · Worse: T<n> — says <x>, is <y> · Closed: T<n> | "none"

## Commit
<ONE title per `CLAUDE.md` §7, or "nothing changed">
```

## Slots

A slot = work that changes the app or removes a defect. Rank:

1. Correctness, and security when severe (`CLAUDE.md` §2): a rule the code breaks, a parser passing
   bad input, a screen misleading a player about their nation.
2. A defect class with no gate. The gate is the slot.
3. Cheap and permanent: one edit, never recurs.

- Never a slot: the architecture gap (closes when touched) · commit, stage, stash · formatting, lint,
  dead config, tidy renames → **Chores**.
- Never a slot, never a new row: what waits on the game → **Backlog** as `Precondition: T<n>`. An
  empty sweep beats manufactured work.
- A slot names one file, never a title. One title total, under `## Commit`.

## After

"Fix all 3" / "do it" → do them silently; reply with the next three in this shape; no re-sweep.
"Do it" = edit and leave dirty. Never stage, commit or push.
Nothing is fixed unless asked. End after the Commit line.
