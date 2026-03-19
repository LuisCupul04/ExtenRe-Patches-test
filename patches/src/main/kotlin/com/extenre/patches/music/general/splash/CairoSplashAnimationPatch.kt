/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.general.splash

import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod
import com.extenre.patcher.util.smali.ExternalLabel
import com.extenre.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import com.extenre.patches.music.utils.extension.Constants.GENERAL_PATH
import com.extenre.patches.music.utils.patch.PatchList.DISABLE_CAIRO_SPLASH_ANIMATION
import com.extenre.patches.music.utils.playservice.is_7_16_or_greater
import com.extenre.patches.music.utils.playservice.is_7_20_or_greater
import com.extenre.patches.music.utils.playservice.versionCheckPatch
import com.extenre.patches.music.utils.resourceid.mainActivityLaunchAnimation
import com.extenre.patches.music.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.music.utils.settings.CategoryType
import com.extenre.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import com.extenre.patches.music.utils.settings.addSwitchPreference
import com.extenre.patches.music.utils.settings.settingsPatch
import com.extenre.util.Utils.printWarn
import com.extenre.util.findFreeRegister
import com.extenre.util.fingerprint.injectLiteralInstructionBooleanCall
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstLiteralInstructionOrThrow
import com.extenre.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_METHOD_DESCRIPTOR =
    "$GENERAL_PATH/CairoSplashAnimationPatch;->disableCairoSplashAnimation(Z)Z"

@Suppress("unused")
val cairoSplashAnimationPatch = bytecodePatch(
    name = DISABLE_CAIRO_SPLASH_ANIMATION.key,
    description = "${DISABLE_CAIRO_SPLASH_ANIMATION.title}: ${DISABLE_CAIRO_SPLASH_ANIMATION.summary}",
) {
    compatibleWith(
        YOUTUBE_MUSIC_PACKAGE_NAME(
            "7.16.53",
            "7.25.53",
            "8.12.54",
            "8.28.54",
            "8.30.54",
            "9.05.51",
        ),
    )

    dependsOn(
        settingsPatch,
        sharedResourceIdPatch,
        versionCheckPatch,
    )

    execute {
        if (!is_7_16_or_greater) {
            printWarn("\"${DISABLE_CAIRO_SPLASH_ANIMATION.title}\" is not supported in this version. Use YouTube Music 7.16.53 or later.")
            return@execute
        }

        fun MutableMethod.getMoveInstructions(
            startIndex: Int,
            endIndex: Int
        ): String {
            var smaliInstructions = ""
            val moveOpcodes = setOf(
                Opcode.MOVE, Opcode.MOVE_16,
                Opcode.MOVE_OBJECT, Opcode.MOVE_OBJECT_FROM16,
                Opcode.MOVE_WIDE, Opcode.MOVE_WIDE_16,
                Opcode.MOVE_WIDE_FROM16, Opcode.MOVE_FROM16
            )
            for (index in startIndex..endIndex) {
                val opcode = getInstruction(index).opcode
                if (moveOpcodes.contains(opcode)) {
                    val opcodeName = opcode.name
                    val instruction = getInstruction<TwoRegisterInstruction>(index)

                    val line = """
                        ${opcode.name} v${instruction.registerA}, v${instruction.registerB}
                        
                        """.trimIndent()

                    smaliInstructions += line
                }
            }

            return smaliInstructions
        }

        if (!is_7_20_or_greater) {
            cairoSplashAnimationConfigFingerprint.injectLiteralInstructionBooleanCall(
                CAIRO_SPLASH_ANIMATION_FEATURE_FLAG,
                EXTENSION_METHOD_DESCRIPTOR
            )
        } else {
            cairoSplashAnimationConfigFingerprint.mutableMethodOrThrow().apply {
                val stringIndex = indexOfFirstStringInstructionOrThrow("sa_e")
                val gotoIndex = indexOfFirstInstructionOrThrow(stringIndex) {
                    opcode == Opcode.GOTO || opcode == Opcode.GOTO_16
                }
                val literalIndex = indexOfFirstLiteralInstructionOrThrow(
                    mainActivityLaunchAnimation
                )
                val insertIndex =
                    indexOfSetContentViewInstruction(this, literalIndex) + 1
                val freeRegister = findFreeRegister(insertIndex, false)
                val jumpIndex = indexOfFirstInstructionOrThrow(insertIndex) {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.parameterTypes?.firstOrNull() == "Ljava/lang/Runnable;"
                } + 1

                addInstructionsWithLabels(
                    insertIndex,
                    getMoveInstructions(stringIndex, gotoIndex) + """
                        const/4 v$freeRegister, 0x1
                        invoke-static {v$freeRegister}, $EXTENSION_METHOD_DESCRIPTOR
                        move-result v$freeRegister
                        if-eqz v$freeRegister, :skip
                        """, ExternalLabel("skip", getInstruction(jumpIndex))
                )
            }
        }

        addSwitchPreference(
            CategoryType.GENERAL,
            "extenre_disable_cairo_splash_animation",
            "false"
        )

        updatePatchStatus(DISABLE_CAIRO_SPLASH_ANIMATION)

    }
}
