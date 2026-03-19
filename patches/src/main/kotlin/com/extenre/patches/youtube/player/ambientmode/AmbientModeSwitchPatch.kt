/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.player.ambientmode

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.patch.PatchList.AMBIENT_MODE_CONTROL
import com.extenre.patches.youtube.utils.playservice.is_19_34_or_greater
import com.extenre.patches.youtube.utils.playservice.is_19_41_or_greater
import com.extenre.patches.youtube.utils.playservice.versionCheckPatch
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.findMethodOrThrow
import com.extenre.util.fingerprint.injectLiteralInstructionBooleanCall
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.extenre.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

@Suppress("unused")
val ambientModeSwitchPatch = bytecodePatch(
    name = AMBIENT_MODE_CONTROL.key,
    description = "${AMBIENT_MODE_CONTROL.title}: ${AMBIENT_MODE_CONTROL.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        versionCheckPatch,
    )

    execute {
        // region patch for bypass ambient mode restrictions

        var syntheticClassList = emptyArray<String>()

        mapOf(
            powerSaveModeBroadcastReceiverFingerprint to false,
            powerSaveModeSyntheticFingerprint to true
        ).forEach { (fingerprint, reversed) ->
            fingerprint.mutableMethodOrThrow().apply {
                val stringIndex =
                    indexOfFirstStringInstructionOrThrow("android.os.action.POWER_SAVE_MODE_CHANGED")
                val targetIndex =
                    if (reversed)
                        indexOfFirstInstructionReversedOrThrow(stringIndex, Opcode.INVOKE_DIRECT)
                    else
                        indexOfFirstInstructionOrThrow(stringIndex, Opcode.INVOKE_DIRECT)
                val targetClass =
                    (getInstruction<ReferenceInstruction>(targetIndex).reference as MethodReference).definingClass

                syntheticClassList += targetClass
            }
        }

        syntheticClassList.distinct().forEach { className ->
            // Reemplazar findmutableMethodOrThrow por búsqueda manual
            val method = findMethodOrThrow(className) {
                name == "accept"
            }
            val classDef = classes.find { it.type == className }
                ?: throw PatchException("Class not found: $className")
            val mutableMethod = proxy(classDef).mutableClass.methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.apply {
                implementation!!.instructions
                    .withIndex()
                    .filter { (_, instruction) ->
                        val reference = (instruction as? ReferenceInstruction)?.reference
                        instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                                reference is MethodReference &&
                                reference.name == "isPowerSaveMode"
                    }
                    .map { (index, _) -> index }
                    .reversed()
                    .forEach { index ->
                        val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

                        addInstructions(
                            index + 2, """
                                invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->bypassAmbientModeRestrictions(Z)Z
                                move-result v$register
                                """
                        )
                    }
            }
        }

        // endregion

        // region patch for disable ambient mode in fullscreen

        if (!is_19_41_or_greater) {
            ambientModeInFullscreenFingerprint.injectLiteralInstructionBooleanCall(
                AMBIENT_MODE_IN_FULLSCREEN_FEATURE_FLAG,
                "$PLAYER_CLASS_DESCRIPTOR->disableAmbientModeInFullscreen()Z"
            )
        }

        if (is_19_34_or_greater) {
            setFullScreenBackgroundColorFingerprint.mutableMethodOrThrow().apply {
                val insertIndex = indexOfFirstInstructionReversedOrThrow {
                    getReference<MethodReference>()?.name == "setBackgroundColor"
                }
                val register = getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                addInstructions(
                    insertIndex,
                    """
                        invoke-static { v$register }, $PLAYER_CLASS_DESCRIPTOR->getFullScreenBackgroundColor(I)I
                        move-result v$register
                        """,
                )
            }
        }

        // endregion

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: AMBIENT_MODE_CONTROLS"
            ),
            AMBIENT_MODE_CONTROL
        )

        // endregion

    }
}
