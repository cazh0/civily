# Civily

A NationStates client for Android, in Kotlin and Jetpack Compose, built from zero on
`rewrite/kotlin-compose`. It began as a rewrite of Stately and shares no code with it; the Java
app in `../Stately/` is untouched and stays as the reference for API behaviour until this one
reaches parity.

Rules of engagement are `../CIVILY-SPEC.md`. Feature surface is `../README.md`.

## Layout

Two Gradle modules. `:app` is the app; `:benchmark` is a `com.android.test` module that ships
nothing, drives `:app` from outside its own process, and is described under "Measuring it".

```
app/src/main/kotlin/dev/cazh0/civily/
├── core/            everything with no feature knowledge
│   ├── net/         NsUrl · NsClient · RateLimiter · UserAgent · AutologinToken
│   ├── session/     Session · Accounts · SessionStore (the only copy of credentials)
│   ├── result/      Outcome · CivilyError          (the only way failure travels)
│   │                LoadState · launchLoad          (the only shape a read screen has)
│   ├── text/        NsId · Numbers · Population · Magnitude · Percent
│   │                RelativeTime · Countdown            (a gap behind, a gap ahead)
│   │                FreedomRating                  (which ratings are good news)
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
    ├── theme/       Color · ChartColors · Dimens · Motion · Theme
    │                                                (the only colours, sizes and durations)
    │                FlagAmbience                    (a plate mixed from a flag)
    └── component/   LoadStateScaffold · LoadStateContent · RichText
                     AmbientFlag · FlagHero · LinkRow · FactCard · NationAvatar
                     Pill · SectionHeader · LoadingState · EmptyState · ErrorState
                     Countdown                       (the only thing that ticks)
```

`data/` holds two kinds of type. A `…Dto` is the wire shape, annotated for XML. A plain type
beside it — `Region`, `Assembly`, `Nation` — is the screen shape, and the repository maps between
them. The mapper exists where there is real work to do: decoding entities, parsing BBCode, and
turning the API's `"0"` sentinel into a null. Where there is no such work the DTO goes to the
screen directly rather than through a mapper that only copies fields — which is why sign-in and
nation search still read `NationDto`, and only the nation screen gets a `Nation`.

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

**The Issues button carries its answer.** A row labelled "Issues" and nothing else asks the
reader to open a screen to find out whether it was worth opening — every time, including the
several times a day there is nothing there. So it says: a count when decisions are waiting, and
when none are, how long until the next one. Either way the press is a decision already made
rather than a check that has to be performed.

The count is the badge everyone already reads, from every notification on the device. Zero
draws *nothing* — not a grey nought, not an empty ring. A badge is a claim that something needs
dealing with, and one that persists at zero makes that claim every time the reader looks at the
screen, which is how a person learns to stop seeing it. It is `primary`, not the `error` colour
Material's own `Badge` defaults to: red in this app means something is wrong, and an issue
waiting is the thing the player opened the app for. Colouring good news as damage is the mistake
the freedom ratings already caught the legacy client making.

**It stands in the chevron's place, and the row has nothing else on it.** The badge and the
arrow occupy one slot and cross over inside it, so no gap can open between two marks that were
never both there, and nothing in the row moves as the count comes and goes. A count already says
there is somewhere to go; an arrow beside it is the same sentence twice, at the one end of the
row where a reader is looking for a single answer. There is no leading icon either. The rows
around this one lead with a flag because a flag is the thing being identified — a glyph invented
to fill that space would only be decoration wearing a flag's clothes.

It costs a request of its own, `unread`+`nextissuetime`, rather than being read off the issues
themselves. That is a count and an instant against every issue's prose and five options each,
for a row on the front screen that is reloaded every time the reader lands on it — including
the moment they come back from answering something, which is the one moment the old number is
guaranteed wrong. Both shards are documented and the legacy client reads `unread` for exactly
this. Nothing is shown while the answer is unknown, and a row still holding the previous
nation's numbers a moment after a switch shows nothing either: the count is stamped with the
nation it belongs to, because switching costs no request and would otherwise leave one nation's
issues sitting under another nation's name. A count that *failed* to load says so in the row's
own grey — spec §5 has no silent failures in it, and a digit that quietly stopped updating when
the device left the network is exactly that.

**The wait is the empty state, and it runs.** A nation between issues is not an error and not a
dead end, and the game says precisely when the next one lands, so the screen says it too: the
nation is gloriously issue-free, and under that a clock. It counts in seconds because this is
the one screen with nothing else on it and a countdown that moves once a minute reads as a
screenshot; the button on the front screen counts in minutes for the opposite reason. The
figures are tabular — Roboto's proportional digits are different widths, so a plain `Text`
counting down re-measures itself on nearly every tick and nudges the block around it — and the
hours field is written even at zero, so crossing an hour does not move the digits under the
reader's eye.

