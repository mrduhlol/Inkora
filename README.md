# INKORA — a quiet notebook for Android

![CI](https://github.com/mrduhlol/Inkora/actions/workflows/android.yml/badge.svg)
![Release](https://img.shields.io/github/v/release/mrduhlol/Inkora?label=version&color=6750A4)
![Platform](https://img.shields.io/badge/platform-Android%2026%2B-3DDC84)
![Offline](https://img.shields.io/badge/offline-100%25-FAF3E3)
![Stack](https://img.shields.io/badge/Kotlin_·_Compose_·_Room-6750A4)

> **Home is a clean notebook. Notes are small sheets of paper with a folded
> corner. Tap one and it opens like a real page — formatted, autosaved, yours.**
> No account. No cloud. No tracking. Everything stays on your device.

<p align="center">
  <a href="https://github.com/mrduhlol/Inkora/releases/latest/download/Inkora-v1.3.apk">
    <img src="https://img.shields.io/badge/Download_Inkora_v1.3_APK-6750A4?style=for-the-badge&logo=android&logoColor=white" alt="Download Inkora v1.3 APK" />
  </a>
</p>

<p align="center">
  <sub>Android 8.0 and up · install via <b>Allow unknown apps</b>, then open the APK · updates arrive through future releases</sub>
</p>

---

## New in v1.3

v1.3 turns the polished notebook into a capable everyday notebook — still offline, still lightweight:

- **Pinning and sections** — pin notes to a Pinned section above All Notes, with a subtle pin badge and per-section sorting that persists.
- **Sorting and views** — Recently updated, Recently created, Title A–Z and Title Z–A in a compact Sort control, plus Grid and List views (list rows keep the folded-paper swatch, excerpt and relative timestamps). Both persist across restarts.
- **Multi-select** — long-press to select, with a "N selected" bar for pin, favorite, folder-move and trash batch actions; Back exits selection.
- **Richer editor** — headings (H1–H3 cycle), quote blocks with an accent bar, real horizontal dividers, per-paragraph alignment (left, center, right, justify) and indentation, all as actual formatting with toolbar states.
- **Images, kept local** — system photo picker (no storage permission), app-private files with Room metadata, downsampled thumbnails, tap-to-view, guarded removal, survival across trash and restart.
- **Duplicate, share, info** — one-tap independent copies titled "Original (Copy)", native Android share sheet with clean text, and a Note Info dialog (dates, counts, folder, flags).
- **Backup your way** — Storage and Data settings with on-demand usage stats, one-file JSON export (notes, folders, formatting, downsampled images) via the system file picker, guarded import that always creates new notes, and confirmed Empty Trash.
- **Safer by default** — additive Room migration (existing notes untouched), save-failure surfacing with retry, search across titles, bodies and folder names.

## New in v1.2

v1.2 polishes the v1.1 foundation without changing its architecture:

- **Accent cursor and live toolbar states** — the caret uses your accent color, and Bold, Italic, Underline, Strike, Bullet, Numbered and Checklist buttons light up exactly when the cursor or selection carries that formatting (mixed selections stay honest).
- **Move to folder from the editor** — the `⋮` menu assigns notes to folders without duplicating them; deleting a folder keeps its notes.
- **Visual page-style picker** — Blank, Ruled, Grid and Dotted now show true mini paper previews and apply instantly.
- **Paper that adapts** — rulings, grids and dots re-tint for dark pages so they stay subtle and readable.
- **Derived titles** — untitled notes show their first content line in previews and lists; your stored title is never overwritten.
- **Instant creation** — new notes open with the title field focused.
- **Safer Trash** — permanent delete asks first; restore keeps content, formatting, folder, favorite and page style.
- **Clearer search** — no-match searches get their own empty state instead of a blank grid.
- **Calmer Settings** — grouped sections with dividers and plain-language control descriptions.

## New in v1.1

v1.1 rebuilds the editor around real rich text and fixes the most-reported v1.0 issues:

- **Selection-based formatting** — select a word, tap Bold, and only that word turns bold. No `**`, `_`, `<u>` or `~~` markers anywhere, ever.
- **Lists that behave** — bullets (`•`), numbered (`1. 2. 3.`) and checklists (`☐` / `☑`) with Enter-to-continue and Enter-on-empty-to-exit. Tapping a checkbox toggles it.
- **Undo / redo** — every formatting, list and checkbox change is one undo step.
- **Toolbar above the keyboard** — the formatting bar rides directly on top of the IME and disappears cleanly with it.
- **Anchored page menu** — the `⋮` popup now opens at the `⋮` button on every screen size and orientation.
- **Accents that actually work** — Blue, Purple, Green, Orange, Red, Pink and Teal drive the FAB, switches, checkboxes, dialogs and highlights immediately, persist across restarts, and respect System / Light / Dark / AMOLED plus Dynamic Color.
- **Readable paper** — ink, cursor and placeholders automatically go dark-on-light and light-on-dark per page background.
- **Clean previews** — home cards show human-readable excerpts; legacy notes with old markers are migrated to real formatting without losing text.
- **New icon** — the luminous feather, on deep navy.

## The experience

| Home — paper grid | Editor — a real page |
|---|---|
| `INKORA` wordmark with room to breathe | A large paper surface, not a form |
| Notes look like sheets with a **folded top-right corner**, drawn in Compose | Title plus body with cursor, selection, copy and paste |
| Every preview shows real title and real content — never placeholder text | **Autosave** — debounced Room writes with a flush on back. You never wonder "did I save?" |
| Responsive: **2 columns** on phones, 3–4 on tablets | A contextual formatting bar: bold, italic, underline, strike, bullets, numbered, checklist, undo, redo |

**Paper, your way.** Backgrounds — white, cream, gray, dark (plus custom hex) — crossed with styles — blank, ruled, grid, dotted. Each note remembers its own paper on Home and in the editor, independent of the app theme.

**Calm theming.** System, Light, Dark and true-black AMOLED, seven accents, Material 3 with optional dynamic color that never hijacks Inkora's identity.

**Fully offline.** Create, format, trash, restore, favorite, archive, search and file notes with airplane mode on. Notes survive app kill, force-stop and reboot via Room.

## Inside v1.3

- [x] Home sections (Pinned, All Notes), four sort orders, grid and list views
- [x] Multi-select with pin, favorite, folder-move and trash batch actions
- [x] Span-based rich editor: headings, quotes, dividers, alignment, indent
- [x] Local images with metadata store, viewer and guarded removal
- [x] Duplicate, native share, Note Info with live counts
- [x] JSON backup export and guarded import via Storage Access Framework
- [x] Storage usage stats and confirmed Empty Trash
- [x] Additive Room v1 to v2 migration; legacy notes preserved
- [x] Accessibility: 48dp targets, content descriptions, scalable type, contrast
- [x] Unit tests: rich text, editor behavior, export, persistence, folders, theming

**Deliberately not in v1.1** (architected for, not shipped): handwriting, images, PDF, audio, AI, cloud sync and Inkora-web. v1.1 is the stable paper foundation.

## How it is built

Native Android only. Kotlin, Jetpack Compose, Material 3, Room, DataStore, Coroutines and StateFlow, ViewModel, Repository pattern, Navigation Compose, Hilt, Gradle Kotlin DSL with a Version Catalog.

```
Compose screens and components
  ↓ StateFlow (lifecycle-aware)
ViewModel (testable, no framework details)
  ↓ suspend and Flow
Repository interface → implementation (domain ↔ data mapping)
  ↓
Room DAO → InkoraDatabase → on-device storage
```

Formatting is stored as span and block structure (`rich-v1` JSON in the note body), so V2 attachments can land in side tables keyed by note id with no rewrite of the notes table.

```
app/src/main/java/com/abhishek/inkora/
  data/local/database/   InkoraDatabase, NoteDao, FolderDao, entities
  data/repository/       NoteRepositoryImpl, FolderRepositoryImpl, SettingsRepository
  domain/model/          Note, Folder, RichText, PageStyle, AppTheme, AccentColor
  domain/repository/     NoteRepository, FolderRepository
  di/                    DatabaseModule, RepositoryModule
  ui/theme/              Color, Theme, Type (single source of tokens)
  ui/navigation/         InkoraNav (type-safe routes)
  ui/components/         Paper preview, paper surface, folded corner, FAB, toolbar…
  features/home, editor, settings, trash, favorites, folders, archive
  MainActivity, InkoraApp
```

## Get it

**Option A — download (easiest).**

[Download Inkora v1.3 APK](https://github.com/mrduhlol/Inkora/releases/latest/download/Inkora-v1.3.apk)

Then allow installs from unknown apps, open the file, and write your first page.

**Option B — build it.** Requires JDK 17, Android SDK 34 and a recent Android Studio:

```powershell
.\gradlew :app:assembleDebug        # APK lands in app/build/outputs/apk/debug/
.\gradlew :app:testDebugUnitTest    # unit tests: rich text, editor, persistence, theme
```

CI builds every push to `main` the same way (`.github/workflows/android.yml`) and attaches the APK to every `v*` tag automatically.

## Privacy

No `INTERNET` permission for core notes. No analytics, ads, accounts or servers. Your words never leave the phone unless you copy them out. Cloud sync remains a post-V4 opt-in idea, never a requirement.

## Roadmap

- **V2** — pen, pencil, brush, highlighter, shapes, stylus with pressure, images, camera, PDF import and annotation
- **V3** — audio, speech-to-text, OCR, handwriting recognition, deeper search
- **V4** — Inkora AI (summaries, flashcards, quizzes, grammar), on-device first
- **V5** — optional account with cross-device sync, plus the Inkora-web companion

## Contribute

Issues and pull requests are welcome. Keep the notebook calm: no gradients for fun, no dashboard clutter, no fake buttons — every visible control must work. Commit messages follow the existing `feat/fix/test/docs/ci/chore` style, one file per commit.

---

<p align="center"><b>INKORA v1.3</b> — write quietly. · <a href="https://github.com/mrduhlol/Inkora/releases/tag/v1.3">Release notes</a> · <a href="https://github.com/mrduhlol/Inkora/releases/latest/download/Inkora-v1.3.apk">Download APK</a></p>
