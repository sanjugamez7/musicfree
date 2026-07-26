# Removed Changelog and Supported Links from Settings

I have removed the "Changelog" and "Open supported links" entries from the Settings screen.

## Changes Made

### [app]

#### [MODIFY] [SettingsScreen.kt](file:///D:/crud/Metrolist-main/app/src/main/kotlin/com/metrolist/music/ui/screens/settings/SettingsScreen.kt)
- Removed the "Open supported links" item from the System & About section.
- Removed the "Changelog" item from the System & About section.
- Cleaned up unused imports and variables (`ActivityNotFoundException`, `isAndroid12OrLater`) that were only used for the removed items.

## Verification Results

### Automated Tests
- Ran `./gradlew :app:assembleFossDebug` and verified that the build finished successfully.

### Manual Verification
- Navigated to the Settings screen in the app.
- Verified that the "System & About" section now only contains "Updater" (if available) and "About".
