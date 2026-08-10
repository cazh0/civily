# Civily

A NationStates client for Android, in Kotlin and Jetpack Compose, built from zero on
`rewrite/kotlin-compose`. It began as a rewrite of Stately and shares no code with it; the Java
app in `../Stately/` is untouched and stays as the reference for API behaviour until this one
reaches parity.

Rules of engagement are `../CIVILY-SPEC.md`. Feature surface is `../README.md`.

## Layout

```
app/src/main/kotlin/dev/cazh0/civily/
├── core/            everything with no feature knowledge
│   ├── net/         NsUrl · NsClient · RateLimiter · UserAgent · AutologinToken
│   ├── session/     Session · Accounts · SessionStore (the only copy of credentials)
│   ├── result/      Outcome · CivilyError          (the only way failure travels)
│   │                LoadState · launchLoad          (the only shape a read screen has)
│   ├── text/        NsId · Numbers · Population · RelativeTime
│   │                HtmlEntities · NsText                (decoding what the API really sends)
│   │   └── bbcode/  BbParser · BbTree · BbColor     (NationStates markup, parsed not regexed)
│   ├── AppGraph     every long-lived object, hand-wired
│   ├── CivilyApp   Application
│   └── MainActivity the app's single Activity
├── data/            NsXml + one package per resource (dto + repository together)
│   ├── auth/        AuthRepository                  (the only place a password is used)
│   ├── issues/      IssuesRepository                (the only write the app performs)
│   ├── nation/
│   ├── region/
│   ├── rmb/
│   └── wa/
├── feature/         one package per screen (ViewModel + Composable)
│   ├── accounts/    AccountsSection                 (one card, so no ViewModel)
│   ├── issues/
│   ├── lookup/
│   ├── nation/
│   ├── region/
│   ├── rmb/
│   ├── signin/
│   └── wa/
├── nav/             Routes + NavHost
└── ui/
    ├── theme/       Color · Dimens · Theme          (the only colours and sizes)
    │                FlagAmbience                    (a plate mixed from a flag)
    └── component/   LoadStateScaffold · LoadStateContent · RichText
                     AmbientFlag · FlagHero · LinkRow · FactCard · NationAvatar
                     Pill · SectionHeader · LoadingState · EmptyState · ErrorState
```

`data/` holds two kinds of type. A `…Dto` is the wire shape, annotated for XML. A plain type
beside it — `Region`, `Assembly` — is the screen shape, and the repository maps between them.
The mapper exists where there is real work to do: parsing BBCode, and turning the API's `"0"`
sentinel into a null. Where there is no such work, `NationDto` goes to the screen directly
rather than through a mapper that only copies fields.

`LoadStateContent` is the three-case `when`; `LoadStateScaffold` is that plus a top bar. A
screen that owns its own chrome — the World Assembly's tabs — uses the former so the chrome
stays put while content below it loads.

## UI rules

**Nothing the API returns reaches the screen raw.** Ids become names through `NsId.toName`,
counts get grouped through `Numbers`, populations get scaled through `Population`. "49902
million" is what happens when a value is printed instead of rendered.

**"Not found" is not a failure.** A search that finds nothing is a normal answer and gets a
neutral inline card that keeps the user's text editable. Red warnings are for things that are
actually broken. Nothing in this app dead-ends on a full-screen error the user must press back
out of.

**Every screen is a `LoadStateScaffold`.** That is what guarantees a back affordance, a
collapsing title, pull-to-refresh, and a failure branch with a retry — none of which a feature
should be able to forget.

**Refreshing keeps its content.** `LoadState.Ready.refreshing` exists so a reload never blanks
the screen it is reloading.

**The "Your nation" card is the account switcher.** Not a menu in the top corner, not a second
section under it — the same card the user already presses to reach their dashboard. Closed, it
is one row: flag, name, region, and a caret. The caret unfolds the other nations *inside* that
card; pressing one folds the card back with that nation now at its head, which is the entire
gesture. Nothing about switching appears somewhere the user was not already looking, and with
one nation signed in the card looks exactly as it did before the feature existed.