Both clocks stop dead while the screen is off. A composition survives the app going to the
background and a `delay` loop inside one goes on waking the process to write a value nobody can
see, so `rememberCountdown` reads the lifecycle as state and simply does not exist while the
screen is stopped. It wakes on the boundary at which the *displayed* value changes rather than
every second from whenever the composition happened to start, which is one state write per
visible change and no repeated or skipped digits.

And the wait ends by itself. The screen counting down is the screen that notices, so the moment
the clock runs out the list reloads under the reader — no pull, no stale "issue-free" over a
nation that has three. It cannot become a poll: the effect is keyed on the instant being waited
for, so a reload handing back the same instant does not restart it, and a later one is a new
wait that restarts it exactly once.

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
paper, `flag` flies on it, `currency` is the cover price. Answering prefers the site's own
tokenised enact form, whose response carries the talking point, trend changes, exact headlines
and `legislation-papers` cutout image URLs printed on the newspaper stack. If fetching or
parsing that form fails before anything irreversible is submitted, Civily falls back to the
documented `c=issue` command rather than breaking the issue feature; once the site form has
been posted, any missing result is reported as unknown and the list refreshes.

**In practice that preference never takes.** `/page=show_dilemma/` answers 403 with the site's
Border Patrol page — "Enable JavaScript and cookies to continue" — which is a bot check, not a
session problem: an anonymous request from a desktop gets the same page. Every enactment
therefore spends one request finding that out and then goes through `c=issue`. Nothing in the
app tries to satisfy that check, and nothing should: the cutouts are worth a lot less than
being a client that walks past a wall the game put up on purpose. See the gap list.

The page has no background of its own. The strips carry their own paper *and* their own torn
edges, so anything solid behind them turns the page back into a rectangle and throws away the
tear. The body's gap is left open for the same reason — `dpaper5` is only a quarter opaque
because the site composites photographs into it. The photographs go *under* the strip, through
the two windows cut out of it, so the newsprint's own torn edges frame them.

Those windows are measured off `dpaper5` itself — 37..450 and 578..684 of its 745, 2..119 of
its 140 — and not off the Figma frames, because the strip is stretched to whatever height the
style's body band is and only the strip knows where it is missing. So a photo's x and width
follow the page width while its y and height follow the band; sized from the width alone they
register with the windows on the front page and miss them by a fifth in the stack.

Each window is floored with newsprint before the photograph goes in, in the tone sampled from
the strip's paper along that window's own edge. A window is a hole: without the floor an empty
one shows the screen through the page, which reads as damage rather than as a front page whose
photograph has not arrived — or, on the aftermath, was never offered.

What goes in them is `PIC1` and `PIC2` from the issue shard, which are artwork ids that address
`/images/newspaper/{id}-1.jpg` and `-2.jpg`. The aftermath page is different: it prints the
whole path, the names are its own — `i16-1.jpg` on one paper, `y108-1.jpg` on the next, and
different again on the next issue — so every `dpaperpic` `src` is carried through exactly as
written and resolved against nationstates.net. Rebuilding one from a stem is how a working link
becomes a 404 the first time the game picks a name that does not fit the pattern. A paper is
read by document order, not by `dpaperpic1`/`dpaperpic2`, and one that prints no photograph
still prints its headline.

`NewspaperStyle` says where a page is being printed and nothing else: `FrontPage` is alone on
the screen, `Stacked` is one sheet in a pile. What the two supplied frames of a design disagree
on is vertical proportion, and the body band is the one part of that every banded design agrees
on — a pile is drawn on deeper paper, 128 of its 480 units against 111.61 of 594 — so the body
hangs on the style and the two bands above it hang on the design. Their horizontal geometry
agrees to within a third of a percent, so it is written once.

The broadsheet is the one design whose bands move: stacked, it gives the headline twice the
room, because the aftermath's headlines are sentences and they wrap. Both stack frames happen
to be drawn with a headline that fits on one line, and holding the app to that would mean
shrinking every longer one until it stopped being a headline.

**The game prints three papers, so the app does.** `newNewspaper.svg` is a second front page on
the same three strips and it is a different newspaper: the masthead sits on a slate plate
between two flags rather than beside one, the cover price is a tilted flash pinned across the
tear, the edition line runs cream out of a slate bar under a heavy red rule, and the body is
given over to one photograph with the headline shouted across it in outlined capitals. It has
no headline band at all, which is why `NewspaperTabloid` shares no band table with the other
two: there is no such band on it to give a height to.
The photographs go *over* the strip here rather than through it: the picture is wider than the
windows cut in `dpaper5` and covers the paper between them, which is only possible from on top.
It is printed whether or not the issue offered a photograph, because it covers both windows and
leaving it out would open two holes; the inset beside it is printed only when there is a second
picture, an empty frame with a shadow under it being no part of a page.

