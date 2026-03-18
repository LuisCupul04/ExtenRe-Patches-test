/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.utils.toolbar

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.extensions.InstructionExtensions.removeInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod
import com.extenre.patches.youtube.utils.extension.Constants.UTILS_PATH
import com.extenre.patches.youtube.utils.indexOfGetDrawableInstruction
import com.extenre.patches.youtube.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.youtube.utils.toolBarButtonFingerprint
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/ToolBarPatch;"

private lateinit var toolbarMethod: MutableMethod

val toolBarHookPatch = bytecodePatch(
    name = "tool-Bar-Hook-Patch",
    description = "toolBarHookPatch"
) {
    dependsOn(sharedResourceIdPatch)

    execute {
        toolBarButtonFingerprint.mutableMethodOrThrow().apply {
            val getDrawableIndex = indexOfGetDrawableInstruction(this)
            val enumOrdinalIndex = indexOfFirstInstructionReversedOrThrow(getDrawableIndex) {
                opcode == Opcode.INVOKE_INTERFACE &&
                        getReference<MethodReference>()?.returnType == "I"
            }
            val freeIndex = getDrawableIndex - 1

            val replaceReference = getInstruction<ReferenceInstruction>(enumOrdinalIndex).reference
            val replaceRegister =
                getInstruction<FiveRegisterInstruction>(enumOrdinalIndex).registerC
            val enumRegister = getInstruction<FiveRegisterInstruction>(enumOrdinalIndex).registerD
            val freeRegister = getInstruction<TwoRegisterInstruction>(freeIndex).registerA

            val imageViewIndex = indexOfFirstInstructionOrThrow(enumOrdinalIndex) {
                opcode == Opcode.IGET_OBJECT &&
                        getReference<FieldReference>()?.type == "Landroid/widget/ImageView;"
            }
            val imageViewReference =
                getInstruction<ReferenceInstruction>(imageViewIndex).reference

            addInstructions(
                enumOrdinalIndex + 1, """
                    iget-object v$freeRegister, p0, $imageViewReference
                    invoke-static {v$enumRegister, v$freeRegister}, $EXTENSION_CLASS_DESCRIPTOR->hookToolBar(Ljava/lang/Enum;Landroid/widget/ImageView;)V
                    invoke-interface {v$replaceRegister, v$enumRegister}, $replaceReference
                    """
            )
            removeInstruction(enumOrdinalIndex)
        }

        toolbarMethod = toolBarPatchFingerprint.mutableMethodOrThrow()
    }
}

internal fun hookToolBar(descriptor: String) =
    toolbarMethod.addInstructions(
        0,
        "invoke-static {p0, p1}, $descriptor(Ljava/lang/String;Landroid/view/View;)V"
    )