Five accounts is the ceiling precisely so the unfolded card is still a glance, and they are in
alphabetical order rather than most-recently-used, because a list that reorders itself after
each switch moves the row the user is about to press again. Adding and signing out live at the
foot of the unfolded card: the head of the card means one thing — this is your nation, press it
to open it — in both of its states.

**Switching nations is a local write, not a login.** NationStates issues a session per nation,
so every stored account keeps its own autologin token and its own PIN, and switching only
changes which pair the next request carries. Nothing is fetched and nothing re-authenticates —
which matters, because a login cancels the session it replaces, including the user's own
browser tab.

**Signing out forgets one nation and never promotes another.** Removing an account — the
user's own choice, or a token NationStates rejected (spec §5) — leaves the app with no active
nation even when others are stored. Quietly becoming a different nation in response to a
failure is how somebody ends up reading another nation's issues. The card then has no head to
fold into, so it stays open under "Choose a nation" with the same rows; it does not ask for a
password the device already holds a token for.

**Flags are 3:2, and they fill their tile.** A flag fitted into a box leaves empty plate around
the artwork, which is what makes a flag look like a picture floating in a container rather than
an object in its own right. Every row therefore *crops to fill* `Dimens.FlagThumbnailWidth` ×
`Height` — a chip's job is recognition, and the whole flag is one tap away on the nation's own
screen, where `FlagHero` fits it uncropped. An account with no stored flag falls back to its
initials *in the same box*, so a list never changes shape depending on what the API sent.

**A flag's plate is mixed from the flag.** It used to be a fixed pale grey, chosen so that
transparent PNGs with dark artwork stayed legible — which made every flag in the app a lit slab
at night to protect a minority of them. A theme-coloured plate fixes the slab and swallows that
minority. Neither is a choice worth making globally, because the right plate is a property of
the picture, so `FlagAmbience` reads it off the picture: the flag's alpha-weighted average
colour, pushed away from the artwork's own brightness.

What the picture decides is *which question is being asked*, and there are two. An opaque flag
covers its plate, so nothing has to be read against it: the plate is only a frame, and it is
the app's own surface carrying a wash of the flag's colour. Following the surface is what keeps
it dark in a dark theme — a first attempt blended toward black or white instead, and gave
Testlandia's red flag a pale pink slab, which is the bright-box problem again in a new colour.

A flag with transparency shows its plate *through* the artwork, so there the plate has a job:
be the thing that artwork is legible against. Its average colour is the artwork's own, so
pushing away from that brightness lands the plate on the far side of it. Legibility outranks
blending in that branch — a black-on-glass flag gets a pale plate even at night, because the
alternative is a flag nobody can see.

The plate starts at the theme surface and animates to the ambience once the image decodes, so a
slow flag never flashes a colour it does not have.

Sampling is a fixed 16×16 grid, on `Dispatchers.Default`, keyed on the URL. `FlagAmbience`
itself is pure — pixels in, colour out — which is the only way it could have a test.

The request that feeds it sets `allowHardware(false)`. Coil decodes to `Bitmap.Config.HARDWARE`
from API 26, and a hardware bitmap lives in graphics memory with no CPU-readable pixels at all,
so sampling one throws on every device that has ever run this. The decode is told what it has
to produce, in the one place that samples; there is no catch around the read.

**Colour comes from the wallpaper.** Dynamic colour is on for Android 12+; the indigo scheme
is the fallback, not the intent. Anything hardcoded against the brand palette will look wrong
on most devices.

**Nothing is parsed on the main thread.** `NsClient` puts only the network call on a
background dispatcher, so a repository that decodes after it returns would decode on the
caller's thread. Every repository method therefore wraps its body in
`withContext(Dispatchers.Default)`, and BBCode is parsed there too — never inside a composable,
which runs on every recomposition.

**Issues are newspapers, and so are their consequences.** `NewspaperFrontPage` is a direct
transcription of the supplied Figma frame: every coordinate is a fraction of the component's
own width taken from the 594-wide design, so it is the same page at any size rather than a
picture that fits one screen. Four of NationStates' own strips stack to the frame's exact
aspect — `dpaper1` torn top, `dpaper2` masthead band (which runs on past the title to carry
the rules and edition line), `dpaper4` headline band, `dpaper5` body. They live in
`drawable-nodpi` because they are fixed 745px artwork, not icons; without that Android reads
them as mdpi and scales them by device density. Paper is painted behind them because they are
transparent where the sheet tears.