`npVariant2.svg` is the third, and `NewspaperBerliner` is named for it the way the other two
are: the three papers carry the names of the three formats a real one is printed in, and this
is the middle of them. It is the broadsheet's four strips and the broadsheet's bands, arranged
as a quieter paper — the cover price takes the left of the masthead where the broadsheet flies
its flag and the flag takes the right where the broadsheet prints its price, the paper's name
sits in the middle of that rather than over toward one end, and the edition line is ruled off
top and bottom by three hairlines instead of hanging under one heavy rule. Its edition line
also runs the other way: volume, city, date. The headline is centred and italic, and every
mark on the page is one slate ink.

Its cells are the thirds of the ruled width, not the frame's own 216.7/77.45/216.7 boxes. That
middle figure hugs one date set in the frame's own face, and a hug is a measurement rather than
a margin — the tabloid's masthead is off its frame's 219.88 for the same reason. Ranged out,
centred and ranged in over equal thirds, the frame's own strings land exactly where the frame
puts them and a longer one still has somewhere to go.

Which paper an issue prints on is `Newspaper.design`, alongside the masthead and the edition
line and for the same reason: it has to be the same page in the list, in the detail, and
tomorrow. Three designs over consecutive ids, against the edition city's four-cycle, come back
around together only every twelfth issue, so every design prints under every city. Two did not:
three and four share no factor, two and four share one, which had the broadsheet taking the
first and third cities for good and the tabloid the other two. `NewspaperForIssue` is the one
door — every screen showing a single issue goes through it, and so does every sheet of the
aftermath pile.

Its type is stated as cap heights rather than point sizes. The frame is set in a bold condensed
grotesque the app does not ship and will not — a display font is tens of kilobytes against a §4
row that says the APK may not grow — so the page is set in the device's `sans-serif-condensed`,
and two faces at one point size do not put capitals at one height. Carrying the height of the
letters across is what keeps the masthead the size the frame drew it. For the same reason the
masthead's fitting box is the plate less the frame's own 4.61-unit inset and not the frame's
219.88-unit text box: that box hugs one string in the frame's own face, and a hug is a
measurement rather than a margin.

The headline is drawn three times at one measurement: a solid silhouette offset down and right,
the one-unit ring around the letters, then the cream face. That is what the frame's filter is —
eight offset copies of the *filled* glyph, seven of them a unit apart to make the ring and one
3.072 units down to make the drop. Hanging the drop off the ring instead, as one shadow on one
stroked pass, prints the ring a second time a few units below and reads as a smear rather than
as type standing above the page. The ring and the drop both scale with however far the string
had to shrink.

**In a pile the tabloid does not shrink at all.** Alone on a screen it has one sheet and one
depth of paper, so a headline that will not fit has to be fitted to it. `newspaperStack.svg`
answers the same problem the other way: both of its tabloids set the headline at the frame's own
cap height and line box — the same size the front page starts from, to five decimal places — and
the one carrying the longer headline is simply printed on deeper paper. So that is what the pile
does. `Normal` is the sheet the frame prints two lines on, `Extended` the sheet it prints three
on; neither line count is typed in, both fall out of how many line boxes fit between the top of
the headline and the foot of the sheet, which is two and six. A headline that will not go in two
gets `Extended`, and one that will not go in six is ellipsised — cut where the reader can see it
was cut.

Only two figures separate those sheets from the front page: the headline is given four fifths of
the sheet rather than two thirds, and the picture is stated as its share of the body band rather
than as a height, which is what lets the band get twice as deep and stay a photograph. The
masthead band is the one figure not carried across — the pile draws it 1.5% deeper with all its
furniture ruled down to sit flush at the foot, and taking the depth without the furniture would
open a strip of bare paper the frame does not have.

**`NewspaperStack` is a pile, not a list.** Offsets and angles — +0.7°, −1.4°, +2.1°, −2.8° —
are `newspaperStack.svg`'s own, as fractions of its 518.45-unit canvas; the sheet is 480 of
those wide, which is what leaves room for each to sit somewhere different, and a sheet turns
about its top left corner because that is the corner the frame turns it about. Sheets draw
first to last so each lands on top of the one before: that ordering is the whole illusion, and
reversing it buries every headline under the paper beneath it. Past the frame's four, the four
repeat — the API has never returned more than four headlines, and a repeat is the one
continuation that cannot walk a fifth sheet off the side of the canvas the way a growing angle
would.

