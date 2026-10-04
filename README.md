# INKORA — Native Android Notes

Offline-first, native Android note-taking app (Kotlin + Jetpack Compose + Material 3 + Room).

> Home is a clean notebook. Notes look like small sheets of paper with a folded
> top-right corner. Opening a note reveals a paper-like editor with autosave.
> No account, no network, no analytics. Everything stays on-device.

## Stack

- Kotlin, Jetpack Compose, Material 3, AndroidX
- Room (single source of truth), DataStore (preferences)
- Coroutines + StateFlow + ViewModel, Repository pattern
- Navigation Compose, Hilt, Gradle Kotlin DSL + Version Catalog

## Architecture

```
UI (Compose screens, components)
 ↓ StateFlow
ViewModel
 ↓ suspend / Flow
Repository (domain interface → data impl)
 ↓
Room DAO → InkoraDatabase → device storage
```

- Composables never touch Room directly.
- Business logic lives in ViewModel / Repository, not in UI.
- Rich-text-ready content model: V1 stores Markdown-ish plain text in
  `content` plus `contentFormat` (`"md-v1"`), so V2/V3 (drawings, images,
  audio, PDF) can add tables/attachments without rewriting the schema.

## Offline-first

Create / edit / delete / restore / favorite / search / archive / folders all
work with zero connectivity. Notes persist across process death and reboot via
Room. No `SharedPreferences` for notes.

## Project layout

```
app/src/main/java/com/abhishek/inkora/
  data/local/database/  (InkoraDatabase, NoteDao, FolderDao, entities)
  data/repository/      (NoteRepositoryImpl, FolderRepositoryImpl, SettingsRepository)
  domain/model/         (Note, Folder, PageStyle, enums)
  domain/repository/    (interfaces)
  di/                   (Hilt modules)
  ui/theme/             (Color, Theme, Type, InkoraTokens)
  ui/navigation/        (InkoraNav)
  ui/components/        (Paper preview, FoldedCorner, FAB, TopBar, Grid, toolbars…)
  features/home/ editor/ settings/ trash/ favorites/ folders/
```

## Build

Requirements: JDK 17+, Android SDK 34, Android Studio Ladybug+.

```powershell
# open in Android Studio, let it sync, then:
.\gradlew :app:assembleDebug
.\gradlew :app:testDebugUnitTest
```

First launch seeds nothing into the user's DB. A debug-only sample dataset
(Physics Notes, Project Ideas…) is available via `SeedDemoNotesUseCase` and is
never forced into release builds.

## V1 scope

Home grid (2-col phones, adaptive tablets), paper preview with Compose-drawn
folded corner, bottom-left `+` FAB with instant persist + autosave editor,
title/body, bold/italic/underline/strike + bullet/numbered/checklist,
page backgrounds (white/cream/gray/dark/custom) × styles (blank/ruled/grid/dotted),
theming (system/light/dark/AMOLED + accent + dynamic color), search (Room FTS-like
`LIKE` queries), favorites, archive/trash/restore, folders, DataStore settings.

Future (architected, not implemented): handwriting/drawing, images/PDF/audio,
AI, cloud sync, Inkora-web.

## Privacy

No network permission required for core notes. No analytics/ads SDKs.