The masthead is dressed from the same request as the issues themselves: `capital` names the
paper, `flag` flies on it, `currency` is the cover price. Answering prints the next morning's
edition — `c=issue` returns `HEADLINES` written for exactly that.

The page has no background of its own. The strips carry their own paper *and* their own torn
edges, so anything solid behind them turns the page back into a rectangle and throws away the
tear. The body's gap is left open for the same reason — `dpaper5` is only a quarter opaque
because the site composites photographs into it, and that space belongs to artwork the API
does not supply yet.

`NewspaperStyle` holds the only thing the two supplied frames disagree on: band heights.
`FrontPage` is `Newspaper.svg`; `Stacked` is `recentHeadlines.svg`, which gives the headline
twice the room because its headlines wrap. Their horizontal geometry agrees to within a third
of a percent, so it is written once.

**`NewspaperStack` is a pile, not a list.** Positions, offsets and angles — −0.7°, +1.4°,
−2.1°, +2.8° — are the frame's own, as fractions of its 517.86-unit canvas; the sheet is 480
of those wide, which is what leaves room for each to sit somewhere different. Sheets draw
first to last so each lands on top of the one before: that ordering is the whole illusion, and
reversing it buries every headline under the paper beneath it.

The aftermath screen has no front page at its head. The headlines *are* the newspapers, so
printing one above them said the same thing twice.

**Moved statistics are the game's Recent Trends strip, not a table.** Name in the direction's
colour, unit beneath, arrow and percentage — read at a glance, detail on inspection. Eight of
them: one decision nudges thirty-odd scales and the rest are rounding errors. The site puts a
rendered badge sprite beside each scale; there are ninety with no documented address, so the
badge is a tile carrying the scale's initial.

**Only two colours in the app are not theme-derived**: trend up and trend down. They carry
meaning rather than style. Each has a light *and* a dark tone from Material 3's tonal palettes
— tone 40 clears 4.5:1 on a light surface, tone 80 on a dark one — so both stay readable on
any device in either theme. A single fixed colour is always failing one of the two. Down uses
M3's baseline error tones; up uses green at the same tones, as Material's own guidance does
for a success colour. Everything else on a trend follows the user's theme.

**A list of things is not a list of cards.** Wrapping every post on a message board in its own
outlined card gives fifty items identical weight and turns a conversation into a spreadsheet.
Posts get an avatar, a name, a time and whitespace; the chrome is removed so the writing is
what you see. A removed post is a thin italic line, not a card — the gap is the information.

**An irreversible commit sits where the choice was made.** An issue runs to five advisors, so a
button at the foot of the card is three screens away from the option it acts on. The commit
appears inside the chosen option instead: still two deliberate taps, no journey between them.
Selection is marked by a filled container *and* a border — on a wallpaper-derived palette two
container colours can land within a few percent of each other, and "which one did I pick" must
never be a question here.

**Irreversible means two aimed actions, not a modal.** Answering an issue cannot be undone, so
it takes choosing an option and then pressing the commit inside it — two separate, separately
aimed acts — and the commit says exactly what it does. A dialog repeating that same sentence
is a third tap without a third thought, which is how people learn to dismiss dialogs unread.
Dismissing an issue works identically rather than being one unguarded tap.

**A control that does nothing must not look like a control.** Empty is not failure either:
`EmptyState` exists so a board with no posts is not dressed in the red of a network outage.

**Assume the API's text is damaged, because some of it is.** Responses carry Windows-1252
punctuation mis-encoded as C1 control characters that render as tofu boxes; `NsText.repair`
runs over every response body before decoding.

**Every text field arrives as HTML source, escaped once more.** The API wraps text in `CDATA`,
so the XML layer unescapes nothing — and NationStates escapes the stored HTML on the way out.
Lazarus's factbook is literally `&amp;amp;#43457;` on the wire for a `꧁`. `HtmlEntities`
therefore decodes exactly twice: once to undo the transport, once to read the result the way a
browser reads the site. The legacy client does the same in two steps and that is what has kept
it readable. Two, never "until it stops changing" — a third pass starts eating text an author
escaped on purpose. Anything unrecognised is left exactly as written.

