/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.utils.dismiss

import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.music.utils.extension.Constants.EXTENSION_PATH
import com.extenre.util.addStaticFieldToExtension
import com.extenre.util.fingerprint.methodOrThrow
import com.extenre.util.getWalkerMethod

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

                addStaticFieldToExtension(
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
