/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.utils.patch

internal enum class PatchList(
    val key: String,
    val title: String,
    val summary: String,
    var included: Boolean? = false
) {
    ALTERNATIVE_THUMBNAILS(
        "alternative-thumbnails",
        "Alternative thumbnails",
        "Adds options to replace video thumbnails using the DeArrow API or image captures from the video."
    ),
    AMBIENT_MODE_CONTROL(
        "ambient-mode-control",
        "Ambient mode control",
        "Adds options to disable Ambient mode and to bypass Ambient mode restrictions."
    ),
    BYPASS_IMAGE_REGION_RESTRICTIONS(
        "bypass-image-region-restrictions",
        "Bypass image region restrictions",
        "Adds an option to use a different host for static images, so that images blocked in some countries can be received."
    ),
    BYPASS_URL_REDIRECTS(
        "bypass-url-redirects",
        "Bypass URL redirects",
        "Adds an option to bypass URL redirects and open the original URL directly."
    ),
    CHANGE_FORM_FACTOR(
        "change-form-factor",
        "Change form factor",
        "Adds an option to change the UI appearance to a phone, tablet, or automotive device."
    ),
    CHANGE_LIVE_RING_CLICK_ACTION(
        "change-live-ring-click-action",
        "Change live ring click action",
        "Adds an option to open the channel instead of the live stream when clicking on the live ring."
    ),
    CHANGE_PLAYER_FLYOUT_MENU_TOGGLES(
        "change-player-flyout-menu-toggles",
        "Change player flyout menu toggles",
        "Adds an option to use text toggles instead of switch toggles within the additional settings menu."
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
    CUSTOM_SHORTS_ACTION_BUTTONS(
        "custom-shorts-action-buttons",
        "Custom Shorts action buttons",
        "Changes, at compile time, the icon of the action buttons of the Shorts player."
    ),
    CUSTOM_BRANDING_ICON_FOR_YOUTUBE(
        "custom-branding-icon-for-youtube",
        "Custom branding icon for YouTube",
        "Changes the YouTube app icon to the icon specified in patch options."
    ),
    CUSTOM_BRANDING_NAME_FOR_YOUTUBE(
        "custom-branding-name-for-youtube",
        "Custom branding name for YouTube",
        "Changes the YouTube app name to the name specified in patch options."
    ),
    CUSTOM_DOUBLE_TAP_LENGTH(
        "custom-double-tap-length",
        "Custom double tap length",
        "Adds Double-tap to seek values that are specified in patch options."
    ),
    CUSTOM_HEADER_FOR_YOUTUBE(
        "custom-header-for-youtube",
        "Custom header for YouTube",
        "Applies a custom header in the top left corner within the app."
    ),
    DESCRIPTION_COMPONENTS(
        "description-components",
        "Description components",
        "Adds options to hide and disable description components."
    ),
    DISABLE_QUIC_PROTOCOL(
        "disable-quic-protocol",
        "Disable QUIC protocol",
        "Adds an option to disable CronetEngine's QUIC protocol."
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
    DISABLE_HAPTIC_FEEDBACK(
        "disable-haptic-feedback",
        "Disable haptic feedback",
        "Adds options to disable haptic feedback when swiping in the video player."
    ),
    DISABLE_LAYOUT_UPDATES(
        "disable-layout-updates",
        "Disable layout updates",
        "Adds an option to disable layout updates by server."
    ),
    DISABLE_RESUMING_MINIPLAYER_ON_STARTUP(
        "disable-resuming-miniplayer-on-startup",
        "Disable resuming Miniplayer on startup",
        "Adds an option to disable the Miniplayer 'Continue watching' from resuming on app startup."
    ),
    DISABLE_RESUMING_SHORTS_ON_STARTUP(
        "disable-resuming-shorts-on-startup",
        "Disable resuming Shorts on startup",
        "Adds an option to disable the Shorts player from resuming on app startup when Shorts were last being watched."
    ),
    DISABLE_SIGN_IN_TO_TV_POPUP(
        "disable-sign-in-to-tv-popup",
        "Disable sign in to TV popup",
        "Adds an option to disable the popup asking to sign into a TV on the same local network."
    ),
    DISABLE_SPLASH_ANIMATION(
        "disable-splash-animation",
        "Disable splash animation",
        "Adds an option to disable the splash animation on app startup."
    ),
    ENABLE_DEBUG_LOGGING(
        "enable-debug-logging",
        "Enable debug logging",
        "Adds an option for debugging and exporting RVX logs to the clipboard."
    ),
    ENABLE_GRADIENT_LOADING_SCREEN(
        "enable-gradient-loading-screen",
        "Enable gradient loading screen",
        "Adds an option to enable the gradient loading screen."
    ),
    FORCE_HIDE_PLAYER_BUTTONS_BACKGROUND(
        "force-hide-player-buttons-background",
        "Force hide player buttons background",
        "Removes, at compile time, the dark background surrounding the video player controls."
    ),
    FULLSCREEN_COMPONENTS(
        "fullscreen-components",
        "Fullscreen components",
        "Adds options to hide or change components related to fullscreen."
    ),
    GMSCORE_SUPPORT(
        "gmscore-support",
        "GmsCore support",
        "Allows patched Google apps to run without root and under a different package name by using GmsCore instead of Google Play Services."
    ),
    HIDE_ACCESSIBILITY_CONTROLS_DIALOG(
        "hide-accessibility-controls-dialog",
        "Hide accessibility controls dialog",
        "Removes, at compile time, accessibility controls dialog 'Turn on accessibility controls for the video player?'."
    ),
    HIDE_SHORTS_DIMMING(
        "hide-shorts-dimming",
        "Hide Shorts dimming",
        "Removes, at compile time, the dimming effect at the top and bottom of Shorts videos."
    ),
    HIDE_ACTION_BUTTONS(
        "hide-action-buttons",
        "Hide action buttons",
        "Adds options to hide action buttons under videos."
    ),
    HIDE_ADS(
        "hide-ads",
        "Hide ads",
        "Adds options to hide ads."
    ),
    HIDE_COMMENTS_COMPONENTS(
        "hide-comments-components",
        "Hide comments components",
        "Adds options to hide components related to comments."
    ),
    HIDE_FEED_COMPONENTS(
        "hide-feed-components",
        "Hide feed components",
        "Adds options to hide components related to feeds."
    ),
    HIDE_FEED_FLYOUT_MENU(
        "hide-feed-flyout-menu",
        "Hide feed flyout menu",
        "Adds the ability to hide feed flyout menu components using a custom filter."
    ),
    HIDE_LAYOUT_COMPONENTS(
        "hide-layout-components",
        "Hide layout components",
        "Adds options to hide general layout components."
    ),
    HIDE_PLAYER_BUTTONS(
        "hide-player-buttons",
        "Hide player buttons",
        "Adds options to hide buttons in the video player."
    ),
    HIDE_PLAYER_FLYOUT_MENU(
        "hide-player-flyout-menu",
        "Hide player flyout menu",
        "Adds options to hide player flyout menu components."
    ),
    HIDE_SHORTCUTS(
        "hide-shortcuts",
        "Hide shortcuts",
        "Remove, at compile time, the app shortcuts that appears when the app icon is long pressed."
    ),
    HOOK_YOUTUBE_MUSIC_ACTIONS(
        "hook-youtube-music-actions",
        "Hook YouTube Music actions",
        "Adds support for opening music in RVX Music using the in-app YouTube Music button."
    ),
    HOOK_DOWNLOAD_ACTIONS(
        "hook-download-actions",
        "Hook download actions",
        "Adds support to download videos with an external downloader app using the in-app download button."
    ),
    MATERIALYOU(
        "materialyou",
        "MaterialYou",
        "Applies the MaterialYou theme for Android 12+ devices."
    ),
    MINIPLAYER(
        "miniplayer",
        "Miniplayer",
        "Adds options to change the in-app minimized player, and if patching target 19.16+ adds options to use modern miniplayers."
    ),
    NAVIGATION_BAR_COMPONENTS(
        "navigation-bar-components",
        "Navigation bar components",
        "Adds options to hide or change components related to the navigation bar."
    ),
    OPEN_LINKS_EXTERNALLY(
        "open-links-externally",
        "Open links externally",
        "Adds an option to always open links in your browser instead of the in-app browser."
    ),
    OVERLAY_BUTTONS(
        "overlay-buttons",
        "Overlay buttons",
        "Adds options to display useful overlay buttons in the video player."
    ),
    PLAYER_COMPONENTS(
        "player-components",
        "Player components",
        "Adds options to hide or change components related to the video player."
    ),
    REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS(
        "remove-background-playback-restrictions",
        "Remove background playback restrictions",
        "Removes restrictions on background playback, including for music and kids videos."
    ),
    REMOVE_VIEWER_DISCRETION_DIALOG(
        "remove-viewer-discretion-dialog",
        "Remove viewer discretion dialog",
        "Adds an option to remove the dialog that appears when opening a video that has been age-restricted by accepting it automatically. This does not bypass the age restriction."
    ),
    RETURN_YOUTUBE_DISLIKE(
        "return-youtube-dislike",
        "Return YouTube Dislike",
        "Adds an option to show the dislike count of videos using the Return YouTube Dislike API."
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
    SEEKBAR_COMPONENTS(
        "seekbar-components",
        "Seekbar components",
        "Adds options to hide or change components related to the seekbar."
    ),
    SET_TRANSCRIPT_COOKIES(
        "set-transcript-cookies",
        "Set Transcript Cookies",
        "Adds an option to set Cookies in YouTube Transcript API requests."
    ),
    SETTINGS_FOR_YOUTUBE(
        "settings-for-youtube",
        "Settings for YouTube",
        "Applies mandatory patches to implement ExtenRe settings into the application."
    ),
    SHORTS_COMPONENTS(
        "shorts-components",
        "Shorts components",
        "Adds options to hide or change components related to YouTube Shorts."
    ),
    SNACK_BAR_COMPONENTS(
        "snack-bar-components",
        "Snack bar components",
        "Adds options to hide or change components related to the snack bar."
    ),
    SPONSORBLOCK(
        "sponsorblock",
        "SponsorBlock",
        "Adds options to enable and configure SponsorBlock, which can skip undesired video segments, such as sponsored content."
    ),
    SPOOF_APP_VERSION(
        "spoof-app-version",
        "Spoof app version",
        "Adds options to spoof the YouTube client version. This can be used to restore old UI elements and features."
    ),
    SWIPE_CONTROLS(
        "swipe-controls",
        "Swipe controls",
        "Adds options for controlling volume and brightness with swiping, and whether to enter fullscreen when swiping down below the player."
    ),
    THEME(
        "theme",
        "Theme",
        "Changes the app's themes to the values specified in patch options."
    ),
    TOOLBAR_COMPONENTS(
        "toolbar-components",
        "Toolbar components",
        "Adds options to hide or change components located on the toolbar, such as the search bar, header, and toolbar buttons."
    ),
    TRANSLATIONS_FOR_YOUTUBE(
        "translations-for-youtube",
        "Translations for YouTube",
        "Add translations or remove string resources."
    ),
    VIDEO_PLAYBACK(
        "video-playback",
        "Video playback",
        "Adds options to customize settings related to video playback, such as default video quality and playback speed."
    ),
    VISUAL_PREFERENCES_ICONS_FOR_YOUTUBE(
        "visual-preferences-icons-for-youtube",
        "Visual preferences icons for YouTube",
        "Adds icons to specific preferences in the settings."
    ),
    WATCH_HISTORY(
        "watch-history",
        "Watch history",
        "Adds an option to change the domain of the watch history or check its status."
    )
}