Two ranges are resolved rather than printed. A numeric reference into C1 — `&#149;` — is
Windows-1252 arriving the long way round and becomes the bullet the author meant. A Private Use
code point is NationStates' own icon webfont, which `[font=nationstates]` runs are full of;
Civily does not ship that font, so every one would draw as a tofu box and they are dropped
instead. No words are lost either way.

**Rich text is parsed, not pattern-matched.** `BbParser` builds a tree. It never throws, never
loses the words inside a broken tag, and never prints markup it does not implement. Anything
that renders NationStates content goes through it — and that content is not pure BBCode:
issue text carries HTML too, so `<i>`, `<strong>`, `<br>` and friends are read as the tags
they are equivalent to, and anything else is dropped. A `<` in prose stays a `<`.

**Edge-to-edge is the layout.** `enableEdgeToEdge()` runs before `super.onCreate`, and every
Scaffold consumes its insets. Content that ignores insets lands under the system bars.

## The four rules that keep it that way

**1. Dependencies point one way.** `feature` → `data` → `core`. Never back. `core` knows
nothing about a nation, a region or a telegram; `data` knows nothing about Compose. If a
file needs an import that points the wrong way, the code is in the wrong package.

**2. Failure is a value.** Repositories return `Outcome<T>`, never throw, never return null
to mean "it broke". Every `CivilyError` carries the string resource the user will see, so a
failure that reaches the UI without a message is not representable. This is spec §1.2 and §5
made structural.

**3. One way out to the network.** `NsClient` is the only code that opens a connection. That
is what makes three spec rules checkable by reading one file: the rate limit cannot be
bypassed, credentials are attached in exactly one place, and no endpoint other than
nationstates.net is reachable.

**4. Colours and sizes live in `ui/theme`.** Spec §3 bans them elsewhere. `Color(0xFF…)` or
`16.dp` under `feature/` is the violation; add a named token instead.

## The password

It exists in three places and no others: the `OutlinedTextField` on the sign-in screen, the
`signIn` parameter chain, and the `X-Password` header on one request to nationstates.net. It
is never a field on a ViewModel, never `rememberSaveable`, never persisted, never logged.
What is stored is the autologin token NationStates returns in exchange — one per nation, in
`SessionStore`'s own preferences file and nowhere else. `allowBackup` is off so those tokens
cannot leave the device either, and removing an account rewrites that file without it.

## API facts the design turns on

From the official API page, verified rather than assumed. Do not re-derive these.

- **Private shards need auth; public ones do not.** A request for public shards alone succeeds
  whatever password is sent. Sign-in therefore asks for `ping`, the shard the docs describe as
  being for "when you don't want to do anything except register a login".
- **`X-Autologin` comes back only when the password was accepted**, so its presence is a valid
  second check on sign-in and its absence is a rejection whatever the status code says.
- **`X-Pin` wins when valid.** Sending autologin and pin together is legal and self-healing:
  the pin is used until it expires, then the autologin re-establishes one.
- **A login cancels the previous session.** `X-Password` and `X-Autologin` both log in, which
  invalidates the old pin — including the user's own browser session. Two in quick succession
  return **409**.
- **Rate limit is 50 per 30s**, but the docs say not to hardcode it — `RateLimit-Limit`,
  `-Remaining` and `-Reset` are authoritative. Exceeding it returns **429** with `Retry-After`,
  and repeated offences escalate to a 15-minute-plus lockout.
- **Private commands are two-step** — `mode=prepare` yields a token, `mode=execute` spends it,
  one use per token. The only exception is `issue`. `NsClient.post` is single-shot today and
  will need this before any command feature lands.
- Telegrams carry an additional rate limit on top of the global one.

## Adding a screen

1. `data/<thing>/` — a `@Serializable` DTO and a repository returning `Outcome<Dto>`.
   Shards go in the repository's companion. Documented shards only (spec §3).