The pile prints the game's papers in the game's own order, continuing from the paper the issue
was itself printed on, so the top sheet is the front page the reader has just come from and the
rest are the papers that would follow it. That is `Newspaper.design` applied one step at a
time; a pile that picked its papers by some other rule would need a second rule to explain.

**Where each sheet starts is the one thing not transcribed.** The frame lays its four out by
hand and its own spacing lands the next sheet across the foot of the one above — the last third
of the broadsheet's headline at one end of the pile, a whole line of the tabloid's at the
other. A pile whose entire point is that every sheet shows its words cannot be laid out by
numbers that cover them, so each sheet is set down where the sheet above stops printing.

Straight lines decide that and nothing else: the foot of each line of the headline above, and
the torn top edge below. Both sheets are turned, so every one of those is sloped, and a sloped
line clears a sloped line all the way along a span if it clears it at the two ends — so each
line's own two bottom corners are asked, and nothing else is.

Each *line*, not the paragraph. A paragraph's box is as wide as the measure it was given, so
clearing it means clearing a corner of blank paper past the end of the shortest line: a headline
that breaks to a short last line — `GOVERNMENT CUTS` over `ECO-WASTE` — was holding the next
paper a finger below where it could sit, with the seam between two torn edges showing through.
Asking the lines gets that back.

The lines are also the ones the words came out on, not the ones they were offered. The tabloid
keeps room for two at the front and up to six in a pile, and the game's headlines regularly take
fewer; spaced by the room rather than the ink, the sheet below started under the whole
photograph. So `rememberFittedType` hands back the boxes it measured along with the size it
settled on, and the page and the pile read the same figures — the page to print them, the pile
to know where they end.

Two things still stood between a line's last ink and its box, and both were the tabloid's.

A line box keeps room for descenders, which a page shouting in capitals has no use for, so that
page hands the pile its baselines rather than its feet. The banded designs keep their feet:
their headlines are sentences, and sentences have descenders.

And its first line is a fifth of a line taller than the rest. Asked for a line shorter than the
face's own — the frame sets this headline on 50 units where the condensed face wants 56 —
Compose gives the first line the face's and every line after it the 50. That deficit cannot be
trimmed, because `LineHeightStyle.Trim` removes space something added and nothing added this;
so the headline is drawn up by the difference, measured off its own first line. It was landing
0.046 of the sheet's width below the body band where the frame draws it at 0.028; it now lands
at 0.026, and the pile gets the same distance back.

Together they lift the sheet under a tabloid by 35px on a 1008px canvas.

Everything a design prints above those lines — masthead, flags, price, edition line — is both
higher and no wider, so clearing them clears those too, and only the headline is stated.
The pile then runs to the foot of its lowest sheet, turned corner included, rather than to the
frame's own end: the frame stops short of that and cuts the bottom paper through its
photographs, and those are the only photographs the pile ever shows.

The aftermath screen has no front page at its head. The headlines *are* the newspapers, so
printing one above them said the same thing twice.

**A nation has seven subjects, so it has seven tabs.** Overview, Policies, People, Government,
Economy, Rankings, Happenings — the set and the order of the game's own nation page, which is
what the legacy client mirrors too. A reader who knows NationStates knows where to look before
they have looked. Splitting them is what lets each subject be answered properly: the government's
paragraph next to the government's budget, rather than forty facts and four paragraphs in one
column that nobody reaches the end of.

The bar is pinned under the top app bar rather than scrolling with the content, because it is the
only way between subjects and a reader four screens into the Rankings has to be able to leave
without scrolling back up. Each pane owns its own scroll: two of them are real lists — ninety
census scales, twenty policy banners — and those have to be lazy, which one outer scroll would
make impossible, and a shared scroll position across seven unrelated pages is wrong anyway. The
selected tab is held above the load state, so a pull-to-refresh, a rotation or a failed retry all
come back to the tab the reader was on.

**A nation's facts are a grid, not a stack of cards.** Short parallel values in full-width cards
is one screen of scrolling per four words, with the eye crossing the whole display for each. Side
by side they are one object taken in at a glance. `FactGrid` packs them by hand rather than with
`LazyVerticalGrid`, because it sits in a scrolling column and a lazy grid nested in a scroll of
the same axis has no bounded height; nothing is gained by laziness when a nation has a fixed dozen
facts. Each row is measured to its tallest tile so a wrapped value does not step the row, an
absent shard drops its tile before packing so the grid closes up rather than showing a hole, and a
fact that runs long — the population, a player's own national animal — takes a row to itself.

