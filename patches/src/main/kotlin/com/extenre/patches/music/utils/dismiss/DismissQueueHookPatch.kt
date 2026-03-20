/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.utils.dismiss

import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.music.utils.extension.Constants.EXTENSION_PATH
import com.extenre.util.addStaticFieldToExtension
import com.extenre.util.findMethodOrThrow
import com.extenre.util.fingerprint.methodOrThrow
import com.extenre.util.getWalkerMethod
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/utils/VideoUtils;"

@Suppress("unused")
val dismissQueueHookPatch = bytecodePatch(
    name = "dismiss-queue-hook",
    description = "dismissQueueHookPatch"
) {

    execute {

        dismissQueueFingerprint.methodOrThrow().apply {
            val dismissQueueIndex = indexOfDismissQueueInstruction(this)

            getWalkerMethod(dismissQueueIndex).apply {
                val smaliInstructions =
                    """
                        if-eqz v0, :ignore
                        invoke-virtual {v0}, $definingClass->$name()V
                        :ignore
                        return-void
                        """

                // Reemplazar findmutableMethodOrThrow por búsqueda manual
                val method = findMethodOrThrow(EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR)
                val classDef = classes.find { it.type == EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR }
                    ?: throw PatchException("Class not found: $EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR")
                val mutableMethod = proxy(classDef).mutableClass.methods.first {
                    MethodUtil.methodSignaturesMatch(it, method)
                }
                mutableMethod.addStaticFieldToExtension(
                    EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR,
                    "dismissQueue",
                    "dismissQueueClass",
                    definingClass,
                    smaliInstructions
                )
            }
        }

    }
}