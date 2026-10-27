# My List — Team 48

An accessible shared shopping list for older adults with mild cognitive impairment (MCI).
University of Canberra capstone, Team 48. Native Android: Kotlin and Jetpack Compose.

It follows the final wireframes and the client's review of them: one rolling list, pictures
first, voice and photo input that always ask before adding anything, optional suggestions,
reminders, and WhatsApp-style QR pairing so a family member's phone shows the same list in
real time.

## What changed after the client's review

| Feedback | In the app now |
|---|---|
| Sign up: can the number come off the phone? | **My number** offers the phone's own number in one tap (Google Phone Number Hint, no permission). The number is optional, so nobody is stuck for not knowing it. |
| Sign up: say both name and number? Press Stop? | **Say it** is a short conversation: *Say your name* → "Mary — is that right?" → *Say your phone number* → confirm → done. Listening ends by itself when the person stops talking. |
| Empty list sounded like an error | It now says **Start a new list**. |
| Suggestions: what happens to "Not now"? | One tap on a picture adds it (tap again to take it off). One button: **Done**. Heading: *Things you often buy*, which is where they come from: things ticked off at least twice in 12 weeks. |
| Adding several items at once | Add items shows no pop-up messages over the buttons; each picture gets a tick and the button reads **Back to shopping list (3 added)**. Voice takes several things at once: "milk, bread and eggs". |
| Settings labels | **Suggest things I often buy** and **Ask about a present when I add a birthday card**. |
| "Empty the list" was unclear and hard to find | Gone from Settings. A **Clear ticked items** button appears under the list once something is ticked, and becomes **Start a new list** when everything is bought. Things still to buy stay. Undo is offered. |
| No need for the Connected screen | Removed. Joining shows "Connected to Mary's list" and goes to the list. Who shares the list is shown in Family member tools. |
| "Disconnect Frank" felt scary | **Stop sharing with Frank?** "Frank's phone won't show this list any more. You can connect again any time." Still open for Wednesday's discussion. |

### Second round (after trying the app)

| Reported | In the app now |
|---|---|
| "Just milk is enough, we don't need its type" | One **Milk**, one **Juice**, one **Bread**, one **Tea**. Saying or typing "light milk", "oat milk", "orange juice" or "bread rolls" adds the plain item. Lists saved by the earlier version are converted automatically. |
| "Put a picture on everything, not icons" | **Every one of the 64 items has a realistic picture**: 30 real photographs and 34 photorealistic 3D pictures (see the next rows). Something typed that is not in the catalogue shows a shopping-bags picture. Icons now appear only on buttons. |
| "The layout is not good on a tablet" (list and Add items were side by side) | Tablets show **one screen at a time**, like a phone. See *Tablets* below. |
| "Make the layout very nice for tablets" | Each screen is a page in the middle of the tablet on a soft green background, and everything is drawn up to a fifth larger. **Held sideways**, the page uses the full width: the list or pictures on the left, the buttons in a column on the right at thumb height, and above them a summary (*5 things to buy*, *Shared with Frank*). **Held upright**, the buttons sit along the bottom as on a phone. Add items shows 5 pictures across sideways and 4 upright. |
| "Make the birthday card more reasonable and simple" | Adding a **Birthday card** now asks one question, with a picture of a present: **Buy a present too?** → **Yes, add a present** / **No, thanks**. No grid of gift ideas, no wrapping paper or candles pushed at the person (they can still be added like anything else). It is not asked if a present is already on the list. The birthday card has a proper picture (a card with a cake) instead of a love-letter envelope. Setting: **Ask about a present when I add a birthday card**. |
| "Add a Category feature ON and OFF button in Settings" | **Settings → Show categories in Add items.** On (the default): the buttons *All, Fruit and veg, Dairy, Bread…* sit above the pictures. Off: they disappear and Add items shows every picture in one list, usual items first. |
| "For each item just put one picture, not a bunch or kilos of them" | **Every picture now shows one item**: one apple, one banana, one carrot, one potato, one carton of milk. Photos come from the dataset's single-product studio shots instead of its supermarket shelves. |
| "Put real 3D pictures, not icons like cartoon" | **No cartoon pictures are left.** 30 items are real photographs of one item (apple, avocado, banana, milk carton, tomato and others). The other 34 (bread, eggs, cheese, butter, meat, fish, packets, jars, bottles, household items, cards and gifts, and a few vegetables) are **photorealistic 3D renders** made for this app in Blender with real-world materials and studio lighting, because no free photos of them could be found. Packets carry plain made-up labels (*TEA, SUGAR, RICE*), not real brands. The render scripts are in `tools/pictures/` so any picture can be changed. |
| "Make the list sync instantly across two devices" | Both devices keep a live connection to the list, so a new item, a tick or a removal shows on the other device in about a second. It now also: opens the list **at once** from the device's own copy instead of first waiting for the server (which could take several seconds on a slow connection); **reconnects immediately** when the app comes back to the front or the internet returns, instead of waiting up to a minute for Firebase to retry; and shows *Frank added milk* with the new item outlined when someone else adds something. See *How fast is sharing* below. |
| "I cannot add any devices / the list is not saved online" | That copy was built **without Firebase** (no `app/google-services.json`), so it could only keep the list on the phone. See **Sharing between phones** below. Sharing itself was also made sturdier: see *Why sharing failed* below. |