Grids are titled and grouped rather than run together: they are separate questions, and a header
over each turns a wall into an index. Freedoms sit three across because they are one comparison,
and wrapping the third onto its own line would invite reading it as a different kind of fact. The
region tile is the only door in the grid, so it is the only tile with a caret and the link colour
— a control that does nothing must not look like a control.

**The prose is the game's, not ours.** `GOVTDESC`, `INDUSTRYDESC` and `CRIME` are finished
paragraphs and are printed as they arrive — each at the head of the tab it is about, which is
what makes the Government tab's chart legible without a caption. Three sentences are composed,
from the shards the site itself composes them from: what the nation is admired and remarkable for
(`ADMIRABLE`, `NOTABLE`), the temperament of its people (`SENSIBILITIES`, `DEMONYM2PLURAL`,
`POPULATION`), and its animal and religion (`ANIMAL`, `ANIMALTRAIT`, `RELIGION`). Each appears
only when every shard it needs came back, because half a sentence is worse than none. The wording
changes between refreshes and that is the game's own behaviour: `NOTABLE` and `ADMIRABLE` return
a rotating pick from the full `notables`/`admirables` lists.

Everything in it goes through `BbParser` even though the game writes it: that is the rule for
anything rendering NationStates content, and it is what guarantees no reader sees a tag the day
the game starts dressing these fields.

**Shares of a whole are the game's own ring.** Three of them appear on this screen — where the
government spends, what the economy is made of, what people die of — and all three are drawn the
way the desktop site draws them, which the legacy client also mirrors: a ring with a hole six
tenths of the radius, a fainter band of the same slices just inside it, no labels on the wedges,
and a wrapped legend of colour dots underneath. Those proportions are the legacy client's own
(`holeRadius = 60`, `transparentCircleRadius = 65`), and matching them is what makes a player who
knows the game recognise this screen. `DonutChart` draws it on a `Canvas` — three arcs and two
circles — because a charting library for that would cost every build and buy nothing (spec §3).

Slices are **not** sorted by size, and that is the point: the wedges carry no labels, so a slice
is identified by its colour alone, and its colour comes from its position in `Department` or
`Sector.Kind`. Education is the same blue-violet on every nation's chart. Sorting would recolour
the chart per nation and make the legend the only way to read it. A department funded at zero is
dropped rather than drawn — eleven wedges of nothing are eleven colours in a legend for no reason.

One departure from the game, and it adds rather than changes: the legend carries each slice's
percentage. The site and the legacy client hide those behind tapping a wedge, which leaves a chart
whose numbers cannot be read at all — and the numbers are why the chart is here.

The API reports every mauling as `Animal Attack` whatever the nation's animal is, so the one cause
of death a player chose for themselves is the one the chart would not name. It is relabelled with
the animal, as the site does. The animal is the player's own words, so it arrives lower-case and
stays that way.

The chart carries no total in the nation's own money. The API publishes the *split* of the budget,
not its size; the legacy client multiplies GDP by the government's share of the economy and
labels the result "Total", which is a different quantity wearing the budget's name. That share is
a real figure and it is on the Economy tab, where it belongs.

**Rankings stay in the game's scale order.** Ninety rows, not sorted by rank: the reader is
usually looking for one particular scale, and a list that reorders itself per nation is a list you
have to read rather than scan. The percentile is the pill because "Top 3%" means something without
knowing how many nations exist. Units sit under the *name* rather than under the score — the
game's units are jokes at full length, "Krugman-Greenspan Business Outlook Index", and the left
column is the only one with room for one.

**Happenings are a chronology, not a list of cards.** A dot, a timestamp in words, and the line
itself. `@@nation@@` and `%%region%%` are the feed's own delimiters, so `BbParser.parseHappening`
reads them as the `[nation]` and `[region]` tags they mean — one parser, one renderer, no markup
reaching a reader. They are recognised only in that mode, because a factbook is allowed to
contain `%%` and turning an author's per-cent signs into a region link loses half their sentence.

The nation's own name appears in nearly every line of its own feed and is deliberately *not* a
link there: a link that pushes the screen the reader is already on is a back press they now owe.
Time reads as "3 hours ago" rather than the board's "3h" — a happening is a sentence, and a
compact token beside one reads as debris. `RelativeTime` hands back a count and a unit rather
than words, because the plural belongs in the resource layer and this object may not hold a
`Context`.

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

Those same two carry *every* good/bad judgement, a moved statistic and a freedom rating alike,
so green means one thing everywhere; a second pair for a second feature would be a second
vocabulary to learn. A tile that carries one is washed with its accent at M3's 12% state-layer
opacity, composited onto the surface it would otherwise have had rather than drawn translucent
— that keeps it opaque, so a judged tile does not read a different lightness from the plain
tile beside it, and it adds no colour value to keep in step with the palette.

