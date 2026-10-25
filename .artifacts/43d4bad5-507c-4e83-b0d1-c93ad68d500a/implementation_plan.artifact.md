# Implementation Plan - Fix Compilation Error in SourceEditorViewModel

Fix the compilation error "Cannot infer type for this parameter. Specify it explicitly" in `SourceEditorViewModel.kt` at line 113.

## User Review Required

> [!IMPORTANT]
> The error is caused by a missing method `listFolders` in the `SmbBrowser` class. I will implement this method to resolve the type inference issue in the ViewModel.

## Proposed Changes

### Data Scanner Component

#### [MODIFY] [SmbBrowser.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/data/scanner/SmbBrowser.kt)
- Implement `listFolders(server: String, share: String, path: String, authContext: AuthenticationContext): Result<List<SmbFileItem>>`.
- This method will list only directories within a given SMB share and path, returning them as a list of `SmbFileItem`.

### UI Sources Component

#### [MODIFY] [SourceEditorViewModel.kt](file:///C:/Users/mcj13/AndroidStudioProjects/RandPlayer/app/src/main/java/com/example/randplayer/ui/sources/SourceEditorViewModel.kt)
- If the type inference error persists on the `.fold()` call after adding the missing method, I will explicitly specify the type parameters or switch to the more idiomatic `onSuccess` and `onFailure` extension functions.

## Verification Plan

### Automated Tests
- Execute `./gradlew :app:compileDebugKotlin` to ensure the project builds successfully.
