/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.utils.patch

internal enum class PatchList(
    val key: String,
    val title: String,
    val summary: String,
    var included: Boolean? = false
) {
    BITRATE_DEFAULT_VALUE(
        "bitrate-default-value",
        "Bitrate default value",
        "Sets the audio quality to 'Always High' when you first install the app."
    ),
    BYPASS_IMAGE_REGION_RESTRICTIONS(
        "bypass-image-region-restrictions",
        "Bypass image region restrictions",
        "Adds an option to use a different host for static images, so that images blocked in some countries can be received."
    ),
    CERTIFICATE_SPOOF(
        "certificate-spoof",
        "Certificate spoof",
        "Enables YouTube Music to work with Android Auto by spoofing the YouTube Music certificate."
    ),
    CHANGE_SHARE_SHEET(
        "change-share-sheet",
        "Change share sheet",
        "Adds an option to change the in-app share sheet to the system share sheet."
    ),
    CHANGE_START_PAGE(
        "change-start-page",
        "Change start page",
        "Adds an option to set which page the app opens in instead of the homepage."
    ),
    CUSTOM_BRANDING_ICON_FOR_YOUTUBE_MUSIC(
        "custom-branding-icon-for-youtube-music",
        "Custom branding icon for YouTube Music",
        "Changes the YouTube Music app icon to the icon specified in patch options."
    ),
    CUSTOM_BRANDING_NAME_FOR_YOUTUBE_MUSIC(
        "custom-branding-name-for-youtube-music",
        "Custom branding name for YouTube Music",
        "Changes the YouTube Music app name to the name specified in patch options."
    ),
    CUSTOM_HEADER_FOR_YOUTUBE_MUSIC(
        "custom-header-for-youtube-music",
        "Custom header for YouTube Music",
        "Applies a custom header in the top left corner within the app."
    ),
    DARK_THEME(
        "dark-theme",
        "Dark theme",
        "Changes the app's dark theme to the values specified in patch options."
    ),
    DISABLE_CAIRO_SPLASH_ANIMATION(
        "disable-cairo-splash-animation",
        "Disable Cairo splash animation",
        "Adds an option to disable Cairo splash animation."
    ),
    DISABLE_FORCED_AUTO_AUDIO_TRACKS(
        "disable-forced-auto-audio-tracks",
        "Disable forced auto audio tracks",
        "Adds an option to disable audio tracks from being automatically enabled."
    ),
    DISABLE_FORCED_AUTO_CAPTIONS(
        "disable-forced-auto-captions",
        "Disable forced auto captions",
        "Adds an option to disable captions from being automatically enabled."
    ),
    DISABLE_DISLIKE_REDIRECTION(
        "disable-dislike-redirection",
        "Disable dislike redirection",
        "Adds an option to disable redirection to the next track when clicking the Dislike button."
    ),
    DISABLE_MUSIC_VIDEO_IN_ALBUM(
        "disable-music-video-in-album",
        "Disable music video in album",
        "Adds option to redirect music videos from albums for non-premium users."
    ),
    DISABLE_QUIC_PROTOCOL(
        "disable-quic-protocol",
        "Disable QUIC protocol",
        "Adds an option to disable CronetEngine's QUIC protocol."
    ),
    ENABLE_DEBUG_LOGGING(
        "enable-debug-logging",
        "Enable debug logging",
        "Adds an option for debugging."
    ),
    ENABLE_LANDSCAPE_MODE(
        "enable-landscape-mode",
        "Enable landscape mode",
        "Adds an option to enable landscape mode when rotating the screen on phones."
    ),
    FLYOUT_MENU_COMPONENTS(
        "flyout-menu-components",
        "Flyout menu components",
        "Adds options to hide or change flyout menu components."
    ),
    GMSCORE_SUPPORT(
        "gmscore-support",
        "GmsCore support",
        "Allows patched Google apps to run without root and under a different package name by using GmsCore instead of Google Play Services."
    ),
    HIDE_ACCOUNT_COMPONENTS(
        "hide-account-components",
        "Hide account components",
        "Adds options to hide components related to the account menu."
    ),
    HIDE_ACTION_BAR_COMPONENTS(
        "hide-action-bar-components",
        "Hide action bar components",
        "Adds options to hide action bar components and replace the offline download button with an external download button."
    ),
    HIDE_ADS(
        "hide-ads",
        "Hide ads",
        "Adds options to hide ads."
    ),
    HIDE_LAYOUT_COMPONENTS(
        "hide-layout-components",
        "Hide layout components",
        "Adds options to hide general layout components."
    ),
    HIDE_OVERLAY_FILTER(
        "hide-overlay-filter",
        "Hide overlay filter",
        "Removes, at compile time, the dark overlay that appears when player flyout menus are open."
    ),
    HIDE_PLAYER_OVERLAY_FILTER(
        "hide-player-overlay-filter",
        "Hide player overlay filter",
        "Removes, at compile time, the dark overlay that appears when single-tapping in the player."
    ),
    NAVIGATION_BAR_COMPONENTS(
        "navigation-bar-components",
        "Navigation bar components",
        "Adds options to hide or change components related to the navigation bar."
    ),
    PLAYER_COMPONENTS(
        "player-components",
        "Player components",
        "Adds options to hide or change components related to the player."
    ),
    REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS(
        "remove-background-playback-restrictions",
        "Remove background playback restrictions",
        "Removes restrictions on background playback, including for kids videos."
    ),
    REMOVE_VIEWER_DISCRETION_DIALOG(
        "remove-viewer-discretion-dialog",
        "Remove viewer discretion dialog",
        "Adds an option to remove the dialog that appears when opening a video that has been age-restricted by accepting it automatically. This does not bypass the age restriction."
    ),
    RESTORE_OLD_STYLE_LIBRARY_SHELF(
        "restore-old-style-library-shelf",
        "Restore old style library shelf",
        "Adds an option to return the Library tab to the old style."
    ),
    RETURN_YOUTUBE_DISLIKE(
        "return-youtube-dislike",
        "Return YouTube Dislike",
        "Adds an option to show the dislike count of songs using the Return YouTube Dislike API."
    ),
    RETURN_YOUTUBE_USERNAME(
        "return-youtube-username",
        "Return YouTube Username",
        "Adds an option to replace YouTube handles with usernames in comments using YouTube Data API v3."
    ),
    SANITIZE_SHARING_LINKS(
        "sanitize-sharing-links",
        "Sanitize sharing links",
        "Adds an option to sanitize sharing links by removing tracking query parameters."
    ),
    SETTINGS_FOR_YOUTUBE_MUSIC(
        "settings-for-youtube-music",
        "Settings for YouTube Music",
        "Applies mandatory patches to implement ExtenRe settings into the application."
    ),
    SPONSORBLOCK(
        "sponsorblock",
        "SponsorBlock",
        "Adds options to enable and configure SponsorBlock, which can skip undesired video segments, such as non-music sections."
    ),
    SPOOF_APP_VERSION(
        "spoof-app-version",
        "Spoof app version",
        "Adds options to spoof the YouTube Music client version. This can be used to restore old UI elements and features."
    ),
    SPOOF_APP_VERSION_FOR_LYRICS(
        "spoof-app-version-for-lyrics",
        "Spoof app version for lyrics",
        "Adds options to spoof the YouTube Music client version. This can be used to restore old lyrics UI."
    ),
    TRANSLATIONS_FOR_YOUTUBE_MUSIC(
        "translations-for-youtube-music",
        "Translations for YouTube Music",
        "Add translations or remove string resources."
    ),
    VIDEO_PLAYBACK(
        "video-playback",
        "Video playback",
        "Adds options to customize settings related to video playback, such as default video quality and playback speed."
    ),
    VISUAL_PREFERENCES_ICONS_FOR_YOUTUBE_MUSIC(
        "visual-preferences-icons-for-youtube-music",
        "Visual preferences icons for YouTube Music",
        "Adds icons to specific preferences in the settings."
    ),
    WATCH_HISTORY(
        "watch-history",
        "Watch history",
        "Adds an option to change the domain of the watch history or check its status."
    )
}