**A freedom rating is coloured by its word, never by its score, and the legacy app has this
wrong.** Civil Rights, Economy and Political Freedom are census scales whose scores rise
monotonically — but the words do not. Swept across the full rank range of all three against the
live API: the top of every ladder turns against the nation. A maximum civil-rights score is
"Frightening", a maximum political-freedom score is "Corrupted", and a runaway economy is
"Frightening" too, with "Excessive" and "Widely Abused" on the rungs below them. Stately colours
these cards on a red-to-green ramp indexed by `score / 7`, which paints every one of those bright
green — the worst outcomes in the game shown as the best possible news. `FreedomRating` reads the
word instead, which is why Civily asks for no score here at all.

Its table is only what that sweep actually returned. A rung it does not recognise is
`Middling` and takes no colour, so NationStates adding one costs a tile its tint and can never
tint one the wrong way — the same discipline `HtmlEntities` uses for an entity it does not know.

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

**A screen on its way out cannot navigate.** A destination stays composed for the whole of its
exit transition and goes on accepting taps, so a tap that lands a frame after the user pressed
back is delivered by a screen that is no longer on the back stack. Acting on it navigates *from*
a popped entry: back on the issue list followed by a tap on a headline pushed an issue detail
with no list beneath it, and the shared `IssuesViewModel` — looked up through the list's entry —
took the app down with it. Every callback in `CivilyNavHost` therefore goes through `NavActions`,
which navigates only while its entry is still the current one — which is what "the screen the
user is on" means. The same guard is what stops a double-tap pushing two copies of a screen.

Lifecycle state is the tempting test — AndroidX's own samples use `RESUMED` — and it stops this
crash as well. It is not the one here because it answers a slower question: an entry is not
`RESUMED` until its *arrival* transition has finished, so permission to navigate is held back by
however long the animation happens to be, and the app grows a dead window the day it gets real
screen transitions. Being the current entry is true the instant the destination exists.

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

**Judge the feel of the app on a release build, not a debug one.** Measured on a physical device,
scrolling and switching every tab of the nation screen:

| | cold start | worst frame (99th) | missed vsyncs | slow UI-thread frames |
|---|---|---|---|---|
| `assembleDebug` | ~700ms | 38ms | 4 | 6 |
| `assembleRelease` | ~190ms | 11ms | 0 | 0 |

Same code. The debug build has no R8, runs its Compose lambdas uninlined, and gets no AOT pass,
so it drops frames the shipped app does not — the release build never misses a vsync and its worst
frame is inside the 16.7ms budget. A slow debug build is not a performance defect to chase; it is
the reason release timings are the only ones worth quoting.

## Measuring it

`:benchmark` is a `com.android.test` module that drives the app from outside its own process, the
way the system does. It ships nothing. It exists to answer spec §4 with numbers instead of
opinions, and to record the Baseline Profile the app carries.

```bash
./gradlew :app:generateReleaseBaselineProfile
```

```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest
```

The first walks `CivilyJourney` — search a nation, open it, read all seven tabs, come back — and
writes what ran to `app/src/release/generated/baselineProfiles/`. That output is **checked in**: it
is a build input, and a build whose result depends on a phone being plugged in is not a build.
Regenerate it when the journey changes, not on every build.

The second measures. `StartupBenchmark` runs cold start twice over, once with the profile and once
with no ahead-of-time compilation at all, because one number on its own says nothing about what the
profile is worth.

Measured on a Pixel 8 Pro, `benchmarkRelease` (R8 on, profile installed), ten iterations each:

| | cold start, median | range |
|---|---|---|
| with the Baseline Profile | **283ms** | 265–316ms |
| no AOT compilation at all | 297ms | 278–321ms |

Both are well inside §4's 1.5s. The profile is worth about 5% here and that is the honest figure
for *this* device: a Pixel 8 Pro JITs its way out of most of the problem. The profile matters in
proportion to how slow the CPU is, which is exactly the device this app is not developed on.

Frames, measured with the platform's own counters over an identical scripted gesture set — twelve
flings of the Overview and eight of the ninety-row Rankings list, about 1,170 frames each, both
builds minified:

| | 50th | 90th | 95th | 99th | janky frames |
|---|---|---|---|---|---|
| before this pass | 5ms | 9ms | 14ms | 17ms | 6.3% |
| after | 5ms | 9ms | **9ms** | 16ms | **3.0%** |

The tail is the whole story: the median frame was never the problem, and the 95th percentile
halving from 14ms to 9ms is the difference between a scroll that hitches and one that does not.
At 120Hz the budget is 8.3ms, which the 90th percentile is still sitting on — so this is better,
not finished.

