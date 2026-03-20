/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.utils.dismiss

import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.music.utils.extension.Constants.EXTENSION_PATH
import com.extenre.util.addStaticFieldToExtension
import com.extenre.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/utils/VideoUtils;"

@Suppress("unused")
val dismissQueueHookPatch = bytecodePatch(
    name = "dismiss-queue-hook",
    description = "dismissQueueHookPatch"
) {

    execute {
        dismissQueueFingerprint.methodOrThrow().let { method ->
            // Buscar la primera instrucción INVOKE_VIRTUAL o INVOKE_STATIC en el método
            val invokeIndex = method.implementation?.instructions?.indexOfFirst { instruction ->
                instruction.opcode == Opcode.INVOKE_VIRTUAL || instruction.opcode == Opcode.INVOKE_STATIC
            } ?: throw PatchException("No se encontró ninguna instrucción INVOKE en el método")

            val invokeInstruction = method.getInstruction<ReferenceInstruction>(invokeIndex)
            val methodRef = invokeInstruction.reference as? MethodReference
                ?: throw PatchException("La instrucción INVOKE no contiene una referencia a método")

            // Obtenemos la clase y nombre del método destino
            val targetClass = methodRef.definingClass
            val targetMethodName = methodRef.name

            val smaliInstructions = """
                if-eqz v0, :ignore
                invoke-virtual {v0}, $targetClass->$targetMethodName()V
                :ignore
                return-void
            """.trimIndent()

            addStaticFieldToExtension(
                EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR,
                "dismissQueue",
                "dismissQueueClass",
                targetClass,
                smaliInstructions
            )
        }
    }
}