Also fixed while checking every screen for dead ends: the list scrolls to a newly added item;
voice explains *why* it did not work (not heard, needs internet, not set up) instead of always
"Didn't catch that"; listening, reading a photo, making or checking a code all give up with a
message after a time limit instead of waiting for ever; a scanner that cannot open goes straight
to *Type the number*; *Join* shows *Checking* and cannot be pressed twice; turning the phone
mid-sign-up or mid-voice carries on where it was; on short screens (phone sideways, keyboard
open, very large text) the buttons scroll with the page so nothing is squeezed out of reach.

---

### Third round (refinements)

> **After installing this version:** publish the updated **`firestore.rules`** (Firestore → Rules →
> paste → Publish), and for photo recognition do the two steps in *Photo recognition (AI)*.

| Asked for | In the app now |
|---|---|
| New logo, sky-blue theme, red bin | The **My List** logo is the app icon and is on the welcome screen. The green theme is now **light sky blue** (buttons a deeper sky blue so white text stays easy to read, 5.9:1). The bin on every row is **red**. The app's name under its icon is *My List*. |
| 1. Sign up went ahead without a name or phone number | **Both are needed now.** Sign up offers this phone's own number straight away (Google's one-tap Phone Number Hint, no permission). If the phone cannot offer it, the person types it or says it. Voice sign-up can no longer skip the number. |
| 2. Remove "Type" on Add items, add a search bar | **A search bar at the top** of Add items. Pictures narrow down with every letter ("ma" shows Mandarins and Mangoes), close spellings still match, and anything not in the catalogue can be added as typed with **Add "…"**. The Type button and screen are gone; *Type instead* on Say it opens the search bar. |
| 3. Profile: the keyboard closed while changing the name | Fixed. Opening the keyboard made the screen "short" and the page was rebuilt in a different layout, which dropped the text box's focus. Every screen now keeps one layout, so typing is never interrupted (this also fixes sign-up). |
| 4. After adding the present, go to the current list and show Add items | **Yes, add a present** puts it on the current list and shows the **Add items** page (opened if the person came from Say it or Photo), scrolled to the ticked Present picture, with *Present added to your list*. |
| 5. A new user was shown "Things you often buy" | That came from made-up shopping history that test builds added for demos. **Removed**, and wiped once from phones that had it. Suggestions now appear only after real shopping (something bought at least twice in 12 weeks). |
| 6. Profile: Save, then a message, then OK back to Settings | **Save** shows **Changed successfully!** with **OK**, which goes back to Settings. A missing or wrong number is pointed out on the field instead. |
| 7. Keep previously bought items, greyed out, deletable, deleted after a while | **Old lists**, under the list, **one folder per shop**: *Clear ticked items* (and *Start a new list*) move the bought things into a folder such as *List from today, 2 items*, instead of deleting them. Folders start closed; tap one to open it and see its greyed-out items, each with the red bin and **Add** to put it back on the list, plus **Add all again**. The folder's own red bin deletes the whole list. Shared phones see the same old lists. Undo still works. |
| 8. Automatic delete of old items, with − and + and 3, 7, 14 days | **Settings → Delete old lists after**: a big **−** and **+** around the number of days (1 to 60) and quick buttons **3 days, 7 days, 14 days**. Default 7. **One setting for the whole list**: changing it on one phone changes it on every phone sharing the list, so they all delete at the same time (needs the new `firestore.rules` published). |
| 9. Remind me with − and + and 3, 7, 14 days | **Remind me** uses the same − / + and quick buttons. |
| 10. The camera could not scan the QR code on the other phone | Google's separate scanner (which needed a module download and failed on some phones) is replaced by **a scanner inside the app**: CameraX shows the camera and ML Kit's barcode model, bundled with the app, reads the code. The camera permission is asked the first time. *Type the number instead* is always on the scan screen. |
| 11. Photo of a mandarin was not recognised | **Photo now uses AI**: Google's Gemini model (through Firebase AI Logic) says what the photo shows, for example *Mandarins*, and that is the first choice. Words read off a packet are still used as well, and offline. Needs the two console steps in *Photo recognition (AI)* below. |
| 12. Typing the pairing number should use the phone's own number keyboard | The six boxes are now an ordinary text box: **the phone's own number keyboard** opens straight away. Done on the keyboard joins. |
| 13. Profile name by voice: "Sam" was asked again | In Profile, *Say it* now fills only the box being edited and stops after **Yes**. (It used to carry on into the sign-up conversation and ask for the phone number.) |