## What keeps it fast

Spec §4 sets the numbers. These are the rules that hold them, and each of them is a rule because
breaking it is invisible in review and obvious on a phone.

**Anything that scrolls is a lazy list.** Not just the long ones. The top app bar collapses as
the reader scrolls, so the content below it is measured against a new height on every frame of
the gesture — with an eager column that is the whole page re-measured per frame, and with a lazy
one it is the handful of items on screen. Laziness is also what stops a tab costing the part of
itself nobody scrolled to: the nation's seven panes, the region screen, the World Assembly
chamber and every feed are items, and prose goes through `richTextItems` so a factbook is a run
of paragraphs rather than one indivisible block.

**A block carries the gap under itself.** `SectionGap`, not the list's `verticalArrangement`. A
section with nothing to show draws nothing, and a lazy list spaces a zero-height item exactly as
it spaces a full one — an arrangement would leave a section's worth of blank page wherever the
API sent no data.

**Nothing is derived twice.** An annotated string is remembered against the block it came from,
the ninety census names are read once for the list rather than once per row, an id becomes a
name once per screen rather than once per recomposition, and `NumberFormat` is built once per
thread rather than once per number. None of this is micro-optimisation: each was work being done
per frame for an answer that had not changed.

**Intrinsic measurement is asked for, never stumbled into.** It measures a subtree an extra time,
so it is used only where a row genuinely has to know its tallest child — `FactGrid`. A quote's
stripe used to force one over arbitrarily deep nested quotes to be told a height it could have
been drawn at instead; it is drawn now. The two-pass custom layout that would replace intrinsics
outright is not available: Compose throws if a child is measured twice in one pass, and its own
error message names intrinsics as the supported answer.

**A transition is over before it is noticed.** navigation-compose fades for 700ms in each
direction by default, which is most of a second of watching a screen that finished composing
before the animation started. `Motion` holds the replacements — 110ms in, 80ms out — and every
duration in the app lives there for the same reason colours live in `Color` and sizes in
`Dimens`.

**The image caches are sized for flags, not for a photo library.** Coil's defaults are a quarter
of the heap and up to 250MB of disk; this app holds a hero flag and a screenful of thumbnails, so
it takes a tenth of the heap and 16MB of disk, and half that memory on a device that reports
itself low on RAM. Cache headers are deliberately ignored — they expire flags in hours, and a
flag changes when a player redraws one. Where Coil can halve the bytes per pixel it is allowed to
on low-RAM devices only; it applies that to JPEGs alone, which have no transparency to lose.

**Nothing reads a file on the main thread.** `SharedPreferences` opens and parses an XML document
on first touch, and the first thing to ask for the session store is the composition of the first
screen — so `CivilyApp` starts that read on an IO dispatcher as the process comes up. `by lazy`
is synchronised, so nothing can see a half-built store and nothing reads the file twice.

**Nothing ticks that is not being looked at.** The two countdowns are the only clocks in the
app, and both are gated on the lifecycle and wake on the boundary where their own display
changes — not on a fixed interval, and not at all while the screen is off. See "The wait is the
empty state" above for why each of those is a rule rather than a nicety.

**A decoded bitmap does not outlive the colour taken from it.** `AmbientFlag` needs one average
colour out of the flag; holding the bitmap in state to get it pinned a full-size image in the
heap for as long as the screen existed, on top of the copy Coil's own cache already holds.

**The app ships a Baseline Profile, and it is recorded rather than written.** Android compiles the
methods it names ahead of time at install, so the code that draws the first screen is native before
it is reached instead of being interpreted while the user watches — which for a Compose app is
mostly the Compose runtime itself. `:benchmark` records it by walking the app; `profileinstaller`
puts it into ART's store on the versions of Android where the platform does not. A profile is only
worth its accuracy, so nothing about it is hand-written: a guessed list of hot classes is a guess
shipped as fact, and it would spend install-time compilation on code nobody runs.

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
- **The aftermath newspapers carry no photographs.** The `legislation-papers` block names one
  cutout per window, per paper, with names that change every issue — the parser reads them and
  the stack has the windows to print them in. It never gets the chance: `/page=show_dilemma/`
  is behind NationStates' Border Patrol check, which wants JavaScript and cookies and answers
  403 to anything else, so answering falls through to `c=issue` — and that command's
  `<HEADLINE>` is bare text, no attributes, checked against a live enactment rather than
  assumed. `<ISSUE>` in the issues shard does carry `PIC1`/`PIC2`, which is why the front page
  has photographs and the aftermath does not. Getting past a bot check is not a gap to fill; it is a thing this client
  declines to do. The windows print as blank newsprint, which is why the parser and the
  geometry are kept working and tested against a captured page rather than deleted — the day
  the game offers the artwork through the API, one mapper is the whole job.
