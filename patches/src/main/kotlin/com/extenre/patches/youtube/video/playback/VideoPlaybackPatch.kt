/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.video.playback

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.shared.customspeed.customPlaybackSpeedPatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.extension.Constants.VIDEO_PATH
import com.extenre.patches.youtube.utils.patch.PatchList.VIDEO_PLAYBACK
import com.extenre.patches.youtube.utils.playservice.is_19_18_or_greater
import com.extenre.patches.youtube.utils.playservice.versionCheckPatch
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.getWalkerMethod
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_VIDEO_PLAYBACK_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/VideoPlaybackPatch;"

@Suppress("unused")
val videoPlaybackPatch = bytecodePatch(
    name = VIDEO_PLAYBACK.key,
    description = "${VIDEO_PLAYBACK.title}: ${VIDEO_PLAYBACK.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        versionCheckPatch,
        customPlaybackSpeedPatch(
            "$VIDEO_PATH/CustomPlaybackSpeedPatch;",
            5.0f
        ),
    )

    execute {
        // region patch for default playback speed

        val playbackSpeedMatch = playbackSpeedFingerprint.matchOrThrow(playbackSpeedParentFingerprint)
        val playbackSpeedMethod = playbackSpeedMatch.method
        val playbackSpeedClassDef = playbackSpeedMatch.classDef
        val playbackSpeedMutableMethod = proxy(playbackSpeedClassDef).mutableClass.methods.first {
            MethodUtil.methodSignaturesMatch(it, playbackSpeedMethod)
        }
        playbackSpeedMutableMethod.apply {
            val startIndex = playbackSpeedMatch.patternMatch!!.startIndex
            val speedRegister =
                getInstruction<OneRegisterInstruction>(startIndex + 1).registerA

            addInstructions(
                startIndex + 2, """
                        invoke-static {v$speedRegister}, $PLAYER_CLASS_DESCRIPTOR->getPlaybackSpeed(F)F
                        move-result v$speedRegister
                        """
            )
        }

        // Called when user selects a playback speed from the flyout menu.
        userSelectedPlaybackSpeedFingerprint.mutableMethodOrThrow().apply {
            val index = indexOfFirstInstructionOrThrow(Opcode.INVOKE_STATIC)
            val register = getInstruction<FiveRegisterInstruction>(index).registerC

            addInstruction(
                index,
                "invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->userSelectedPlaybackSpeed(F)V"
            )
        }

        // endregion

        // region patch for remember playback speed

        // Hook into the method that saves the selected speed.
        playbackSpeedSaveFingerprint.mutableMethodOrThrow().apply {
            val index = indexOfFirstInstructionOrThrow(Opcode.IPUT)
            val register = getInstruction<TwoRegisterInstruction>(index).registerA

            addInstructions(
                index, """
                    invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->savePlaybackSpeed(F)V
                    """
            )
        }

        // Hook into the method that loads the saved speed.
        playbackSpeedLoadFingerprint.mutableMethodOrThrow().apply {
            val index = indexOfFirstInstructionOrThrow(Opcode.IGET)
            val register = getInstruction<TwoRegisterInstruction>(index).registerA

            addInstructions(
                index + 1, """
                    invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->loadPlaybackSpeed(F)F
                    move-result v$register
                    """
            )
        }

        // endregion

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: VIDEO_PLAYBACK"
            ),
            VIDEO_PLAYBACK
        )

        // endregion
    }
}