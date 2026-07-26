# Implementation Plan - Spotify Style Seek Bar Refinement

The user wants to change "only the seek bar" to the Spotify style. This implies making the Spotify-style seek bar the default and potentially removing the other experimental slider styles to focus on a clean, authentic Spotify look.

## User Review Required

> [!IMPORTANT]
> This plan will make the Spotify-style seek bar the **default and only** seek bar in the player, removing the previous "Wavy", "Squiggly", and "Slim" options to satisfy the "change only the seek bar" request.

## Proposed Changes

### [UI Components]

#### [MODIFY] [BigSeekBar.kt](file:///D:/flux%20copy/Metrolist-main/app/src/main/kotlin/com/metrolist/music/ui/component/BigSeekBar.kt)
- Rewrite `BigSeekBar` (renaming it to `SpotifySeekBar`) to feature:
    - A thin track (2dp-4dp) with rounded ends.
    - A visible thumb (circle) only during user interaction.
    - Smooth animations for track thickening and thumb appearance.

### [Player]

#### [MODIFY] [Player.kt](file:///D:/flux%20copy/Metrolist-main/app/src/main/kotlin/com/metrolist/music/ui/player/Player.kt)
- Replace the `when (sliderStyle)` block with a single call to the new `SpotifySeekBar`.
- Clean up unused imports and variables related to other slider styles.

### [Settings]

#### [MODIFY] [AppearanceSettings.kt](file:///D:/flux%20copy/Metrolist-main/app/src/main/kotlin/com/metrolist/music/ui/screens/settings/AppearanceSettings.kt)
- Remove the "Slider Style" setting as it will no longer be configurable.

### [Constants]

#### [DELETE] [PreferenceKeys.kt](file:///D:/flux%20copy/Metrolist-main/app/src/main/kotlin/com/metrolist/music/constants/PreferenceKeys.kt)
- Remove `SliderStyle` enum and `SliderStyleKey`.

## Verification Plan

### Automated Tests
- Build the app using `./gradlew :app:assembleFossDebug` to ensure no compilation errors.

### Manual Verification
1. Open the player screen.
2. Verify the seek bar is thin and clean.
3. Touch/drag the seek bar and verify:
    - A thumb appears.
    - The track slightly thickens.
    - The seeking behavior is smooth.
