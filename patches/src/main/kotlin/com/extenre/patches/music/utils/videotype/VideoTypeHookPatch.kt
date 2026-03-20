/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.utils.videotype

import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.smali.ExternalLabel
import com.extenre.patches.music.utils.extension.Constants.UTILS_PATH
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/VideoTypeHookPatch;"

@Suppress("unused")
val videoTypeHookPatch = bytecodePatch(
    name = "video-type-hook-patch",
    description = "videoTypeHookPatch"
) {

    execute {

        videoTypeFingerprint.mutableMethodOrThrow(videoTypeParentFingerprint).apply {
            val getEnumIndex = indexOfGetEnumInstruction(this)
            val enumClass =
                (getInstruction<ReferenceInstruction>(getEnumIndex).reference as MethodReference).definingClass
            val referenceIndex = indexOfFirstInstructionOrThrow(getEnumIndex) {
                opcode == Opcode.SGET_OBJECT &&
                        getReference<FieldReference>()?.type == enumClass
            }
            val referenceInstruction =
                getInstruction<ReferenceInstruction>(referenceIndex).reference

            val insertIndex = indexOfFirstInstructionOrThrow(getEnumIndex, Opcode.IF_NEZ)
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstructionsWithLabels(
                insertIndex, """
                    if-nez v$insertRegister, :dismiss
                    sget-object v$insertRegister, $referenceInstruction
                    :dismiss
                    invoke-static {v$insertRegister}, $EXTENSION_CLASS_DESCRIPTOR->setVideoType(Ljava/lang/Enum;)V
                    """
            )
        }
    }
}
