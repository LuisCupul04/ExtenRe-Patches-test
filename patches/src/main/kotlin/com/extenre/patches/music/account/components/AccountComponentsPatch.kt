/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.account.components

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.extensions.InstructionExtensions.replaceInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.music.utils.extension.Constants.ACCOUNT_CLASS_DESCRIPTOR
import com.extenre.patches.music.utils.patch.PatchList.HIDE_ACCOUNT_COMPONENTS
import com.extenre.patches.music.utils.resourceid.channelHandle
import com.extenre.patches.music.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.music.utils.settings.CategoryType
import com.extenre.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import com.extenre.patches.music.utils.settings.addPreferenceWithIntent
import com.extenre.patches.music.utils.settings.addSwitchPreference
import com.extenre.patches.music.utils.settings.settingsPatch
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val accountComponentsPatch = bytecodePatch(
    name = HIDE_ACCOUNT_COMPONENTS.key,
    description = "${HIDE_ACCOUNT_COMPONENTS.title}: ${HIDE_ACCOUNT_COMPONENTS.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        sharedResourceIdPatch,
        settingsPatch,
    )

    execute {

        // region patch for hide account menu

        menuEntryFingerprint.mutableMethodOrThrow().apply {
            val textIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.name == "setText"
            }
            val viewIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.name == "addView"
            }

            val textRegister = getInstruction<FiveRegisterInstruction>(textIndex).registerD
            val viewRegister = getInstruction<FiveRegisterInstruction>(viewIndex).registerD

            addInstruction(
                textIndex + 1,
                "invoke-static {v$textRegister, v$viewRegister}, " +
                        "$ACCOUNT_CLASS_DESCRIPTOR->hideAccountMenu(Ljava/lang/CharSequence;Landroid/view/View;)V"
            )
        }

        // endregion

        // region patch for hide handle

        // account menu
        accountSwitcherAccessibilityLabelFingerprint.mutableMethodOrThrow().apply {
            val textColorIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.name == "setTextColor"
            }
            val setVisibilityIndex = indexOfFirstInstructionOrThrow(textColorIndex) {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.name == "setVisibility"
            }
            val textViewInstruction =
                getInstruction<FiveRegisterInstruction>(setVisibilityIndex)

            replaceInstruction(
                setVisibilityIndex,
                "invoke-static {v${textViewInstruction.registerC}, v${textViewInstruction.registerD}}, " +
                        "$ACCOUNT_CLASS_DESCRIPTOR->hideHandle(Landroid/widget/TextView;I)V"
            )
        }

        // account switcher
        val textViewField = with(
            channelHandleFingerprint
                .mutableMethodOrThrow(namesInactiveAccountThumbnailSizeFingerprint)
        ) {
            val literalIndex = indexOfFirstLiteralInstructionOrThrow(channelHandle)
            getInstruction(
                indexOfFirstInstructionOrThrow(literalIndex) {
                    opcode == Opcode.IPUT_OBJECT &&
                            getReference<FieldReference>()?.type == "Landroid/widget/TextView;"
                },
            ).getReference<FieldReference>()
        }

        namesInactiveAccountThumbnailSizeFingerprint.mutableMethodOrThrow().apply {
            var hook = false

            implementation!!.instructions
                .withIndex()
                .filter { (_, instruction) ->
                    val reference =
                        (instruction as? ReferenceInstruction)?.reference
                    instruction.opcode == Opcode.IGET_OBJECT &&
                            reference is FieldReference &&
                            reference == textViewField
                }
                .map { (index, _) -> index }
                .forEach { index ->
                    val insertIndex = index - 1
                    if (!hook && getInstruction(insertIndex).opcode == Opcode.IF_NEZ) {
                        val insertRegister =
                            getInstruction<OneRegisterInstruction>(insertIndex).registerA

                        addInstructions(
                            insertIndex, """
                                invoke-static {v$insertRegister}, $ACCOUNT_CLASS_DESCRIPTOR->hideHandle(Z)Z
                                move-result v$insertRegister
                                """
                        )
                        hook = true
                    }
                }

            if (!hook) {
                throw PatchException("Could not find TextUtils.isEmpty() index")
            }
        }

        // endregion

        // region patch for hide terms container

        termsOfServiceFingerprint.mutableMethodOrThrow().apply {
            val insertIndex = indexOfFirstInstructionOrThrow {
                val reference = getReference<MethodReference>()
                opcode == Opcode.INVOKE_VIRTUAL &&
                        reference?.name == "setVisibility" &&
                        reference.definingClass.endsWith("/PrivacyTosFooter;")
            }
            val visibilityRegister =
                getInstruction<FiveRegisterInstruction>(insertIndex).registerD

            addInstruction(
                insertIndex + 1,
                "const/4 v$visibilityRegister, 0x0"
            )
            addInstructions(
                insertIndex, """
                    invoke-static {}, $ACCOUNT_CLASS_DESCRIPTOR->hideTermsContainer()I
                    move-result v$visibilityRegister
                    """
            )

        }

        // endregion

        addSwitchPreference(
            CategoryType.ACCOUNT,
            "extenre_hide_account_menu",
            "false"
        )
        addPreferenceWithIntent(
            CategoryType.ACCOUNT,
            "extenre_hide_account_menu_filter_strings",
            "extenre_hide_account_menu"
        )
        addSwitchPreference(
            CategoryType.ACCOUNT,
            "extenre_hide_account_menu_empty_component",
            "false",
            "extenre_hide_account_menu"
        )
        addSwitchPreference(
            CategoryType.ACCOUNT,
            "extenre_hide_handle",
            "true"
        )
        addSwitchPreference(
            CategoryType.ACCOUNT,
            "extenre_hide_terms_container",
            "false"
        )

        updatePatchStatus(HIDE_ACCOUNT_COMPONENTS)

    }
}
