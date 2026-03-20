/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.misc.backgroundplayback

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.smali.ExternalLabel
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.patch.PatchList.REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS
import com.extenre.patches.youtube.utils.playservice.is_19_28_or_greater
import com.extenre.patches.youtube.utils.playservice.versionCheckPatch
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.getWalkerMethod
import com.extenre.util.indexOfFirstInstruction
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.extenre.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

@Suppress("unused")
val backgroundPlaybackPatch = bytecodePatch(
    name = REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS.key,
    description = "${REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS.title}: ${REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        versionCheckPatch,
    )

    execute {
        // region patch for background playback

        // Called when the app is in the background, and when the video details are loaded.
        // Should return true if background playback is allowed, otherwise false.
        backgroundPlaybackManagerFingerprint.mutableMethodOrThrow().addInstructions(
            0, """
                invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->getBackgroundPlaybackState()Z
                move-result v0
                return v0
                """
        )

        // Called only when the app is in the background.
        // Should return true to resume playback when returning to the app.
        backgroundPlaybackResumeFingerprint.mutableMethodOrThrow().addInstructions(
            0, """
                invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->getBackgroundPlaybackResumeState()Z
                move-result v0
                return v0
                """
        )

        // endregion

        // region patch for background playback for kids videos

        kidsBackgroundFingerprint.mutableMethodOrThrow().addInstructions(
            0, """
                invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->getBackgroundPlaybackState()Z
                move-result v0
                return v0
                """
        )

        // endregion

        // region patch for exclusive audio playback

        // Don't play music videos in background when audio only is not available.
        // Called when the video is opened.
        val videoStageMatch = videoStageFingerprint.matchOrThrow()
        val videoStageMethod = videoStageMatch.method
        val videoStageClassDef = videoStageMatch.classDef
        val videoStageMutableMethod = proxy(videoStageClassDef).mutableClass.methods.first {
            MethodUtil.methodSignaturesMatch(it, videoStageMethod)
        }
        videoStageMutableMethod.apply {
            val startIndex = videoStageMatch.patternMatch!!.startIndex
            val targetIndex = indexOfFirstInstructionOrThrow(startIndex, Opcode.INVOKE_VIRTUAL)
            val register = getInstruction<FiveRegisterInstruction>(targetIndex).registerC

            addInstructions(
                targetIndex, """
                        invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->checkAudioOnlyState(Ljava/lang/Object;)Ljava/lang/Object;
                        move-result-object v$register
                        """
            )
        }

        // Called when a video is opened to check if audio only should be used.
        val audioTrackCheckMatch = audioTrackCheckFingerprint.matchOrThrow()
        val audioTrackCheckMethod = audioTrackCheckMatch.method
        val audioTrackCheckClassDef = audioTrackCheckMatch.classDef
        val audioTrackCheckMutableMethod = proxy(audioTrackCheckClassDef).mutableClass.methods.first {
            MethodUtil.methodSignaturesMatch(it, audioTrackCheckMethod)
        }
        audioTrackCheckMutableMethod.apply {
            val startIndex = audioTrackCheckMatch.patternMatch!!.startIndex
            val targetIndex = indexOfFirstInstructionOrThrow(startIndex, Opcode.IGET)
            val register = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

            addInstructions(
                targetIndex, """
                        invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->getAudioOnlyState(Z)Z
                        move-result v$register
                        """
            )
        }

        // endregion

        // region patch for background playback for kids videos

        kidsBackgroundPlaybackPolicyControllerFingerprint.mutableMethodOrThrow().addInstruction(
            0, "return-void"
        )

        // endregion

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS"
            ),
            REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS
        )

        // endregion
    }
}