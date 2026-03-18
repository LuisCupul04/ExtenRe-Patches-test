/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.utils.flyoutmenu

import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import com.extenre.patches.youtube.utils.playbackRateBottomSheetBuilderFingerprint
import com.extenre.patches.youtube.utils.resourceid.sharedResourceIdPatch
import com.extenre.util.addStaticFieldToExtension
import com.extenre.util.fingerprint.mutableMethodOrThrow

private const val EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/utils/VideoUtils;"

val flyoutMenuHookPatch = bytecodePatch(
    name = "flyout-Menu-Hook-Patch",
    description = "flyoutMenuHookPatch"
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(sharedResourceIdPatch)

    execute {
        playbackRateBottomSheetBuilderFingerprint.mutableMethodOrThrow().apply {
            val smaliInstructions =
                """
                    if-eqz v0, :ignore
                    invoke-virtual {v0}, $definingClass->$name()V
                    :ignore
                    return-void
                    """

            addStaticFieldToExtension(
                EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR,
                "showYouTubeLegacyPlaybackSpeedFlyoutMenu",
                "playbackRateBottomSheetClass",
                definingClass,
                smaliInstructions
            )
        }

        videoQualityBottomSheetClassFingerprint.mutableMethodOrThrow().apply {
            val smaliInstructions =
                """
                    if-eqz v0, :ignore
                    const/4 v1, 0x1
                    invoke-virtual {v0, v1}, $definingClass->$name(Z)V
                    :ignore
                    return-void
                    """

            addStaticFieldToExtension(
                EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR,
                "showYouTubeLegacyVideoQualityFlyoutMenu",
                "videoQualityBottomSheetClass",
                definingClass,
                smaliInstructions
            )
        }
    }
}
