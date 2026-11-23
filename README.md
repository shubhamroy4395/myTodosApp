# To dos

**A todo app that works like texting yourself on WhatsApp.**

For years I kept my todos in a WhatsApp chat with only myself in it: dated lists, one message after another. This app keeps that habit and adds what WhatsApp is missing. You can tick items off inside the bubble, see what's still pending across days, and keep random thoughts in their own place.

Everything stays on your phone. There's no account and no internet.

<!-- Screenshots: create a `screenshots/` folder next to this README and save your images with these names -->
<p align="center">
  <img src="screenshots/todos.png" width="240" alt="To dos tab" />
  <img src="screenshots/thoughts.png" width="240" alt="Thoughts tab" />
  <img src="screenshots/pending.png" width="240" alt="Pending tab" />
</p>
<p align="center">
  <img src="screenshots/themes.png" width="240" alt="Theme picker" />
</p>
<img width="627" height="1280" alt="WhatsApp Image 2026-11-23 at 10 13 50 PM" src="https://github.com/user-attachments/assets/7ab63801-5e32-4b15-a7b5-b19f12e2e8a6" />


## Features

### ✅ To dos: a chat you can tick off
- **Type a todo and press Enter.** It's saved straight away and goes into the current bubble. Press Enter 10 times and you get a 10-item list in one bubble.
- **Tap ✓ to finish a list.** The next todo starts a new bubble. A new bubble also starts on its own after 30 quiet minutes or at midnight.
- **Tap any line to tick it.** It gets crossed out, and tapping it again undoes the tick.
- **Titles:** a line ending in `:` (like `Jazz:`) starts a new bubble with a bold title. Pasting a list that has a title line followed by numbered lines works too.
- **WhatsApp ticks show progress:** one grey ✓ means nothing in the bubble is done, two grey ✓✓ mean some of it is, and coloured ✓✓ mean all of it is.
- **Full date and time** (like `23 Nov 2026 · 8:58 pm`) on every bubble.
- **Vertical progress line:** a timeline runs down the left side with a marker for each day (`Today · Monday, 23 November 2026`). Each day's ring fills as you finish that day's todos, and the line next to each bubble fills as you tick its items.
- **Hold a todo** to see when it was added and when it was done, or to delete it or its whole list.

### 💭 Thoughts
- A separate chat for random thoughts. Here Enter makes a new line, so you can write paragraphs, and the arrow button saves the thought.
- **Hold a thought** to copy, share or delete it.
- The top-bar icons **copy or share every thought at once**.

### 🌅 Pending: your morning view
- A greeting card at the top with today's date, how many todos are pending, how many were carried over from earlier days, and your oldest pending day.
- Every pending todo, grouped by the day you added it, **oldest first** so nothing gets buried.
- Tap a todo to tick it. It moves to **Done today**, and tapping it there undoes the tick.

### 🎨 And also
- **Swipe between tabs:** To dos → Thoughts → Pending. The app reopens on the tab you used last.
- **Copy all / Share all** for whichever tab you're on.
- **6 themes:** Default (WhatsApp dark with rust bubbles), Winter, AMOLED dark, Light, Spring and Sunset. They apply instantly and are remembered.
- **Backup:** ⋮ → *Export backup file* saves everything as a `.json` file wherever you choose, such as Google Drive or Downloads. *Import backup file* adds it back without overwriting anything.

---

## Quick reference

| Do this | Where | What happens |
|---|---|---|
| Type + **Enter** | To dos | Adds the todo to the current bubble |
| Tap **✓** | To dos | Finishes the bubble, so the next todo starts a new one |
| Type `Title:` + Enter | To dos | Starts a new bubble with that title |
| **Tap** a todo | To dos / Pending | Ticks or unticks it |
| **Hold** a todo | To dos | Shows when it was added and done; delete options |
| **Hold** a bubble title | To dos | Deletes the whole list |
| Type + **arrow** | Thoughts | Saves a thought (Enter = new line) |
| **Hold** a thought | Thoughts | Copy / Share / Delete |
| Copy / Share icons | Top bar | Copies or shares the whole current tab |
| Palette icon | Top bar | Changes the theme |
| ⋮ menu | Top bar | Exports or imports a backup file |

---

## Your data

- Everything is stored in a **SQLite database on your phone**, in the app's private storage (`databases/todos.db`). Your theme and last-used tab are kept in the app's settings file.
- **Nothing ever leaves your phone.** The app doesn't ask for internet permission.
- **Nothing expires.** Data stays until you uninstall the app or tap *Clear storage* in App info.
- Updating the app from Android Studio keeps your data. **Uninstalling deletes it**, so export a backup first.
- To be safe if you lose or reset your phone, export a backup now and then and keep the file in Google Drive.

---

## Tech

- **Kotlin** + **Jetpack Compose** (Material 3)
- **SQLite** through Android's built-in `SQLiteOpenHelper`, with no Room and no extra dependencies
- **SharedPreferences** for the theme and last tab
- Icons are drawn in code, so no icon library is needed
- **No third-party libraries.** The project uses only what Android Studio's *Empty Activity* template includes

### Project structure

```
app/src/main/java/com/shubham/todos/
├── MainActivity.kt          app entry, theme + status bar colours
├── data/
│   ├── Todo.kt              TodoItem, TodoList, Thought + small helpers
│   ├── Database.kt          SQLite tables (lists, items, thoughts)
│   ├── TodoStore.kt         add / tick / delete, backup export & import
│   └── Settings.kt          last tab + theme
└── ui/
    ├── theme/               (generated by Android Studio, unused)
    └── chat/
        ├── ChatScreen.kt    top bar, tabs, swipeable pages, theme picker
        ├── TodosPage.kt     To dos tab + vertical progress timeline
        ├── ThoughtsPage.kt  Thoughts tab
        ├── PendingPage.kt   Pending tab (morning view)
        ├── MessageBubble.kt todo and thought bubbles
        ├── Common.kt        input bar, ticks, check circles, icons, wallpaper
        ├── ChatColors.kt    the 6 themes
        ├── DateLabels.kt    "8:58 pm", "Today · Monday, 23 November 2026"…
        └── Export.kt        copy / share as plain text
```

---

## Build & run

1. Clone this repo and open the folder in **Android Studio**.
2. Wait for Gradle sync to finish.
3. Connect your Android phone with **USB debugging** on (or start an emulator).
4. Pick your device at the top and press **▶ Run**.

To install it without Android Studio, use the APK from the [Releases](../../releases) page if one has been published.

---

## Roadmap

- [ ] Morning notification (e.g. 9 am) that opens the Pending tab
- [ ] Snooze / move a todo to another day
- [ ] Due dates and reminders
- [ ] Home-screen widget with today's pending count
- [ ] Search across todos and thoughts
- [ ] Share a WhatsApp message straight into the app

---

## Author

Built by **Shubham Roy**, a product manager who got tired of scrolling up in a WhatsApp chat to find what was still pending.

[GitHub](https://github.com/shubhamroy4395) · [LinkedIn](https://www.linkedin.com/in/roy-shubham/)
