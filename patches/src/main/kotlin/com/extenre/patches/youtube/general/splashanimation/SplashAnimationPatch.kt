/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.general.splashanimation

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.patch.PatchList.DISABLE_SPLASH_ANIMATION
import com.extenre.patches.youtube.utils.playservice.is_20_02_or_greater
import com.extenre.patches.youtube.utils.playservice.versionCheckPatch
import com.extenre.patches.youtube.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val splashAnimationPatch = bytecodePatch(
    name = DISABLE_SPLASH_ANIMATION.key,
    description = "${DISABLE_SPLASH_ANIMATION.title}: ${DISABLE_SPLASH_ANIMATION.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        sharedResourceIdPatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {

        val startUpResourceIdMethod =
            startUpResourceIdFingerprint.mutableMethodOrThrow(startUpResourceIdParentFingerprint)
        val startUpResourceIdMethodCall =
            startUpResourceIdMethod.definingClass + "->" + startUpResourceIdMethod.name + "(I)Z"

        splashAnimationFingerprint.mutableMethodOrThrow().apply {
            implementation!!.instructions
                .withIndex()
                .filter { (_, instruction) ->
                    instruction.opcode == Opcode.INVOKE_STATIC &&
                            (instruction as? ReferenceInstruction)?.reference?.toString() == startUpResourceIdMethodCall
                }
                .map { (index, _) -> index }
                .reversed()
                .forEach { index ->
                    val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

                    addInstructions(
                        index + 2, """
                            invoke-static {v$register}, $GENERAL_CLASS_DESCRIPTOR->disableSplashAnimation(Z)Z
                            move-result v$register
                            """
                    )
                }

            if (is_20_02_or_greater) {
                val animatedVectorDrawableIndex =
                    indexOfStartAnimatedVectorDrawableInstruction(this)
                val arrayIndex =
                    indexOfFirstInstructionReversedOrThrow(animatedVectorDrawableIndex) {
                        val reference = getReference<MethodReference>()
                        opcode == Opcode.INVOKE_VIRTUAL &&
                                reference?.returnType == "V" &&
                                reference.parameterTypes.size == 1 &&
                                reference.parameterTypes.first().startsWith("[L")
                    }

                val insertIndex =
                    indexOfFirstInstructionOrThrow(arrayIndex, Opcode.IF_NE)
                val insertInstruction = getInstruction<TwoRegisterInstruction>(insertIndex)

                addInstructions(
                    insertIndex, """
                        invoke-static {v${insertInstruction.registerA}, v${insertInstruction.registerB}}, $GENERAL_CLASS_DESCRIPTOR->disableSplashAnimation(II)I
                        move-result v${insertInstruction.registerA}
                        """
                )
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: DISABLE_SPLASH_ANIMATION"
            ),
            DISABLE_SPLASH_ANIMATION
        )

        // endregion

    }
}
