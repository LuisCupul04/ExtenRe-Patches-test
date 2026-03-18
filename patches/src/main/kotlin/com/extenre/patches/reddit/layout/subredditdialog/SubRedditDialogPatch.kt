/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.reddit.layout.subredditdialog

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.reddit.utils.extension.Constants.PATCHES_PATH
import com.extenre.patches.reddit.utils.patch.PatchList.REMOVE_SUBREDDIT_DIALOG
import com.extenre.patches.reddit.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.reddit.utils.settings.is_2024_41_or_greater
import com.extenre.patches.reddit.utils.settings.is_2025_01_or_greater
import com.extenre.patches.reddit.utils.settings.is_2025_05_or_greater
import com.extenre.patches.reddit.utils.settings.is_2025_06_or_greater
import com.extenre.patches.reddit.utils.settings.settingsPatch
import com.extenre.patches.reddit.utils.settings.updatePatchStatus
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.fingerprint.mutableClassOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstruction
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/RemoveSubRedditDialogPatch;"

@Suppress("unused")
val subRedditDialogPatch = bytecodePatch(
    name = REMOVE_SUBREDDIT_DIALOG.key,
    description = "${REMOVE_SUBREDDIT_DIALOG.title}: ${REMOVE_SUBREDDIT_DIALOG.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        sharedResourceIdPatch,
    )

    execute {

        if (is_2024_41_or_greater) {
            frequentUpdatesHandlerFingerprint
                .mutableMethodOrThrow()
                .apply {
                    listOfIsLoggedInInstruction(this)
                        .forEach { index ->
                            val register =
                                getInstruction<OneRegisterInstruction>(index + 1).registerA

                            addInstructions(
                                index + 2, """
                                    invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->spoofLoggedInStatus(Z)Z
                                    move-result v$register
                                    """
                            )
                        }
                }
        }

        // Not used in latest Reddit client.
        if (!is_2025_05_or_greater) {
            frequentUpdatesSheetScreenFingerprint.mutableMethodOrThrow().apply {
                val index = indexOfFirstInstructionReversedOrThrow(Opcode.RETURN_OBJECT)
                val register =
                    getInstruction<OneRegisterInstruction>(index).registerA

                addInstruction(
                    index,
                    "invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->dismissDialog(Landroid/view/View;)V"
                )
            }
        }

        if (is_2025_01_or_greater) {
            nsfwAlertEmitFingerprint.mutableMethodOrThrow().apply {
                val hasBeenVisitedIndex = indexOfHasBeenVisitedInstruction(this)
                val hasBeenVisitedRegister =
                    getInstruction<OneRegisterInstruction>(hasBeenVisitedIndex + 1).registerA

                addInstructions(
                    hasBeenVisitedIndex + 2, """
                        invoke-static {v$hasBeenVisitedRegister}, $EXTENSION_CLASS_DESCRIPTOR->spoofHasBeenVisitedStatus(Z)Z
                        move-result v$hasBeenVisitedRegister
                        """
                )
            }

            var hookCount = 0

            nsfwAlertBuilderFingerprint.mutableClassOrThrow().let {
                it.methods.forEach { method ->
                    method.apply {
                        val showIndex = indexOfFirstInstruction {
                            opcode == Opcode.INVOKE_VIRTUAL &&
                                    getReference<MethodReference>()?.name == "show"
                        }
                        if (showIndex >= 0) {
                            val dialogRegister =
                                getInstruction<OneRegisterInstruction>(showIndex + 1).registerA

                            addInstruction(
                                showIndex + 2,
                                "invoke-static {v$dialogRegister}, $EXTENSION_CLASS_DESCRIPTOR->dismissNSFWDialog(Ljava/lang/Object;)V"
                            )
                            hookCount++
                        }
                    }
                }
            }

            if (hookCount == 0) {
                throw PatchException("Failed to find hook method")
            }
        }

        // Not used in latest Reddit client.
        if (!is_2025_06_or_greater) {
            redditAlertDialogsFingerprint.mutableMethodOrThrow().apply {
                val backgroundTintIndex = indexOfSetBackgroundTintListInstruction(this)
                val insertIndex =
                    indexOfFirstInstructionOrThrow(backgroundTintIndex) {
                        opcode == Opcode.INVOKE_VIRTUAL &&
                                getReference<MethodReference>()?.name == "setTextAppearance"
                    }
                val insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerC

                addInstruction(
                    insertIndex,
                    "invoke-static {v$insertRegister}, $EXTENSION_CLASS_DESCRIPTOR->confirmDialog(Landroid/widget/TextView;)V"
                )
            }
        }

        updatePatchStatus(
            "enableSubRedditDialog",
            REMOVE_SUBREDDIT_DIALOG
        )
    }
}
