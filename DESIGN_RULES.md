# Civily — Design Rules

Visual law. Sovereign in its domain. Violation blocks merge.
Token values → `ui/theme/`. Inventory → the code. Changes only when a rule does.

---

## 1. Colour and tokens

1. No literal outside `ui/theme/` (`RULES.md` §3), except zero and a paper's frame measurements
   (§6.1), which stay beside its drawing. A feature speaks `MaterialTheme` and `ui/theme`. Nothing
   else.
2. Colour comes from the wallpaper. Dynamic colour on Android 12+; the indigo scheme is the fallback,
   not the intent.
3. A colour that ignores the wallpaper carries meaning or identity, and `Color.kt` says which: trend
   pair · chart and avatar palettes · newspaper inks.
4. One green/red pair carries every judgement — moved statistic and freedom rating alike. Light +
   dark tone (M3 40 / 80), ≥4.5:1 in both themes. A judged tile = its accent at M3 state-layer
   opacity, composited over its surface, opaque.
5. Red = broken. `error` is for failure only. Empty ≠ failure. Good news (an issue waiting) =
   `primary`.
6. Typography and shapes: Material 3 defaults.

## 2. Components

1. One definition per family, placed per `ARCHITECTURE.md` A7. A recoloured copy fails.
2. Every screen is an `ARCHITECTURE.md` §4 shape.
3. A list of things is not a list of cards. Post = avatar, name, time, whitespace. Removed post = one
   thin italic line.
4. Short parallel facts = a grid, never a stack of cards. Row height = its tallest tile · an absent
   shard drops its tile before packing · a long fact takes a full row · every grid titled.
5. A share of a whole = the game's ring: hole at 0.6 of the radius, a faint band inside, unlabelled
   wedges, a wrapped legend with percentages. Never sorted: a slice's colour is its enum position.
   A zero slice is dropped.
6. Flags are 3:2 and fill their tile. Rows crop; the nation screen fits the whole flag. No flag →
   initials, same box.
7. A flag's plate is mixed from the flag, and holds the theme surface until the flag decodes.

## 3. Layout

1. Edge-to-edge. Every `Scaffold` consumes its insets.
2. A tab bar is pinned under the top app bar. Each pane owns its scroll. The selected tab is held
   above the load state, so it survives refresh, rotation and retry.
3. The game's organisation is the app's. Nation: the game's seven subjects, in its order. Rankings:
   the game's scale order.

## 4. Copy

1. Every user-visible string is a resource. No literal in a composable.
2. Game prose prints as written, via `BbParser`. A composed sentence appears only when every shard it
   needs arrived.
3. Print nothing the app does not know: no invented adjective, no guessed caption, no total in a unit
   the API never gave. Absent is honest.
4. Sentences read as sentences: "3 hours ago", never "3h". Plurals live in resources.

## 5. Behaviour

1. An irreversible commit sits inside the option it acts on (`RULES.md` §3). Selected = filled
   container AND border.
2. An inert control never looks like a control. A readout is text, never a pill.
3. Never link to the screen the reader is on.
4. A count at zero draws nothing. A count takes the chevron's slot, never a place beside it.
5. Every read screen ships loading, empty and failure via `LoadState`. "Not found" = a neutral inline
   card that keeps the user's text editable.
6. The "Your nation" card is the account switcher. ≤5 nations, alphabetical. Add and sign out sit at
   the foot of the unfolded card.
7. Nothing shifts under the eye. A ticking figure is tabular and writes every field, even at zero.
8. Transitions: `Motion`'s durations, fade only.
9. Light, dark, phone, font scale: always in scope. Only the newspaper ignores font scale
   (`DECISIONS.md`).

## 6. Newspapers

1. A paper transcribes its frame. Every coordinate = a fraction of the component's width, taken from
   the frame's canvas.
2. The game's strips are fixed artwork, in `drawable-nodpi`.
3. Type is stated as cap height, never point size.
4. An issue's paper = `Newspaper.design`. `NewspaperForIssue` is the sole door: every single-issue
   screen, every sheet of the pile.
5. An artwork URL is used exactly as written, never rebuilt from a stem.
6. A photo window with no photo is floored with newsprint.
7. A pile sheet is set down where the sheet above stops printing. Every headline shows whole.

## 7. Verification

1. A visual claim = a device screenshot, or it is stated as unverified. Compile ≠ visual review.
2. Geometry is measured, never eyeballed. A transcription is checked against its frame's numbers.

## 8. Posture

1. A user directive overrides every rule here: named value, named scope, verbatim.
2. Root cause, never call site. A wrong colour is a wrong token.
3. Match the reference — the supplied frame, the game's page — on first authoring.
4. Perfection is the floor. Meeting the standard = finished; reopening needs a rule it breaks.