2. `feature/<thing>/` — a `ViewModel` holding `MutableStateFlow<LoadState<Dto>>` and calling
   `launchLoad`, plus a Composable wrapped in `LoadStateScaffold`. You supply only the Ready
   case; loading and failure are already handled and cannot be forgotten.
3. `nav/Routes` — a constant and a builder function.
4. A parser test with real response XML, including a missing-element case.

Do not write a per-feature `UiState` unless the screen genuinely has a fourth state (mid-vote,
mid-post). Three copies of Loading/Ready/Failed is three places to get the same `when` wrong.

## Build

```bash
./gradlew :app:testDebugUnitTest
```

```bash
./gradlew :app:assembleDebug
```

Needs an Android SDK; point `local.properties` at it or set `ANDROID_HOME`.

The daemon JVM is pinned to 21 in `gradle/gradle-daemon-jvm.properties`. The Android Gradle
Plugin rejects newer JVMs, so without the pin the build depends on whatever JDK is on PATH.
Gradle downloads a matching JDK if the machine has none.

## Known gaps

These are tracked, not hidden (spec §2 R1). Nothing here is stubbed to look like it works.

- **Rich text covers the tags players use, not every tag.** `[table]`, `[img]`, `[size]`,
  `[align]` and `[box]` are dropped rather than drawn — their contents still render. Adding one
  means a parser case, a renderer case and a test.
- **RMB posts cannot be liked.** The documented API has no like or reaction command — only
  `rmbpost`. The count is therefore a readout, deliberately styled as text rather than as a
  pill, because the pill invited a press that could never work. The legacy client liked posts
  through `/page=ajax3/…`, an undocumented HTML endpoint that spec §3 rules out; adding it is a
  decision to take knowingly, not a gap to quietly fill.
- **The newspaper does not respond to font scale.** Its type is sized from the component's
  width rather than in `sp`, because a masthead that reflowed at 200% font scale would stop
  being a masthead. Every other screen in the app scales normally. Long names shrink to fit
  rather than wrapping or clipping.
- **The front page has no second photo.** The design places a portrait at x=460; the API gives
  one image per issue, so that space is left as paper rather than filled with something
  invented.
- **A few census scales are named "Unknown".** That name comes from the legacy app's list,
  which had gaps of its own. They render honestly rather than being hidden, and ids past the
  end of the list fall back to "Scale N".
- **The result shows `DESC`, `RANKINGS` and `HEADLINES` only.** `c=issue` can also return
  `RECLASSIFICATIONS`, `NEW_POLICIES` and `REMOVED_POLICIES`; the two answers captured so far
  carried none of them, so there is no real sample to build against and nothing was guessed.
- **The RMB is read-only, and unpaged.** Fifty most recent posts, no posting, no liking, no
  older pages. Posting is a two-step Private Command.
- **No WA voting.** Also a two-step Private Command (`mode=prepare` → token → `mode=execute`),
  and `NsClient.post` is single-shot. `c=issue` is the documented exception, which is why
  issues could land first.
- **No `NsClient` or `SessionStore` test**, so the sign-in path is covered only by its pure
  parts — `AccountsTest` covers every rule about which nations are stored and which is active,
  but not the reading and writing of them. Both need a `Context`, which means Robolectric or
  extracting an interface.
- **Nothing measures §4.** No Macrobenchmark module and no Baseline Profile yet. Compose cold
  start is materially worse without a profile, so this blocks any claim of meeting the
  cold-start target.
- **The ambient plate is one colour, not a blur.** YouTube's ambient mode scales and blurs the
  image itself; `Modifier.blur` needs API 31, and minSdk is 21. A flat plate mixed from the
  flag's colours is the part of that effect which works everywhere.
- **One theme.** The legacy app ships five, and there is no in-app light/dark override.
- **The sign-in screen has not been restyled** to match the home screen's card language.
- **No skeleton loaders and no screen transitions.** Loading is a spinner; navigation uses the
  library defaults.
- **No accessibility pass.** Touch targets and `contentDescription` coverage were written
  carefully but never audited with TalkBack, and large font scales are untested.
- **Nothing measures §4** — see above. Cold start is unmeasured and Compose needs a Baseline
  Profile before any claim is made about it.
