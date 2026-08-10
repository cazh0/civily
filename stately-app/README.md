# Stately (Kotlin rewrite)

Kotlin + Jetpack Compose rebuild of the Stately Android client. Built from zero on
`rewrite/kotlin-compose`. The Java app in `../Stately/` is untouched and stays as the
reference for API behaviour until this one reaches parity.

Rules of engagement are `../STATELY-SPEC.md`. Feature surface is `../README.md`.

## Layout

```
app/src/main/kotlin/dev/cazh0/stately/
├── core/            everything with no feature knowledge
│   ├── net/         NsUrl · NsClient · RateLimiter · UserAgent · AutologinToken
│   ├── session/     Session · SessionStore          (the only copy of credentials)
│   ├── result/      Outcome · StatelyError          (the only way failure travels)
│   │                LoadState · launchLoad          (the only shape a read screen has)
│   ├── text/        NsId · Numbers · Population · RelativeTime
│   │                HtmlEntities · NsText                (decoding what the API really sends)
│   │   └── bbcode/  BbParser · BbTree · BbColor     (NationStates markup, parsed not regexed)
│   ├── AppGraph     every long-lived object, hand-wired
│   ├── StatelyApp   Application
│   └── MainActivity the app's single Activity
├── data/            NsXml + one package per resource (dto + repository together)
│   ├── auth/        AuthRepository                  (the only place a password is used)
│   ├── issues/      IssuesRepository                (the only write the app performs)
│   ├── nation/
│   ├── region/
│   ├── rmb/
│   └── wa/
├── feature/         one package per screen (ViewModel + Composable)
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
    └── component/   LoadStateScaffold · LoadStateContent · RichText · FlagHero
                     LinkRow · FactCard · NationAvatar · Pill
                     LoadingState · EmptyState · ErrorState
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

**Assume the API's text is damaged, because some of it is.** Responses carry HTML entities the
XML layer does not unescape, and Windows-1252 punctuation mis-encoded as C1 control characters
that render as tofu boxes. `NsText.repair` runs over every response body before decoding and
`HtmlEntities` runs inside the BBCode tokenizer. Both are one-pass and only ever fix; neither
can alter what an author actually wrote.

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
to mean "it broke". Every `StatelyError` carries the string resource the user will see, so a
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
What is stored is the autologin token NationStates returns in exchange. `allowBackup` is off
so that token cannot leave the device either.

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
- **One nation per install.** The legacy app stored several in a Sugar ORM table. Multi-nation
  switching needs a store; spec §3 bars new DB tables without instruction.
- **No `NsClient` test**, so the sign-in path is covered only by its pure parts. It needs a
  `Context` for `SessionStore`, which means Robolectric or extracting a session interface.
- **Nothing measures §4.** No Macrobenchmark module and no Baseline Profile yet. Compose cold
  start is materially worse without a profile, so this blocks any claim of meeting the
  cold-start target.
- **One theme.** The legacy app ships five, and there is no in-app light/dark override.
- **The sign-in screen has not been restyled** to match the home screen's card language.
- **No skeleton loaders and no screen transitions.** Loading is a spinner; navigation uses the
  library defaults.
- **No accessibility pass.** Touch targets and `contentDescription` coverage were written
  carefully but never audited with TalkBack, and large font scales are untested.
- **Nothing measures §4** — see above. Cold start is unmeasured and Compose needs a Baseline
  Profile before any claim is made about it.
