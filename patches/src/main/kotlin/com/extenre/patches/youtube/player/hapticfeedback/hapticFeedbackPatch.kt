/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.player.hapticfeedback

import com.extenre.patcher.Fingerprint
import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.smali.ExternalLabel
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.patch.PatchList.DISABLE_HAPTIC_FEEDBACK
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val hapticFeedbackPatch = bytecodePatch(
    name = DISABLE_HAPTIC_FEEDBACK.key,
    description = "${DISABLE_HAPTIC_FEEDBACK.title}: ${DISABLE_HAPTIC_FEEDBACK.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        fun Pair<String, Fingerprint>.hookHapticFeedback(methodName: String) =
            matchOrThrow().let {
                it.method.apply {
                    var index = 0
                    var register = 0

                    if (name == "run") {
                        val stringIndex = it.stringMatches!!.first().index
                        index = indexOfFirstInstructionReversedOrThrow(stringIndex) {
                            opcode == Opcode.SGET &&
                                    getReference<FieldReference>()?.toString() == "Landroid/os/Build${'$'}VERSION;->SDK_INT:I"
                        }
                        register = getInstruction<OneRegisterInstruction>(index).registerA
                    }

                    addInstructionsWithLabels(
                        index, """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->$methodName()Z
                        move-result v$register
                        if-eqz v$register, :vibrate
                        return-void
                        """, ExternalLabel("vibrate", getInstruction(index))
                    )
                }
            }

        arrayOf(
            seekHapticsFingerprint to "disableSeekVibrate",
            seekUndoHapticsFingerprint to "disableSeekUndoVibrate",
            scrubbingHapticsFingerprint to "disableScrubbingVibrate",
            markerHapticsFingerprint to "disableChapterVibrate",
            zoomHapticsFingerprint to "disableZoomVibrate"
        ).map { (fingerprint, methodName) ->
            fingerprint.hookHapticFeedback(methodName)
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: DISABLE_HAPTIC_FEEDBACK"
            ),
            DISABLE_HAPTIC_FEEDBACK
        )

        // endregion

    }
}
