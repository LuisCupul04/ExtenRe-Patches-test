/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.utils.componentlist

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod
import com.extenre.patches.shared.conversionContextFingerprintToString
import com.extenre.patches.shared.litho.componentContextSubParserFingerprint
import com.extenre.patches.youtube.utils.extension.Constants.UTILS_PATH
import com.extenre.patches.youtube.utils.extension.sharedExtensionPatch
import com.extenre.util.addInstructionsAtControlFlowLabel
import com.extenre.util.findFreeRegister
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.extenre.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/LazilyConvertedElementPatch;"

private lateinit var lazilyConvertedElementMethod: MutableMethod

val lazilyConvertedElementHookPatch = bytecodePatch(
    name = "lazily-Converted-Element-Hook-Patch",
    description = "lazilyConvertedElementHookPatch"
) {
    dependsOn(sharedExtensionPatch)

    execute {
        componentListFingerprint.mutableMethodOrThrow(componentContextSubParserFingerprint).apply {
            val identifierReference = with(conversionContextFingerprintToString.mutableMethodOrThrow()) {
                val identifierStringIndex =
                    indexOfFirstStringInstructionOrThrow(", identifierProperty=")
                val identifierStringAppendIndex =
                    indexOfFirstInstructionOrThrow(identifierStringIndex, Opcode.INVOKE_VIRTUAL)
                val identifierAppendIndex =
                    indexOfFirstInstructionOrThrow(
                        identifierStringAppendIndex + 1,
                        Opcode.INVOKE_VIRTUAL
                    )
                val identifierRegister =
                    getInstruction<FiveRegisterInstruction>(identifierAppendIndex).registerD
                val identifierIndex =
                    indexOfFirstInstructionReversedOrThrow(identifierAppendIndex) {
                        opcode == Opcode.IGET_OBJECT &&
                                getReference<FieldReference>()?.type == "Ljava/lang/String;" &&
                                (this as? TwoRegisterInstruction)?.registerA == identifierRegister
                    }
                getInstruction<ReferenceInstruction>(identifierIndex).reference
            }

            val listIndex = implementation!!.instructions.lastIndex
            val listRegister = getInstruction<OneRegisterInstruction>(listIndex).registerA
            val identifierRegister = findFreeRegister(listIndex, listRegister)

            addInstructionsAtControlFlowLabel(
                listIndex, """
                    move-object/from16 v$identifierRegister, p2
                    iget-object v$identifierRegister, v$identifierRegister, $identifierReference
                    invoke-static {v$listRegister, v$identifierRegister}, $EXTENSION_CLASS_DESCRIPTOR->hookElements(Ljava/util/List;Ljava/lang/String;)V
                    """
            )

            lazilyConvertedElementMethod = lazilyConvertedElementPatchFingerprint.mutableMethodOrThrow()
        }
    }
}

internal fun hookElementList(descriptor: String) =
    lazilyConvertedElementMethod.addInstruction(
        0,
        "invoke-static {p0, p1}, $descriptor(Ljava/util/List;Ljava/lang/String;)V"
    )
