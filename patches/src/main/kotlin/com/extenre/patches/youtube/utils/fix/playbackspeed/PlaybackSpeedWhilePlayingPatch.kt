/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.utils.fix.playbackspeed

import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.smali.ExternalLabel
import com.extenre.patches.youtube.utils.engagement.engagementPanelHookPatch
import com.extenre.patches.youtube.utils.extension.Constants.UTILS_PATH
import com.extenre.patches.youtube.utils.extension.sharedExtensionPatch
import com.extenre.patches.youtube.utils.playertype.playerTypeHookPatch
import com.extenre.patches.youtube.utils.playservice.is_19_34_or_greater
import com.extenre.patches.youtube.utils.playservice.versionCheckPatch
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/PlaybackSpeedWhilePlayingPatch;"

val playbackSpeedWhilePlayingPatch = bytecodePatch(
    name = "playback-Speed-While-Playing-Patch",
    description = "playbackSpeedWhilePlayingPatch"
) {
    dependsOn(
        sharedExtensionPatch,
        engagementPanelHookPatch,
        playerTypeHookPatch,
        versionCheckPatch,
    )

    execute {
        if (!is_19_34_or_greater) {
            return@execute
        }

        /**
         * There is an issue where sometimes when you click on a comment in a video and press the back button or click on the timestamp of the comment, the playback speed will change to 1.0x.
         *
         * This can be reproduced on unpatched YouTube 19.34.42+ by following these steps:
         * 1. After the video starts, manually change the playback speed to something other than 1.0x.
         * 2. If enough time has passed since the video started, open the comments panel.
         * 3. Click on a comment and press the back button, or click on the timestamp of the comment.
         * 4. Sometimes the playback speed will change to 1.0x.
         *
         * This is an issue that Google should fix, but it is not that hard to fix, so it has been implemented in the patch.
         */
        playbackSpeedInFeedsFingerprint.mutableMethodOrThrow().apply {
            val freeRegister = implementation!!.registerCount - parameters.size - 2
            val playbackSpeedIndex = indexOfGetPlaybackSpeedInstruction(this)
            val playbackSpeedRegister =
                getInstruction<TwoRegisterInstruction>(playbackSpeedIndex).registerA
            val jumpIndex = indexOfFirstInstructionOrThrow(playbackSpeedIndex, Opcode.RETURN_VOID)

            addInstructionsWithLabels(
                playbackSpeedIndex + 1, """
                    invoke-static { v$playbackSpeedRegister }, $EXTENSION_CLASS_DESCRIPTOR->playbackSpeedChanged(F)Z
                    move-result v$freeRegister
                    if-nez v$freeRegister, :do_not_change
                    """, ExternalLabel("do_not_change", getInstruction(jumpIndex))
            )
        }
    }
}
