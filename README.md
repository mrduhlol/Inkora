# ✒️ INKORA — a quiet notebook for Android

![CI](https://github.com/mrduhlol/Inkora/actions/workflows/android.yml/badge.svg)
![Release](https://img.shields.io/github/v/release/mrduhlol/Inkora?label=v1.0&color=6750A4)
![Platform](https://img.shields.io/badge/platform-Android%2026%2B-3DDC84)
![Offline](https://img.shields.io/badge/offline-100%25-FAF3E3)
![Stack](https://img.shields.io/badge/Kotlin_·_Compose_·_Room-6750A4)

> **Home is a clean notebook. Notes are small sheets of paper with a folded
> corner. Tap one and it opens like a real page — and it saves itself.**
> No account. No cloud. No tracking. Everything stays on your device.

<p align="center">
  <a href="https://github.com/mrduhlol/Inkora/releases/latest/download/Inkora-debug.apk">
    <img src="https://img.shields.io/badge/⬇_Download_Inkora_v1.0_APK-6750A4?style=for-the-badge&logo=android&logoColor=white" alt="Download Inkora v1.0 APK" />
  </a>
</p>

<p align="center">
  <sub>Android 8.0+ · ~15 MB · install via <b>Allow unknown apps</b> → open the APK · updates via future releases</sub><br />
  <sub>Prefer QR / mirror? Grab <code>Inkora-v1.0-debug.apk</code> from <a href="https://github.com/mrduhlol/Inkora/releases/tag/v1.0">Releases → v1.0</a>.</sub>
</p>

---

## ✨ What v1.0 feels like

| Home — paper grid | Editor — a real page |
|---|---|
| `INKORA` wordmark, lots of breathing room | Large paper surface, not a form |
| Notes look like sheets with a **folded top-right corner** (drawn in Compose, no images) | Title + body, cursor, keyboard, copy/paste, select |
| Real title + real content in every preview — never lorem ipsum | **Autosave** — debounced Room writes, flush on back. Never asks “did I save?” |
| Responsive: **2 columns** on phones, 3–4 on tablets | Bottom **formatting bar**: bold · italic · underline · strike · bullets · numbered · checklist |

**Paper, your way.** Backgrounds — white · cream · gray · dark (+ custom hex) — crossed with styles — blank · ruled · grid · dotted. Each note remembers its own paper, on Home *and* in the editor.

**Calm theming.** System / Light / Dark / **AMOLED**, 7 accents (blue · purple · green · orange · red · pink · teal), Material 3 + dynamic color that never hijacks Inkora’s identity.

**Everything offline.** Create, edit, trash, restore, favorite, archive, search, folders — with airplane mode on. Notes survive app kill, force-stop, and reboot via Room.

## 📦 What’s inside v1.0

- [x] Home grid + paper preview + folded corner + empty state
- [x] Bottom-**left** `+` FAB → instant note, straight into editor
- [x] Editor with autosave + Markdown foundation (`**bold**`, `_italic_`, `<u>`, `~~strike~~`, `- `, `1. `, `- [ ]`)
- [x] Page styles + paper backgrounds (persisted per note)
- [x] Local search across title + content (Room queries, debounced)
- [x] Favorites (star on preview + dedicated screen)
- [x] Archive + Trash (soft-delete via `isDeleted`, restore, permanent delete)
- [x] Folders (create / rename / delete — deleting a folder keeps its notes)
- [x] Settings via DataStore (theme, accent, dynamic, defaults, sort, grid)
- [x] Accessibility: 48dp targets, content descriptions, scalable type, contrast
- [x] Tests: Room persistence + Compose UI

**Deliberately not in v1.0** (architected for, not shipped): handwriting, images/PDF/audio, AI, cloud sync, Inkora-web. V1 is the stable paper foundation.

## 🏗️ How it’s built

**Native Android only.** Kotlin · Jetpack Compose · Material 3 · Room · DataStore · Coroutines/StateFlow · ViewModel · Repository pattern · Navigation Compose · Hilt · Gradle Kotlin DSL + Version Catalog.

```
Compose screens / components
  ↓ StateFlow (lifecycle-aware)
ViewModel (no Android framework, testable)
  ↓ suspend / Flow
Repository interface → impl (domain ↔ data mapping)
  ↓
Room DAO → InkoraDatabase → on-device storage
```

> **Content-format note:** V1 stores lightweight Markdown-ish text with a `contentFormat = "md-v1"` tag. V2+ attachments (drawings, images, audio, PDF) land in side tables keyed by `noteId` — no rewrite of the notes table.

```
app/src/main/java/com/abhishek/inkora/
  data/local/database/  InkoraDatabase · NoteDao · FolderDao · entities/
  data/repository/      NoteRepositoryImpl · FolderRepositoryImpl · SettingsRepository · SeedDemoNotesUseCase (debug only)
  domain/model/        Note · Folder · PageStyle · PaperBackground · AppTheme · AccentColor · SortOrder
  domain/repository/   NoteRepository · FolderRepository
  di/                  DatabaseModule · RepositoryModule
  ui/theme/            Color · Theme · Type  (single source of tokens — no scattered hex)
  ui/navigation/       InkoraNav (type-safe routes)
  ui/components/       InkoraPaperPreview · InkoraPaperSurface · FoldedCorner · InkoraFab · InkoraTopBar · NoteGrid · EmptyNotesState · FormattingToolbar · PageStyleSelector · AccentColorSelector · ThemeSelector
  features/home · editor · settings · trash · favorites · folders · archive
  MainActivity · InkoraApp
```

## 🚀 Get it

**Option A — download (easiest).** Tap the button at the top, or:

👉 **[Download Inkora APK](https://github.com/mrduhlol/Inkora/releases/latest/download/Inkora-debug.apk)**

Then: `Allow install from unknown apps` → open the file → write your first page.

**Option B — build it.** Needs JDK 17 + Android SDK 34 + Android Studio Ladybug+:

```powershell
.\gradlew :app:assembleDebug        # APK → app/build/outputs/apk/debug/
.\gradlew :app:testDebugUnitTest    # Room persistence tests (Robolectric)
```

CI builds every push to `main` the same way (`.github/workflows/android.yml`) and attaches the APK to every `v*` tag automatically.

## 🔒 Privacy

No `INTERNET` permission for core notes. No analytics, ads, accounts, or servers. Your words never leave the phone unless *you* copy them out. Cloud sync is a post-V4 opt-in idea, never a requirement.

## 🗺️ Roadmap

- **V2** — pen/pencil/brush/highlighter, shapes, stylus + pressure, images, camera, PDF import/annotate
- **V3** — audio, speech-to-text, OCR, handwriting recognition, deeper search
- **V4** — Inkora AI (summaries, flashcards, quizzes, grammar) — on-device first
- **V5** — optional account + cross-device sync · **Inkora-web** companion

## 🤝 Contribute / file bugs

Issues and PRs welcome. Keep the notebook calm: no gradients-for-fun, no dashboard clutter, no fake buttons — every visible control must work.

---

<p align="center"><b>INKORA v1.0</b> — write quietly. · <a href="https://github.com/mrduhlol/Inkora/releases/tag/v1.0">Release notes</a> · <a href="https://github.com/mrduhlol/Inkora/releases/latest/download/Inkora-debug.apk">Download APK</a></p>