## Run it

1. Install **Android Studio Ladybug (2024.2) or newer**. It includes the JDK 17 the project needs.
2. **File → Open** and choose the `ShoppingList` folder (the one containing `settings.gradle.kts`).
3. Wait for Gradle sync to finish. The first sync downloads Gradle 8.11.1 and the libraries, so it
   needs internet and can take several minutes.
4. Pick a phone or emulator and press **Run ▶**.

Runs on **Android 8.0 (API 26) and newer**: phones, tablets, foldables, portrait and landscape.

### Tablets

Tablets show **one screen at a time, the same as a phone**, so there is never more than one thing
to look at. The screen is a page in the middle with a soft sky-blue background around it, and
everything (text, pictures, buttons) is drawn up to 20% larger for reading at arm's length.

- **Never two columns.** Each screen is a single page whose width suits it: forms and settings a
  narrow page, the list a medium one, Add items a wide one for the pictures.
- **Forms and settings** (welcome, sign up, profile, settings, connect): the buttons sit right under
  the boxes they belong to, centred, instead of far away at the bottom of the page.
- **The list:** *Clear ticked items* and *Add items* side by side at the bottom.
- **Add items:** *Say it*, *Photo* and *Back to list* in one row at the bottom, so the pictures keep
  the height; about 6 pictures across sideways and 4 upright.
