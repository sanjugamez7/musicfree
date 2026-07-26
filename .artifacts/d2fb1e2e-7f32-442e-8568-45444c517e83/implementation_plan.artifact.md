# Remove Changelog and Supported Links from Settings

Remove the "Changelog" and "Open supported links" entries from the Settings screen to simplify the UI.

## Proposed Changes

### [app]

#### [MODIFY] [SettingsScreen.kt](file:///D:/crud/Metrolist-main/app/src/main/kotlin/com/metrolist/music/ui/screens/settings/SettingsScreen.kt)
- Remove the code block that adds the "Open supported links" (`default_links`) item to the System & About section.
- Remove the code block that adds the "Changelog" item to the System & About section.
- Remove the unused `showChangelog` state retrieval.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleFossDebug` to ensure no syntax errors.

### Manual Verification
- Deploy the app and navigate to **Settings**.
- Scroll to the **System & About** section.
- Verify that "Open supported links" (if on Android 12+) and "Changelog" are no longer visible.
