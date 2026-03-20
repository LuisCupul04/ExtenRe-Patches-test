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
import com.extenre.patcher.util.MethodNavigator
import com.extenre.patches.music.utils.extension.Constants.EXTENSION_PATH
import com.extenre.util.addStaticFieldToExtension
import com.extenre.util.fingerprint.methodOrThrow
import com.extenre.util.indexOfDismissQueueInstruction   // Asegúrate de que esta función exista
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/utils/VideoUtils;"

@Suppress("unused")
val dismissQueueHookPatch = bytecodePatch(
    name = "dismiss-queue-hook",
    description = "dismissQueueHookPatch"
) {

    execute {
        dismissQueueFingerprint.methodOrThrow().let { method ->
            val dismissQueueIndex = indexOfDismissQueueInstruction(method)

            // Navegar al método llamado desde la instrucción en dismissQueueIndex
            val navigator = MethodNavigator(method)
            val targetMethod = navigator.navigate(dismissQueueIndex).stop() // MutableMethod

            // Construir el código smali usando la clase del método destino
            val smaliInstructions = """
                if-eqz v0, :ignore
                invoke-virtual {v0}, ${targetMethod.definingClass}->${targetMethod.name}()V
                :ignore
                return-void
            """.trimIndent()

            // Llamar a la función auxiliar (asumimos que está adaptada a la nueva API)
            addStaticFieldToExtension(
                EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR,
                "dismissQueue",
                "dismissQueueClass",
                targetMethod.definingClass,
                smaliInstructions
            )
        }
    }
}