/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.reddit.layout.navigation

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.reddit.utils.extension.Constants.PATCHES_PATH
import com.extenre.patches.reddit.utils.patch.PatchList.HIDE_NAVIGATION_BUTTONS
import com.extenre.patches.reddit.utils.settings.is_2024_26_or_greater
import com.extenre.patches.reddit.utils.settings.is_2025_06_or_greater
import com.extenre.patches.reddit.utils.settings.settingsPatch
import com.extenre.patches.reddit.utils.settings.updatePatchStatus
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.fingerprint.resolvable
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/NavigationButtonsPatch;"

@Suppress("unused")
val navigationButtonsPatch = bytecodePatch(
    name = HIDE_NAVIGATION_BUTTONS.key,
    description = "${HIDE_NAVIGATION_BUTTONS.title}: ${HIDE_NAVIGATION_BUTTONS.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        if (is_2024_26_or_greater) {
            val fingerprints = mutableListOf(bottomNavScreenSetupBottomNavigationFingerprint)

            if (is_2025_06_or_greater) fingerprints += composeBottomNavScreenFingerprint

            fingerprints.forEach { fingerprint ->
                fingerprint.mutableMethodOrThrow().apply {
                    val arrayIndex = indexOfButtonsArrayInstruction(this)
                    val arrayRegister =
                        getInstruction<OneRegisterInstruction>(arrayIndex + 1).registerA

                    addInstructions(
                        arrayIndex + 2, """
                            invoke-static {v$arrayRegister}, $EXTENSION_CLASS_DESCRIPTOR->hideNavigationButtons([Ljava/lang/Object;)[Ljava/lang/Object;
                            move-result-object v$arrayRegister
                            """
                    )
                }
            }
        } else {
            if (bottomNavScreenFingerprint.resolvable()) {
                val bottomNavScreenMutableClass = with(bottomNavScreenFingerprint.mutableMethodOrThrow()) {
                    val startIndex = indexOfGetDimensionPixelSizeInstruction(this)
                    val targetIndex =
                        indexOfFirstInstructionOrThrow(startIndex, Opcode.NEW_INSTANCE)
                    val targetReference =
                        getInstruction<ReferenceInstruction>(targetIndex).reference.toString()

                    classBy { it.type == targetReference }
                        ?.mutableClass
                        ?: throw PatchException("Failed to find class $targetReference")
                }

                bottomNavScreenOnGlobalLayoutFingerprint.second.matchOrNull(
                    bottomNavScreenMutableClass
                )?.let { match ->
                    val method = match.method
                    val classDef = match.classDef
                    val mutableMethod = proxy(classDef).mutableClass.methods.first {
                        MethodUtil.methodSignaturesMatch(it, method)
                    }
                    mutableMethod.apply {
                        val startIndex = match.patternMatch!!.startIndex
                        val targetRegister =
                            getInstruction<FiveRegisterInstruction>(startIndex).registerC

                        addInstruction(
                            startIndex + 1,
                            "invoke-static {v$targetRegister}, $EXTENSION_CLASS_DESCRIPTOR->hideNavigationButtons(Landroid/view/ViewGroup;)V"
                        )
                    }
                }
            } else {
                // Legacy method.
                bottomNavScreenHandlerFingerprint.mutableMethodOrThrow().apply {
                    val targetIndex = indexOfGetItemsInstruction(this) + 1
                    val targetRegister =
                        getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    addInstructions(
                        targetIndex + 1, """
                            invoke-static {v$targetRegister}, $EXTENSION_CLASS_DESCRIPTOR->hideNavigationButtons(Ljava/util/List;)Ljava/util/List;
                            move-result-object v$targetRegister
                            """
                    )
                }
            }
        }

        updatePatchStatus(
            "enableNavigationButtons",
            HIDE_NAVIGATION_BUTTONS
        )
    }
}
