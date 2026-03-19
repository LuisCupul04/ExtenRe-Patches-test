/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.utils.dismiss

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod
import com.extenre.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import com.extenre.patches.youtube.utils.extension.sharedExtensionPatch
import com.extenre.util.addStaticFieldToExtension
import com.extenre.util.findMethodOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.getWalkerMethod
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.extenre.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/utils/VideoUtils;"

private lateinit var dismissMethod: MutableMethod

val dismissPlayerHookPatch = bytecodePatch(
    name = "dismiss-Player-Hook-Patch",
    description = "dismissPlayerHookPatch"
) {
    dependsOn(sharedExtensionPatch)

    execute {
        dismissPlayerOnClickListenerFingerprint.mutableMethodOrThrow().apply {
            val literalIndex =
                indexOfFirstLiteralInstructionOrThrow(DISMISS_PLAYER_LITERAL)
            val dismissPlayerIndex = indexOfFirstInstructionOrThrow(literalIndex) {
                val reference = getReference<MethodReference>()
                opcode == Opcode.INVOKE_VIRTUAL &&
                        reference?.returnType == "V" &&
                        reference.parameterTypes.isEmpty()
            }

            getWalkerMethod(dismissPlayerIndex).apply {
                val jumpIndex = indexOfFirstInstructionReversedOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.returnType == "V"
                }
                getWalkerMethod(jumpIndex).apply {
                    val jumpIndex = indexOfFirstInstructionReversedOrThrow {
                        val reference = getReference<MethodReference>()
                        opcode == Opcode.INVOKE_VIRTUAL &&
                                reference?.returnType == "V" &&
                                reference.parameterTypes.firstOrNull() == "I"
                    }
                    dismissMethod = getWalkerMethod(jumpIndex)
                }
            }

            val dismissPlayerReference =
                getInstruction<ReferenceInstruction>(dismissPlayerIndex).reference as MethodReference
            val dismissPlayerClass = dismissPlayerReference.definingClass

            val fieldIndex =
                indexOfFirstInstructionReversedOrThrow(dismissPlayerIndex) {
                    opcode == Opcode.IGET_OBJECT &&
                            getReference<FieldReference>()?.type == dismissPlayerClass
                }
            val fieldReference =
                getInstruction<ReferenceInstruction>(fieldIndex).reference as FieldReference

            // Reemplazar findmutableMethodOrThrow por búsqueda manual
            run {
                val method = findMethodOrThrow(fieldReference.definingClass)
                val classDef = classes.find { it.type == fieldReference.definingClass }
                    ?: throw PatchException("Class not found: ${fieldReference.definingClass}")
                val mutableMethod = proxy(classDef).mutableClass.methods.first {
                    MethodUtil.methodSignaturesMatch(it, method)
                }
                mutableMethod.apply {
                    val insertIndex = indexOfFirstInstructionOrThrow {
                        opcode == Opcode.IPUT_OBJECT &&
                                getReference<FieldReference>() == fieldReference
                    }
                    val insertRegister =
                        getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                    addInstruction(
                        insertIndex,
                        "sput-object v$insertRegister, $EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR->dismissPlayerClass:$dismissPlayerClass"
                    )

                    val smaliInstructions =
                        """
                        if-eqz v0, :ignore
                        invoke-virtual {v0}, $dismissPlayerReference
                        :ignore
                        return-void
                        """

                    addStaticFieldToExtension(
                        EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR,
                        "dismissPlayer",
                        "dismissPlayerClass",
                        dismissPlayerClass,
                        smaliInstructions,
                        false
                    )
                }
            }
        }
    }
}

/**
 * This method is called when the video is closed.
 */
internal fun hookDismissObserver(descriptor: String) =
    dismissMethod.addInstruction(
        0,
        "invoke-static {p1}, $descriptor"
    )
