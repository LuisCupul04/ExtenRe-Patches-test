/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.reddit.utils.patch

internal enum class PatchList(
    val key: String,
    val title: String,
    val summary: String,
    var included: Boolean? = false
) {
    CHANGE_PACKAGE_NAME(
        "change-package-name",
        "Change package name",
        "Changes the package name for Reddit to the name specified in patch options."
    ),
    CUSTOM_BRANDING_NAME_FOR_REDDIT(
        "custom-branding-name-for-reddit",
        "Custom branding name for Reddit",
        "Changes the Reddit app name to the name specified in patch options."
    ),
    DISABLE_SCREENSHOT_POPUP(
        "disable-screenshot-popup",
        "Disable screenshot popup",
        "Adds an option to disable the popup that appears when taking a screenshot."
    ),
    HIDE_RECENTLY_VISITED_SHELF(
        "hide-recently-visited-shelf",
        "Hide Recently Visited shelf",
        "Adds an option to hide the Recently Visited shelf in the sidebar."
    ),
    HIDE_ADS(
        "hide-ads",
        "Hide ads",
        "Adds options to hide ads."
    ),
    HIDE_NAVIGATION_BUTTONS(
        "hide-navigation-buttons",
        "Hide navigation buttons",
        "Adds options to hide buttons in the navigation bar."
    ),
    HIDE_RECOMMENDED_COMMUNITIES_SHELF(
        "hide-recommended-communities-shelf",
        "Hide recommended communities shelf",
        "Adds an option to hide the recommended communities shelves in subreddits."
    ),
    HIDE_TOOLBAR_BUTTON(
        "hide-toolbar-button",
        "Hide toolbar button",
        "Adds an option to hide the r/place or Reddit recap button in the toolbar."
    ),
    HIDE_TRENDING_TODAY_SHELF(
        "hide-trending-today-shelf",
        "Hide Trending Today shelf",
        "Adds an option to hide the Trending Today shelf from search suggestions."
    ),
    OPEN_LINKS_DIRECTLY(
        "open-links-directly",
        "Open links directly",
        "Adds an option to skip over redirection URLs in external links."
    ),
    OPEN_LINKS_EXTERNALLY(
        "open-links-externally",
        "Open links externally",
        "Adds an option to always open links in your browser instead of in the in-app-browser."
    ),
    PREMIUM_ICON(
        "premium-icon",
        "Premium icon",
        "Unlocks premium app icons."
    ),
    REMOVE_SUBREDDIT_DIALOG(
        "remove-subreddit-dialog",
        "Remove subreddit dialog",
        "Adds options to remove the NSFW community warning and notifications suggestion dialogs by dismissing them automatically."
    ),
    SANITIZE_SHARING_LINKS(
        "sanitize-sharing-links",
        "Sanitize sharing links",
        "Adds an option to sanitize sharing links by removing tracking query parameters."
    ),
    SETTINGS_FOR_REDDIT(
        "settings-for-reddit",
        "Settings for Reddit",
        "Applies mandatory patches to implement ExtenRe settings into the application."
    )
}