- **The newspaper does not respond to font scale.** Its type is sized from the component's
  width rather than in `sp`, because a masthead that reflowed at 200% font scale would stop
  being a masthead. Every other screen in the app scales normally. Long names shrink to fit
  rather than wrapping or clipping.
- **The summary has no size adjective.** The site opens with "is a massive, efficient nation";
  Civily opens with "is efficient and remarkable for…". `efficient` is the `admirable` shard,
  but no public shard carries the population-size word — every one in the API's list was requested
  against a live nation to check, `nstats` and `legislation` included. Inventing a ladder of
  population thresholds would be a guess printed as fact, so the adjective is absent instead.
- **The nation grid is phone-first.** Column counts are fixed at two, and three for freedoms.
  There is no tablet or landscape breakpoint, so a wide screen gets wide tiles rather than more
  of them.
- **The nation screen shows no World Assembly badges and no resolution votes.** `wabadges`,
  `gavote` and `scvote` are documented shards and the legacy client's Overview card carries all
  three, but every nation sampled returned `<WABADGES></WABADGES>` and empty votes, so there is no
  real response to build a parser test against and nothing was guessed at.
- **Rankings are a readout, not a door.** Each row shows the score, the world rank and the
  percentile; there is no census history or trend screen behind it. The legacy client opens a
  chart, which needs `mode=history` and a plotting surface — neither exists here yet.
- **Nothing on this screen knows about Z-Day.** The `zombie` shard is live for one day a year and
  the legacy client grows a whole card stack for it. Out of season there is no response to build
  against.
- **A few census scales are named "Unknown".** That name comes from the legacy app's list,
  which had gaps of its own. They render honestly rather than being hidden, and ids past the
  end of the list fall back to "Scale N".
- **The result still skips policy and reclassification changes.** `c=issue` can return
  `RECLASSIFICATIONS`, `NEW_POLICIES` and `REMOVED_POLICIES`; the captured answers carried none
  of them, so there is no real sample to build against and nothing was guessed.
- **The RMB is read-only, and unpaged.** Fifty most recent posts, no posting, no liking, no
  older pages. Posting is a two-step Private Command.
- **No WA voting.** Also a two-step Private Command (`mode=prepare` → token → `mode=execute`),
  and `NsClient.post` is single-shot. `c=issue` is the documented exception, which is why
  issues could land first.
- **No `NsClient` or `SessionStore` test**, so the sign-in path is covered only by its pure
  parts — `AccountsTest` covers every rule about which nations are stored and which is active,
  but not the reading and writing of them. Both need a `Context`, which means Robolectric or
  extracting an interface.
- **Frame durations come back from the benchmark as a frame *count* and nothing else.** On the
  device this was written against, `FrameTimingMetric` reports `frameCount` but neither
  `frameDurationCpuMs` nor `frameOverrunMs` — the same class of gap as the launch-detection one
  that forced `androidx.benchmark` up to 1.4.1, and 1.5.0-beta01 does not build here at all
  (`Could not stat file .../app/provider(?)`). Until a version parses this platform's traces, the
  frame figures in the table above come from `dumpsys gfxinfo`, which is the platform's own
  counter rather than a per-frame breakdown. `NationScrollBenchmark` still runs the journey and
  still catches a regression that changes how many frames it takes.
- **Nothing runs any of this automatically.** Both the profile and the measurements need a phone
  on the end of a cable and a person to type the command. There is no CI, so nothing fails when a
  change makes the app slower — the numbers in this file are a record, not a guard.
- **The APK grew by 120KB** to carry the profile machinery: ~110KB of `profileinstaller` and ~10KB
  of profile. That is a real cost against spec §4's "no larger than previous version", taken
  knowingly for the cold-start figures above.
- **The ambient plate is one colour, not a blur.** YouTube's ambient mode scales and blurs the
  image itself; `Modifier.blur` needs API 31, and minSdk is 21. A flat plate mixed from the
  flag's colours is the part of that effect which works everywhere.
- **One theme.** The legacy app ships five, and there is no in-app light/dark override.
- **The sign-in screen has not been restyled** to match the home screen's card language.
- **No skeleton loaders.** Loading is a spinner. Screen transitions are a short cross-fade and
  nothing more — no shared element, no direction, no depth.
- **No accessibility pass.** Touch targets and `contentDescription` coverage were written
  carefully but never audited with TalkBack, and large font scales are untested.
- **Cold start and frames are measured; the other three §4 rows are not.** Nothing checks
  transition time, main-thread block length, or APK size, and see the CI gap above for why none
  of it is enforced.
