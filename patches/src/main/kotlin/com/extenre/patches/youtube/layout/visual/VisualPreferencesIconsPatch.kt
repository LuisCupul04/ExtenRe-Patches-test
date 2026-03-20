/*
 * Copyright (C) 2026 LuisCupul04
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2022 ReVanced LLC
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.layout.visual

import com.extenre.patcher.patch.booleanOption
import com.extenre.patcher.patch.resourcePatch
import com.extenre.patcher.patch.stringOption
import com.extenre.patches.youtube.layout.branding.icon.customBrandingIconPatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.patch.PatchList.VISUAL_PREFERENCES_ICONS_FOR_YOUTUBE
import com.extenre.patches.youtube.utils.settings.ResourceUtils.YOUTUBE_SETTINGS_PATH
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.ResourceGroup
import com.extenre.util.Utils.trimIndentMultiline
import com.extenre.util.copyResources
import com.extenre.util.doRecursively
import com.extenre.util.getStringOptionValue
import com.extenre.util.underBarOrThrow
import org.w3c.dom.Element

private const val DEFAULT_ICON = "extension"
private const val EMPTY_ICON = "empty_icon"
private const val EXTENRE_PREFERENCE_PATH = "res/xml/extenre_prefs.xml" // Definida localmente

@Suppress("unused")
val visualPreferencesIconsPatch = resourcePatch(
    name = VISUAL_PREFERENCES_ICONS_FOR_YOUTUBE.key,
    description = "${VISUAL_PREFERENCES_ICONS_FOR_YOUTUBE.title}: ${VISUAL_PREFERENCES_ICONS_FOR_YOUTUBE.summary}",
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

    val applyToAll by booleanOption(
        key = "applyToAll",
        default = false,
        title = "Apply to all settings menu",
        description = """
            Whether to apply Visual preferences icons to all settings menus.

            If true: icons are applied to the parent PreferenceScreen of YouTube settings, the parent PreferenceScreen of ExtenRe settings and the ExtenRe sub-settings (if supported).

            If false: icons are applied only to the parent PreferenceScreen of YouTube settings and ExtenRe settings.
            """.trimIndentMultiline(),
        required = true
    )

    lateinit var preferenceIcon: Map<String, String>

    fun Set<String>.setPreferenceIcon() = associateWith { title ->
        when (title) {
            // Internal ExtenRe settings
            "extenre_alt_thumbnail_home" -> "extenre_hide_navigation_home_button_icon"
            "extenre_alt_thumbnail_library" -> "extenre_preference_screen_video_icon"
            "extenre_alt_thumbnail_player" -> "extenre_preference_screen_player_icon"
            "extenre_alt_thumbnail_search" -> "extenre_hide_shorts_shelf_search_icon"
            "extenre_alt_thumbnail_subscriptions" -> "extenre_hide_navigation_subscriptions_button_icon"
            "extenre_change_share_sheet" -> "extenre_hide_share_button_icon"
            "extenre_change_shorts_background_repeat_state" -> "extenre_change_shorts_repeat_state_icon"
            "extenre_default_app_settings" -> "extenre_preference_screen_settings_menu_icon"
            "extenre_disable_shorts_background_playback" -> "offline_key_icon"
            "extenre_settings_search_history" -> "history_key_icon"
            "extenre_hide_download_button" -> "extenre_overlay_button_external_downloader_icon"
            "extenre_hide_keyword_content_home" -> "extenre_hide_navigation_home_button_icon"
            "extenre_hide_keyword_content_search" -> "extenre_hide_shorts_shelf_search_icon"
            "extenre_hide_keyword_content_subscriptions" -> "extenre_hide_navigation_subscriptions_button_icon"
            "extenre_hide_like_dislike_button" -> "sb_voting_button_icon"
            "extenre_hide_navigation_label" -> "extenre_swipe_text_overlay_size_icon"
            "extenre_hide_navigation_library_button" -> "extenre_preference_screen_video_icon"
            "extenre_hide_navigation_notifications_button" -> "notification_key_icon"
            "extenre_hide_navigation_shorts_button" -> "extenre_preference_screen_shorts_icon"
            "extenre_hide_player_autoplay_button" -> "extenre_change_player_flyout_menu_toggle_icon"
            "extenre_hide_player_captions_button" -> "captions_key_icon"
            "extenre_hide_player_flyout_menu_ambient_mode" -> "extenre_preference_screen_ambient_mode_icon"
            "extenre_hide_player_flyout_menu_captions" -> "captions_key_icon"
            "extenre_hide_player_flyout_menu_enhanced_bitrate" -> "video_quality_settings_key_icon"
            "extenre_hide_player_flyout_menu_listen_with_youtube_music" -> "extenre_hide_player_youtube_music_button_icon"
            "extenre_hide_player_flyout_menu_loop_video" -> "extenre_overlay_button_always_repeat_icon"
            "extenre_hide_player_flyout_menu_more_info" -> "about_key_icon"
            "extenre_hide_player_flyout_menu_pip" -> "offline_key_icon"
            "extenre_hide_player_flyout_menu_premium_controls" -> "premium_early_access_browse_page_key_icon"
            "extenre_hide_player_flyout_menu_report" -> "extenre_hide_report_button_icon"
            "extenre_hide_player_fullscreen_button" -> "extenre_preference_screen_fullscreen_icon"
            "extenre_hide_shorts_shelf" -> "extenre_preference_screen_shorts_icon"
            "extenre_hide_shorts_shelf_channel" -> "account_switcher_key_icon"
            "extenre_hide_shorts_shelf_home_related_videos" -> "extenre_hide_navigation_home_button_icon"
            "extenre_hide_shorts_shelf_subscriptions" -> "extenre_hide_navigation_subscriptions_button_icon"
            "extenre_open_shorts_in_regular_player" -> "extenre_preference_screen_player_icon"
            "extenre_preference_screen_account_menu" -> "account_switcher_key_icon"
            "extenre_preference_screen_channel_bar" -> "account_switcher_key_icon"
            "extenre_preference_screen_channel_page" -> "account_switcher_key_icon"
            "extenre_preference_screen_feed_flyout_menu" -> "extenre_preference_screen_player_flyout_menu_icon"
            "extenre_preference_screen_general" -> "general_key_icon"
            "extenre_preference_screen_haptic_feedback" -> "extenre_swipe_haptic_feedback_icon"
            "extenre_preference_screen_hook_buttons" -> "extenre_preference_screen_import_export_icon"
            "extenre_preference_screen_miniplayer" -> "offline_key_icon"
            "extenre_preference_screen_patch_information" -> "about_key_icon"
            "extenre_preference_screen_sb" -> "sb_create_new_segment_icon"
            "extenre_preference_screen_shorts_player" -> "extenre_preference_screen_shorts_icon"
            "extenre_preference_screen_snack_bar" -> "extenre_preference_screen_action_buttons_icon"
            "extenre_preference_screen_video_filter" -> "extenre_preference_screen_video_icon"
            "extenre_preference_screen_watch_history" -> "history_key_icon"
            "extenre_swipe_gestures_lock_mode" -> "extenre_hide_player_flyout_menu_lock_screen_icon"
            "extenre_swipe_overlay_progress_brightness_color" -> "extenre_swipe_brightness_icon"
            "extenre_swipe_overlay_progress_volume_color" -> "extenre_swipe_volume_icon"
            else -> "${title}_icon"
        }
    }

    execute {
        // Check patch options first.
        val selectedIconType = settingsMenuIconOption
            .underBarOrThrow()

        val appIconOption = customBrandingIconPatch
            .getStringOptionValue("appIcon")

        val customBrandingIconType = appIconOption
            .underBarOrThrow()

        val allPreferenceKeys = mutableSetOf<String>().apply {
            addAll(preferenceKey)
            if (applyToAll == true) {
                addAll(extenrePreferenceKey)
            }
        }

        preferenceIcon = allPreferenceKeys.setPreferenceIcon()

        // region copy shared resources.

        arrayOf(
            ResourceGroup(
                "drawable",
                *preferenceIcon.values.map { "$it.xml" }.toTypedArray()
            ),
            ResourceGroup(
                "drawable-xxhdpi",
                "$EMPTY_ICON.png"
            ),
        ).forEach { resourceGroup ->
            copyResources("youtube/visual/shared", resourceGroup)
        }

        // endregion.

        // region copy ExtenRe settings menu icon.

        val fallbackIconPath = "youtube/visual/icons/extension"
        val iconPath = when (selectedIconType) {
            "custom_branding_icon" -> "youtube/branding/$customBrandingIconType/settings"
            else -> "youtube/visual/icons/$selectedIconType"
        }
        val resourceGroup = ResourceGroup(
            "drawable",
            "extenre_settings_key_icon.xml"
        )

        try {
            copyResources(iconPath, resourceGroup)
        } catch (_: Exception) {
            // Ignore if resource copy fails

            // Add a fallback extended icon
            // It's needed if someone provides custom path to icon(s) folder
            // but custom branding icons for ExtenRe setting are predefined,
            // so it won't copy custom branding icon
            // and will raise an error without fallback icon
            copyResources(fallbackIconPath, resourceGroup)
        }

        // endregion.

        addPreference(VISUAL_PREFERENCES_ICONS_FOR_YOUTUBE)
    }

    finalize {
        // region set visual preferences icon.

        arrayOf(
            EXTENRE_PREFERENCE_PATH,
            YOUTUBE_SETTINGS_PATH
        ).forEach { xmlFile ->
            document(xmlFile).use { document ->
                document.doRecursively loop@{ node ->
                    if (node !is Element) return@loop

                    node.getAttributeNode("android:key")
                        ?.textContent
                        ?.removePrefix("@string/")
                        ?.let { title ->
                            val drawableName = when (title) {
                                in preferenceKey -> preferenceIcon[title]
                                in extenrePreferenceKey -> preferenceIcon[title]
                                in intentKey -> intentIcon[title]
                                in emptyTitles -> EMPTY_ICON
                                else -> null
                            }
                            if (drawableName == EMPTY_ICON &&
                                applyToAll == false
                            ) return@loop

                            drawableName?.let {
                                node.setAttribute("android:icon", "@drawable/$it")
                            }
                        }
                }

                // overlay buttons empty icon
                if (applyToAll == true) {
                    val tags = document.getElementsByTagName("PreferenceScreen")
                    List(tags.length) { tags.item(it) as Element }
                        .find { it.getAttribute("android:key") == "extenre_preference_screen_player_buttons" }
                        ?.let { pref ->
                            val childNodes = pref.childNodes
                            for (i in 0 until childNodes.length) {
                                val node = childNodes.item(i) as? Element
                                node?.getAttributeNode("android:key")
                                    ?.textContent
                                    ?.let { key ->
                                        if (emptyTitlesOverlayButtons.contains(key)) {
                                            node.setAttribute(
                                                "android:icon",
                                                "@drawable/$EMPTY_ICON"
                                            )
                                        }
                                    }
                            }
                        }
                }
            }
        }

        // endregion.
    }
}

private val preferenceKey = setOf(
    // YouTube settings.
    "about_key",
    "accessibility_settings_key",
    "account_switcher_key",
    "auto_play_key",
    "billing_and_payment_key",
    "captions_key",
    "connected_accounts_browse_page_key",
    "data_saving_settings_key",
    "general_key",
    "history_key",
    "live_chat_key",
    "notification_key",
    "offline_key",
    "pair_with_tv_key",
    "parent_tools_key",
    "playback_key",
    "premium_early_access_browse_page_key",
    "privacy_key",
    "subscription_product_setting_key",
    "video_quality_settings_key",
    "your_data_key",

    // ExtenRe settings (main screens)
    "extenre_preference_screen_ads",
    "extenre_preference_screen_alt_thumbnails",
    "extenre_preference_screen_feed",
    "extenre_preference_screen_general",
    "extenre_preference_screen_player",
    "extenre_preference_screen_shorts",
    "extenre_preference_screen_swipe_controls",
    "extenre_preference_screen_video",
    "extenre_preference_screen_ryd",
    "extenre_preference_screen_return_youtube_username",
    "extenre_preference_screen_sb",
    "extenre_preference_screen_misc",
)

private val extenrePreferenceKey = setOf(
    // Internal ExtenRe settings (items without prefix are listed first, others are sorted alphabetically)
    "gms_core_settings",
    "sb_create_new_segment",
    "sb_voting_button",

    "extenre_alt_thumbnail_home",
    "extenre_alt_thumbnail_library",
    "extenre_alt_thumbnail_player",
    "extenre_alt_thumbnail_search",
    "extenre_alt_thumbnail_subscriptions",
    "extenre_bypass_url_redirects",
    "extenre_change_player_flyout_menu_toggle",
    "extenre_change_share_sheet",
    "extenre_change_shorts_background_repeat_state",
    "extenre_change_shorts_repeat_state",
    "extenre_default_app_settings",
    "extenre_disable_hdr_auto_brightness",
    "extenre_disable_quic_protocol",
    "extenre_disable_resuming_shorts_player",
    "extenre_disable_shorts_background_playback",
    "extenre_disable_swipe_to_switch_video",
    "extenre_settings_search_history",
    "extenre_hide_ask_button",
    "extenre_hide_clip_button",
    "extenre_hide_download_button",
    "extenre_hide_hype_button",
    "extenre_hide_keyword_content_comments",
    "extenre_hide_keyword_content_home",
    "extenre_hide_keyword_content_search",
    "extenre_hide_keyword_content_subscriptions",
    "extenre_hide_like_dislike_button",
    "extenre_hide_navigation_create_button",
    "extenre_hide_navigation_home_button",
    "extenre_hide_navigation_label",
    "extenre_hide_navigation_library_button",
    "extenre_hide_navigation_notifications_button",
    "extenre_hide_navigation_shorts_button",
    "extenre_hide_navigation_subscriptions_button",
    "extenre_hide_player_autoplay_button",
    "extenre_hide_player_captions_button",
    "extenre_hide_player_cast_button",
    "extenre_hide_player_collapse_button",
    "extenre_hide_player_flyout_menu_ambient_mode",
    "extenre_hide_player_flyout_menu_audio_track",
    "extenre_hide_player_flyout_menu_captions",
    "extenre_hide_player_flyout_menu_enhanced_bitrate",
    "extenre_hide_player_flyout_menu_help",
    "extenre_hide_player_flyout_menu_listen_with_youtube_music",
    "extenre_hide_player_flyout_menu_lock_screen",
    "extenre_hide_player_flyout_menu_loop_video",
    "extenre_hide_player_flyout_menu_more_info",
    "extenre_hide_player_flyout_menu_pip",
    "extenre_hide_player_flyout_menu_playback_speed",
    "extenre_hide_player_flyout_menu_premium_controls",
    "extenre_hide_player_flyout_menu_quality_header",
    "extenre_hide_player_flyout_menu_report",
    "extenre_hide_player_flyout_menu_sleep_timer",
    "extenre_hide_player_flyout_menu_stable_volume",
    "extenre_hide_player_flyout_menu_stats_for_nerds",
    "extenre_hide_player_flyout_menu_watch_in_vr",
    "extenre_hide_player_fullscreen_button",
    "extenre_hide_player_previous_next_button",
    "extenre_hide_player_youtube_music_button",
    "extenre_hide_playlist_button",
    "extenre_hide_remix_button",
    "extenre_hide_report_button",
    "extenre_hide_rewards_button",
    "extenre_hide_share_button",
    "extenre_hide_shop_button",
    "extenre_hide_shorts_floating_button",
    "extenre_hide_shorts_shelf",
    "extenre_hide_shorts_shelf_channel",
    "extenre_hide_shorts_shelf_history",
    "extenre_hide_shorts_shelf_home_related_videos",
    "extenre_hide_shorts_shelf_search",
    "extenre_hide_shorts_shelf_subscriptions",
    "extenre_hide_thanks_button",
    "extenre_language",
    "extenre_open_links_externally",
    "extenre_open_shorts_in_regular_player",
    "extenre_overlay_button_always_repeat",
    "extenre_overlay_button_copy_video_url",
    "extenre_overlay_button_copy_video_url_timestamp",
    "extenre_overlay_button_external_downloader",
    "extenre_overlay_button_mute_volume",
    "extenre_overlay_button_play_all",
    "extenre_overlay_button_speed_dialog",
    "extenre_overlay_button_whitelist",
    "extenre_preference_screen_account_menu",
    "extenre_preference_screen_action_buttons",
    "extenre_preference_screen_ambient_mode",
    "extenre_preference_screen_carousel_shelf",
    "extenre_preference_screen_category_bar",
    "extenre_preference_screen_channel_bar",
    "extenre_preference_screen_channel_page",
    "extenre_preference_screen_comments",
    "extenre_preference_screen_community_posts",
    "extenre_preference_screen_custom_filter",
    "extenre_preference_screen_debugging",
    "extenre_preference_screen_feed_flyout_menu",
    "extenre_preference_screen_fullscreen",
    "extenre_preference_screen_haptic_feedback",
    "extenre_preference_screen_hook_buttons",
    "extenre_preference_screen_import_export",
    "extenre_preference_screen_miniplayer",
    "extenre_preference_screen_navigation_bar",
    "extenre_preference_screen_patch_information",
    "extenre_preference_screen_player_buttons",
    "extenre_preference_screen_player_flyout_menu",
    "extenre_preference_screen_seekbar",
    "extenre_preference_screen_settings_menu",
    "extenre_preference_screen_shorts_player",
    "extenre_preference_screen_snack_bar",
    "extenre_preference_screen_spoof_streaming_data",
    "extenre_preference_screen_toolbar",
    "extenre_preference_screen_video_description",
    "extenre_preference_screen_video_filter",
    "extenre_preference_screen_watch_history",
    "extenre_sanitize_sharing_links",
    "extenre_swipe_brightness",
    "extenre_swipe_gestures_lock_mode",
    "extenre_swipe_haptic_feedback",
    "extenre_swipe_lowest_value_enable_auto_brightness",
    "extenre_swipe_overlay_background_opacity",
    "extenre_swipe_overlay_progress_brightness_color",
    "extenre_swipe_overlay_progress_volume_color",
    "extenre_swipe_overlay_rect_size",
    "extenre_swipe_overlay_style",
    "extenre_swipe_overlay_timeout",
    "extenre_swipe_press_to_engage",
    "extenre_swipe_save_and_restore_brightness",
    "extenre_swipe_text_overlay_size",
    "extenre_swipe_threshold",
    "extenre_swipe_volume",
    "extenre_switch_create_with_notifications_button",
)

private val intentKey = setOf(
    "extenre_settings_key",
)

val intentIcon = intentKey.associateWith { "${it}_icon" }

private val emptyTitles = setOf(
    "extenre_disable_like_dislike_glow",
    "extenre_disable_swipe_to_enter_fullscreen_mode_below_the_player",
    "extenre_disable_swipe_to_enter_fullscreen_mode_in_the_player",
    "extenre_disable_swipe_to_exit_fullscreen_mode",
    "extenre_enable_narrow_navigation_buttons",
    "extenre_fix_swipe_tap_and_hold_speed",
    "extenre_hide_player_flyout_menu_captions_footer",
    "extenre_hide_player_flyout_menu_quality_footer",
    "extenre_hide_stop_ads_button",
    "extenre_swipe_brightness_distance_dip",
    "extenre_swipe_volumes_sensitivity",
)

private val emptyTitlesOverlayButtons = setOf(
    "extenre_overlay_button_external_downloader_queue_manager",
    "extenre_external_downloader_package_name_video",
    "extenre_overlay_button_speed_dialog_type",
    "extenre_overlay_button_play_all_type",
    "extenre_whitelist_settings",
)
