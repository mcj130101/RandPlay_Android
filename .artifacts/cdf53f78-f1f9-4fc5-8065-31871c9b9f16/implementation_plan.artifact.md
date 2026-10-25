# Implementation Plan - File Explorer

Add a complete file explorer functionality to browse, search, sort, and perform file operations (edit, delete, move, copy) on the configured source folders (Local and SMB).

## User Review Required

> [!IMPORTANT]
> The file explorer will operate on the configured sources. For local folders, it uses the Storage Access Framework (SAF) permissions already granted when adding the source. For SMB, it uses the credentials associated with the source.

> [!WARNING]
> File operations like Delete and Move are destructive. I will implement confirmation dialogs for these actions.

## Proposed Changes

### Data Layer

#### [NEW] [FileBrowser.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/domain/browser/FileBrowser.kt)
Define a common interface for file operations across different source types.

#### [NEW] [LocalFileBrowser.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/data/browser/LocalFileBrowser.kt)
Implementation of `FileBrowser` using `DocumentFile` for local SAF-managed folders.

#### [MODIFY] [SmbBrowser.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/data/scanner/SmbBrowser.kt)
Expand `SmbBrowser` to implement `FileBrowser` and support files, deletion, renaming, and copying.

### UI Layer

#### [NEW] [ExplorerScreen.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/ui/explorer/ExplorerScreen.kt)
A new screen to display the file list, breadcrumbs, search bar, and action menus.

#### [NEW] [ExplorerViewModel.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/ui/explorer/ExplorerViewModel.kt)
ViewModel to manage the explorer state, navigation within the file system, and orchestration of file operations.

#### [MODIFY] [SourcesScreen.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/ui/sources/SourcesScreen.kt)
Add a "Browse" button to each source item to open the explorer for that source.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/MainActivity.kt)
Add the Explorer route to the navigation graph.

## Verification Plan

### Automated Tests
- I will add unit tests for `LocalFileBrowser` and `SmbBrowser` logic where possible (mocking `DocumentFile` and `SmbFile`).
- UI tests for the Explorer screen using Compose Test Rule.

### Manual Verification
1.  Add a local source and browse it.
2.  Perform search and sort operations.
3.  Rename a file/folder.
4.  Delete a file/folder.
5.  Copy/Move a file from one folder to another within the same source.
6.  Repeat for an SMB source.
