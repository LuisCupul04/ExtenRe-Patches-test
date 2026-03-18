/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.player.action

import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.shared.litho.addLithoFilter
import com.extenre.patches.shared.litho.lithoFilterPatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.componentlist.hookElementList
import com.extenre.patches.youtube.utils.componentlist.lazilyConvertedElementHookPatch
import com.extenre.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import com.extenre.patches.youtube.utils.extension.Constants.PLAYER_PATH
import com.extenre.patches.youtube.utils.fix.hype.hypeButtonIconPatch
import com.extenre.patches.youtube.utils.fix.litho.lithoLayoutPatch
import com.extenre.patches.youtube.utils.patch.PatchList.HIDE_ACTION_BUTTONS
import com.extenre.patches.youtube.utils.request.buildRequestPatch
import com.extenre.patches.youtube.utils.request.hookBuildRequest
import com.extenre.patches.youtube.utils.request.hookInitPlaybackBuildRequest
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.patches.youtube.video.information.videoInformationPatch
import com.extenre.patches.youtube.video.videoid.videoIdPatch

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/ActionButtonsFilter;"
private const val ACTION_BUTTONS_CLASS_DESCRIPTOR =
    "$PLAYER_PATH/ActionButtonsPatch;"

@Suppress("unused")
val actionButtonsPatch = bytecodePatch(
    name = HIDE_ACTION_BUTTONS.key,
    description = "${HIDE_ACTION_BUTTONS.title}: ${HIDE_ACTION_BUTTONS.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        lazilyConvertedElementHookPatch,
        videoInformationPatch,
        videoIdPatch,
        buildRequestPatch,
        hypeButtonIconPatch,
    )

    execute {
        addLithoFilter(FILTER_CLASS_DESCRIPTOR)

        // region patch for hide action buttons by index

        hookBuildRequest("$ACTION_BUTTONS_CLASS_DESCRIPTOR->fetchRequest(Ljava/lang/String;Ljava/util/Map;)V")
        hookElementList("$ACTION_BUTTONS_CLASS_DESCRIPTOR->hideActionButtonByIndex")
        hookInitPlaybackBuildRequest("$ACTION_BUTTONS_CLASS_DESCRIPTOR->fetchRequest(Ljava/lang/String;Ljava/util/Map;)V")

        // endregion

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: HIDE_ACTION_BUTTONS"
            ),
            HIDE_ACTION_BUTTONS
        )

        // endregion

    }
}