- A tablet counts as one whose shorter side is at least 600 dp (Android's own definition), so
  phones turned sideways keep the phone layout.

Without any further setup the app is fully usable on one phone. Sharing between phones needs
Firebase: see **Sharing between phones** below.

### Debug build

The debug build (what **Run ▶** installs) starts empty like a real new user: no made-up shopping
history. It differs from the release build only in small ways for testing:

- **Suggest things I often buy** and **Ask about a present when I add a birthday card** start switched on
  (suggestions still appear only after real shopping).
- **Remind me** has a *Show a test reminder* button.
- Family member tools shows a *For the team* line explaining any sharing problem.
- Firebase App Check uses the debug provider (see *Photo recognition (AI)*).

### Emulator notes

| Feature | On an emulator |
|---|---|
| **Say it** (voice) | Use a system image with **Google Play**. In the emulator's *Extended controls → Microphone*, turn on *Virtual microphone uses host audio input*. |
| **Photo** | Works with the emulator's virtual camera, but reading a packet needs a real printed label, so test this on a real phone. |
| **Scan code** | Uses the emulator's virtual camera (*Extended controls → Camera*); a real phone is easier. *Type the number instead* works everywhere. |
| **Photo (AI)** | Works on an emulator with internet once its App Check debug code is added (see *Photo recognition (AI)*). |
| **Reminders** | Allow notifications when asked. Use *Show a test reminder* rather than waiting days. |

A real phone is the best test for voice, photo and scanning.

---

## Sharing between phones (Firebase)

**Why "Sharing is not set up" appears.** The online list lives in a Firebase database, and every
Firebase database belongs to someone's Google account. The project cannot ship with one, so the app
looks for a file called `app/google-services.json` when it is built. Without that file it still
works fully, but **only on that one phone**: *Connect a phone* explains this instead of pairing, and
**Settings → Family member tools → Shared list** says *Only on this phone*. Adding the file once
fixes it for every phone the app is installed on from that build.

Takes about ten minutes, once, by one person in the team. The free **Spark** plan is enough;
no Cloud Functions and no billing are needed.

1. Go to <https://console.firebase.google.com>, sign in with a Google account, and **Create a project**
   (any name, for example *team48-shopping*). Google Analytics is not needed.
2. On the project page press the **Android** symbol to **add an Android app**. Package name:
   `au.edu.canberra.team48.shoppinglist` (exactly this). Nickname and SHA-1 can be left empty.
   **Download `google-services.json`** and copy it into the **`app/`** folder of this project, next
   to `app/build.gradle.kts`. Skip the remaining wizard steps; the project is already set up for them.
3. **Build → Authentication → Get started → Sign-in method → Anonymous → Enable → Save.**
   Nobody ever sees a sign-in screen: each phone signs itself in invisibly.
4. **Build → Firestore Database → Create database.** Choose a nearby location, for example
   `australia-southeast1` (Sydney), and **production mode**.
5. Still in Firestore, open the **Rules** tab, delete everything there, paste in the whole of
   **`firestore.rules`** from this project, and press **Publish**.
6. In Android Studio: **File → Sync Project with Gradle Files**, then **Run ▶** again, on **every**
   phone that should share the list (each phone needs the new build).

**Check it worked:** open **Settings → Family member tools**. Under **Shared list** it should say
**Working** within a few seconds (the phone needs internet). In the debug build, a line *For the team*
underneath says exactly what is missing if it does not:

| Shared list says | Meaning | Fix |
|---|---|---|
| Only on this phone | No `google-services.json` in this build. | Steps 1, 2 and 6. |
| Connecting… | Waiting for the internet or for Firebase. | Check the phone is online. |
| Not working: "could not sign in" | Anonymous sign-in is off. | Step 3. |
| Not working: "server refused" | Firestore rules not published (or still the default rules). | Step 5. |

**To connect two phones:** on each phone go to profile picture → **Settings → Family member tools →
Connect a phone**. One phone shows **My code**, the other uses **Scan code** or
**Type the number instead**. Codes last ten minutes and work once. Both phones must be built
with the same `google-services.json`.

### Photo recognition (AI)

*Photo* sends the picture to Google's Gemini model through **Firebase AI Logic** and gets back the
item's everyday name. It uses the same Firebase project as sharing. Two steps, once:

1. Firebase console → **AI Services → AI Logic → Get started**. Choose the **Gemini Developer API**
   (it has a no-cost tier) and let it enable the APIs.
2. **App Check** (Firebase requires it for AI Logic). Run the debug build once, then in Android
   Studio's **Logcat** search for `DebugAppCheckProvider`. It prints *"Enter this debug secret into the
   allow list in the Firebase Console for your project: 1234abcd-…"*. In the console open
   **Security → App Check → Apps**, the ⋮ menu next to the Android app → **Manage debug tokens**, and
   add that code. Each phone or emulator running a debug build prints its own code; add each one.

Release builds use **Play Integrity** instead, which works once the app is distributed through Google
Play (register the app's SHA-256 in App Check). Until then, photo recognition works in debug builds.

Without these steps, or without internet, Photo still works: it reads the words on a packet, and
otherwise asks what it is. The model names tried are at the top of `platform/ItemRecognizer.kt`; if
Google retires one, the next is used.

### How fast is sharing

Instant, while both devices are online and the app is open: each device keeps a live connection
(a Firestore listener) to the list, so a change on one shows on the other in about a second, with
no refreshing. To test: open the list on both devices side by side, add or tick something on one
and watch the other.

- The app shows its own change at once, before the server confirms it, so nothing ever waits.
- On opening, the list appears straight away from the device's own copy and then catches up.
- Coming back to the app, or getting internet back, reconnects immediately.
- **Offline,** changes are kept on the device and sent the moment it is online again. The list says
  *No internet* meanwhile.
- A device whose app is closed catches up as soon as it is opened. (Updating a closed app in the
  background would need push notifications through Cloud Functions, which need a paid plan.)

### Why sharing failed even with Firebase (fixed)

Testing found that the earlier version could get stuck even after Firebase was added:

- If the app was opened **before the rules were published**, it remembered a list the server had
  refused to create and kept trying to use it for ever. It now checks the list really exists on the
  server and that this phone belongs to it, and makes a fresh one if not, **keeping the items already
  on the phone**.
- Saves that the server refused were **silently dropped**. Every save is now watched; a refusal shows
  *The list could not be shared. It is still saved on this phone.* and the reason appears under
  Shared list.
- If anonymous sign-in was off, the phone gave up without saying anything. It now says so, and keeps
  retrying (2 seconds, doubling to once a minute) so it recovers by itself once the setting is fixed.
- Photos taken of an item were lost when the list was shared. They are now kept on that phone.
- The rules allowed a code only if the phone's clock was within a minute; this is now five minutes,
  so a phone whose clock is slightly off can still make a code.

Logcat (filter `ShoppingList`) shows the same messages in full.

---

## Tests

```
./gradlew test
```

Runs 86 unit tests covering the app's rules: the list behaves like paper, clearing ticked items
and undo, voice and photo matching including several items in one sentence, one plain item per
product (and old lists converted), suggestions and
birthday items, reminders, pairing codes, and name and phone checks.
In Android Studio: right-click `app/src/test` → *Run Tests*.

---

## How this was verified

Built and checked in an environment without Android Studio, where Google's Android download
servers are blocked, so **no APK was built there**. Instead:

- **All 86 unit tests were compiled and run: 86 passed.**
- **The whole app was type-checked** by the Kotlin 2.1 compiler against stand-in versions of
  every Android, Compose, Firebase, ML Kit and Play services API it uses, written with the real
  signatures: 0 errors, 0 warnings. Planted mistakes (a missing picture, a misspelt parameter,
  a wrong function, a missing import) are all caught.
- **Compose's own rules were checked** with the real Compose compiler plugin: 0 problems. A
  planted Compose mistake (drawing text from inside a button's click handler) is caught.
- **Every screen and pop-up was walked through** for dead ends: every screen has a way back,
  every button does something visible, every wait ends with a message, and every failure
  offers a way forward.
- **Every resource was validated**: all XML files parse and every `@drawable`, `@color`, `@xml`
  and `@mipmap` reference exists. **Every one of the 64 catalogue items has its own picture file.**
- **Sharing was not tested against a live Firebase project** from that environment (Google's
  servers are blocked there). The Firestore code was type-checked against the real API signatures
  and the rules re-read line by line.
- **Both build types were type-checked**: debug (App Check debug provider) and release (Play
  Integrity), 0 errors each. The new camera scanner (CameraX, ML Kit barcode), Firebase AI Logic
  and App Check code was checked against stand-ins written from their real signatures, and planted
  mistakes in it were caught.
- **Photo recognition and the in-app scanner were not run** here (no camera, no Google servers).
  The rules that turn the AI's answer into a list item are unit tested (a mandarin becomes
  *Mandarins*; singular and plural; things not in the catalogue).

What that cannot prove: how it looks and behaves on a real device. **The first real build happens
on your machine.** If anything fails, copy the first error from the *Build* window and it can be
fixed quickly.

---

## Project layout

```
app/src/main/java/au/edu/canberra/team48/shoppinglist/
  domain/     The rules, plain Kotlin, no Android. All unit tested.
              Catalog (64 items, every one with a picture), Matching (voice, search, photo and AI answers),
              ListLogic (the list and Bought before), Suggestions, Reminders and Days, Pairing, ProfileRules.
  data/       Prefs (on the phone), Repository (on-phone list), FirestoreRepository (shared list).
  platform/   Speech, photos and text recognition, ItemRecognizer (AI), QrCamera (in-app scanner),
              QR codes, network status, reminders.
  ui/         Theme (colours, Atkinson Hyperlegible), Components, Overlays (pop-ups and messages),
              and the screens: Start, List (with search and Bought before), Input (voice and photo),
              Settings (with the days picker), Connect (scan and type the number).
  AppViewModel.kt   Every screen's state and every action.
  MainActivity.kt   The only activity.
app/src/debug/, app/src/release/   App Check setup for each build type.
app/src/main/res/
  drawable-nodpi/   65 item pictures, one item in each (30 photos, 35 photorealistic renders), and the My List logo.
  mipmap-*/         The launcher icon, made from the logo.
  drawable/         54 vector icons (buttons, the themed-icon layer, notification icon).
  font/             Atkinson Hyperlegible.
firestore.rules     Security rules for sharing.
ATTRIBUTION.md      Picture, icon and font credits.
tools/pictures/     Scripts that render the photorealistic item pictures.
licenses/           Full licence texts.
```

### Changing the pictures

To use your own photo for an item, replace `app/src/main/res/drawable-nodpi/photo_<key>.jpg`
with a square JPEG of the same name, about 320 to 640 pixels. To add a new item, add a line to
`domain/Catalog.kt` and a matching photo and line in `ui/Pictures.kt`; an item without one shows
the shopping-bags picture.

The photorealistic 3D pictures are made by the scripts in `tools/pictures/` (Blender as a Python
module). Its README shows how to change one and render it again, for example a different label on
the tea box. ATTRIBUTION.md says where every picture came from.

---

## Accessibility

- Text follows the **phone's own text size and dark mode**. There is no separate in-app setting,
  as evaluation participants asked.
- Buttons are at least 56 dp tall and list rows at least 80 dp. The tick box sits at the far right
  and the bin at the far left, so one cannot be pressed instead of the other.
- A ticked item turns grey. **The word is never struck through.**
- Every control has a screen reader label. Pop-ups hide the screen behind them from TalkBack, and
  messages are announced.
- Colours were checked against WCAG 2.1: body text 13.9:1, button text 8.7:1.
- Permissions are asked only when first needed: the microphone on first *Say it*, notifications
  when *Remind me* is switched on. **Photo needs no camera permission**: it hands over to the
  phone's own camera app.

---

## Known limitations

- **A household's own photos are not shared.** A photo taken on one phone replaces the picture on
  that phone only; other phones show the catalogue picture. (Sharing photos would need Firebase
  Storage, which needs a paid plan.)
- **Reinstalling the app starts afresh.** The invisible identity and sign-up belong to that
  installation, so re-pair after reinstalling. Backups are switched off on purpose, so a restored
  copy cannot claim a list it no longer has access to.
- **Six-digit codes** are protected by a ten-minute life and single use. Before any public release,
  add Firebase App Check to stop automated guessing.
- **The phone number is optional and stored for display only.** It is not verified by SMS.
  *My number* needs Google Play and a SIM; on an emulator it says to type the number instead.
- **Packaged product photos (milk, juice, yoghurt, sour cream) show Swedish brands** (see ATTRIBUTION.md). Replace them with Australian
  products before participant sessions.
- **Android only.** The proposal's web and iOS versions are not included.
