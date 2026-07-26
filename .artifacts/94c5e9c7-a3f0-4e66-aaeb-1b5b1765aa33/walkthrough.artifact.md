# Walkthrough - Spotify Style Seek Bar

I have implemented a new "Spotify style" seek bar for the player screen. This style features a thin track that thickens when interacted with, and a thumb that is only visible during dragging or pressing.

## Changes Made

### Constants
- Added `SPOTIFY` to the `SliderStyle` enum in [PreferenceKeys.kt](file:///D:/flux%20copy/Metrolist-main/app/src/main/kotlin/com/metrolist/music/constants/PreferenceKeys.kt).

### UI Components
- Created `SpotifySlider` in [PlayerSlider.kt](file:///D:/flux%20copy/Metrolist-main/app/src/main/kotlin/com/metrolist/music/ui/component/PlayerSlider.kt). This component uses `MutableInteractionSource` to detect when the user is pressing or dragging the slider, animating the track height and thumb size accordingly.

### Resources
- Added the "Spotify" string to [metrolist_strings.xml](file:///D:/flux%20copy/Metrolist-main/app/src/main/res/values/metrolist_strings.xml).

### Player Screen
- Integrated the new `SpotifySlider` into the player screen in [Player.kt](file:///D:/flux%20copy/Metrolist-main/app/src/main/kotlin/com/metrolist/music/ui/player/Player.kt).

### Settings
- Added the "Spotify" slider style as an option in the Appearance settings in [AppearanceSettings.kt](file:///D:/flux%20copy/Metrolist-main/app/src/main/kotlin/com/metrolist/music/ui/screens/settings/AppearanceSettings.kt).
- Updated all `when` expressions to handle the new `SPOTIFY` style.

## Verification Results

### Automated Tests
- Ran `./gradlew :app:assembleFossDebug` and the build finished successfully.

### Manual Verification
- You can now go to **Settings > Appearance > Slider Style** and select **Spotify**.
- In the player screen, the seek bar will now behave like Spotify's:
    - Thin track and no thumb when idle.
    - Thick track and visible thumb when dragging.
