/*
 * Copyright (C) 2026 LuisCupul04
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2022 ReVanced LLC
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.layout.visual

import com.extenre.patcher.patch.resourcePatch
import com.extenre.patcher.patch.stringOption
import com.extenre.patches.music.layout.branding.icon.customBrandingIconPatch
import com.extenre.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.music.utils.patch.PatchList.VISUAL_PREFERENCES_ICONS_FOR_YOUTUBE_MUSIC
import com.extenre.patches.music.utils.settings.ResourceUtils.SETTINGS_HEADER_PATH
import com.extenre.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import com.extenre.patches.music.utils.settings.settingsPatch
import com.extenre.util.ResourceGroup
import com.extenre.util.copyResources
import com.extenre.util.doRecursively
import com.extenre.util.getStringOptionValue
import com.extenre.util.underBarOrThrow
import org.w3c.dom.Element

private const val DEFAULT_ICON = "extension"

@Suppress("unused")
val visualPreferencesIconsPatch = resourcePatch(
    name = VISUAL_PREFERENCES_ICONS_FOR_YOUTUBE_MUSIC.key,
    description = "${VISUAL_PREFERENCES_ICONS_FOR_YOUTUBE_MUSIC.title}: ${VISUAL_PREFERENCES_ICONS_FOR_YOUTUBE_MUSIC.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    val settingsMenuIconOption = stringOption(
        key = "settingsMenuIcon",
        default = DEFAULT_ICON,
        values = mapOf(
            "Custom branding icon" to "custom_branding_icon",
            "Extension" to DEFAULT_ICON,
            "Gear" to "gear",
            "ReVanced" to "revanced",
            "ReVanced Colored" to "extenre_colored",
            "RVX Letters" to "rvx_letters",
            "RVX Letters Bold" to "rvx_letters_bold",
            "YT alt" to "yt_alt",
        ),
        title = "ExtenRe settings menu icon",
        description = "The icon for the ExtenRe settings menu.",
        required = true,
    )

    execute {
        // Check patch options first.
        val selectedIconType = settingsMenuIconOption
            .underBarOrThrow()

        val appIconOption = customBrandingIconPatch
            .getStringOptionValue("appIcon")

        val customBrandingIconType = appIconOption
            .underBarOrThrow()

        // region copy shared resources.

        arrayOf(
            ResourceGroup(
                "drawable",
                *preferenceKey.map { it + "_icon.xml" }.toTypedArray()
            ),
        ).forEach { resourceGroup ->
            copyResources("music/visual/shared", resourceGroup)
        }

        // endregion.

        // region copy RVX settings menu icon.

        val iconPath = when (selectedIconType) {
            "custom_branding_icon" -> "music/branding/$customBrandingIconType/settings"
            else -> "music/visual/icons/$selectedIconType"
        }
        val resourceGroup = ResourceGroup(
            "drawable",
            "extenre_settings_icon.xml"
        )

        try {
            copyResources(iconPath, resourceGroup)
        } catch (_: Exception) {
            // Ignore if resource copy fails
        }

        // endregion.

        updatePatchStatus(VISUAL_PREFERENCES_ICONS_FOR_YOUTUBE_MUSIC)

    }

    finalize {
        // region set visual preferences icon.

        document(SETTINGS_HEADER_PATH).use { document ->
            document.doRecursively loop@{ node ->
                if (node !is Element) return@loop

                node.getAttributeNode("android:key")
                    ?.textContent
                    ?.removePrefix("@string/")
                    ?.let { title ->
                        val drawableName = when (title) {
                            in preferenceKey -> title + "_icon"
                            else -> null
                        }

                        drawableName?.let {
                            node.setAttribute("android:icon", "@drawable/$it")
                        }
                    }
            }
        }

        // endregion.
    }
}

// region preference key and icon.

private val preferenceKey = setOf(
    // YouTube settings.
    "pref_key_parent_tools",
    "settings_header_general",
    "settings_header_playback",
    "settings_header_data_saving",
    "settings_header_downloads_and_storage",
    "settings_header_notifications",
    "settings_header_privacy_and_location",
    "settings_header_recommendations",
    "settings_header_paid_memberships",
    "settings_header_about_youtube_music",

    // RVX settings.
    "extenre_settings",

    "extenre_preference_screen_account",
    "extenre_preference_screen_action_bar",
    "extenre_preference_screen_ads",
    "extenre_preference_screen_flyout",
    "extenre_preference_screen_general",
    "extenre_preference_screen_navigation",
    "extenre_preference_screen_player",
    "extenre_preference_screen_settings",
    "extenre_preference_screen_video",
    "extenre_preference_screen_ryd",
    "extenre_preference_screen_return_youtube_username",
    "extenre_preference_screen_sb",
    "extenre_preference_screen_misc",
)

// endregion.
