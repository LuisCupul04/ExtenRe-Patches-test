/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.shared.captions

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.smali.ExternalLabel
import com.extenre.patches.shared.extension.Constants.PATCHES_PATH
import com.extenre.patches.shared.startVideoInformerFingerprint
import com.extenre.util.fingerprint.mutableMethodOrThrow

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/AutoCaptionsPatch;"

val baseAutoCaptionsPatch = bytecodePatch(
    name = "base-Auto-Captions-Patch",
    description = "baseAutoCaptionsPatch"
) {
    execute {
        subtitleTrackFingerprint.mutableMethodOrThrow().apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->disableAutoCaptions()Z
                    move-result v0
                    if-eqz v0, :disabled
                    const/4 v0, 0x1
                    return v0
                    """, ExternalLabel("disabled", getInstruction(0))
            )
        }

        mapOf(
            startVideoInformerFingerprint to 0,
            storyboardRendererDecoderRecommendedLevelFingerprint to 1
        ).forEach { (fingerprint, enabled) ->
            fingerprint.mutableMethodOrThrow().addInstructions(
                0, """
                    const/4 v0, 0x$enabled
                    invoke-static {v0}, $EXTENSION_CLASS_DESCRIPTOR->setCaptionsButtonStatus(Z)V
                    """
            )
        }
    }
}

