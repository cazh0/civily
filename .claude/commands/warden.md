---
description: Start a working session. Reads the law, runs the gates, names the task.
---

Task: $ARGUMENTS

In order. Nothing else until the report.

1. Read `ARCHITECTURE.md` and `RULES.md`, every line.
2. From `civily-app/`: `./gradlew :app:testDebugUnitTest`. Red → stop; report the failing test and
   the fix. No SDK → say so, name what did not run.
3. Name the task. Given above → that one, no argument. Else the highest open `BACKLOG.md` row:
   sections top-down, rows top-down (`CLAUDE.md` §2). Never the cheapest unblocked. Too big → still
   the task, owned to the end. Never **Waiting on the game**. Never a move-only row; take the next.
4. Touches a screen → read `DESIGN_RULES.md`. Reverses something, or a choice in the code looks
   arbitrary → read `DECISIONS.md`. Skipped → say which.
5. Open every file the task touches, whole, before planning.

Report exactly:

```
Gates      <green · or the failing test, plainly>
Task       <the task; if self-chosen, why it outranks the next row>
Touches    <files, one line>
Unread     <skipped documents, or "none">
First step <the one change about to be made>
```

Then:

- Task named in the invocation → start after the report, never instead of it.
- Task self-chosen → stop and wait. The owner decides.
- Silent (`CLAUDE.md` §3). Break only for a failure, a blocker or an owner-only choice, in one line.
- A piece done ≠ the task done. Continue to the end. Stop early only for an owner-only choice or
  ~500k tokens of context, on a finished piece.
- End in one report: line per file · gates in one word · what works · what is open · one next action
  · the commit title.
