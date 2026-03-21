/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.misc.debugging

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.shared.WATCH_NEXT_RESPONSE_PROCESSING_DELAY_STRING
import com.extenre.patches.shared.playbackStartParametersConstructorFingerprint
import com.extenre.patches.shared.playbackStartParametersToStringFingerprint
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.MISC_PATH
import com.extenre.patches.youtube.utils.patch.PatchList.ENABLE_DEBUG_LOGGING
import com.extenre.patches.youtube.utils.playservice.is_19_16_or_greater
import com.extenre.patches.youtube.utils.playservice.versionCheckPatch
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.patches.youtube.utils.webview.webViewPatch
import com.extenre.util.findFieldFromToString
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$MISC_PATH/DebuggingPatch;"

@Suppress("unused")
val debuggingPatch = bytecodePatch(
    name = ENABLE_DEBUG_LOGGING.key,
    description = "${ENABLE_DEBUG_LOGGING.title}: ${ENABLE_DEBUG_LOGGING.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        webViewPatch,
        versionCheckPatch,
    )

    execute {
        var settingArray = arrayOf(
            "SETTINGS: DEBUGGING"
        )

        if (is_19_16_or_greater) {
            currentWatchNextResponseFingerprint
                .mutableMethodOrThrow(currentWatchNextResponseParentFingerprint)
                .addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->enableWatchNextProcessingDelay()Z
                        move-result v0
                        if-eqz v0, :ignore
                        const/4 v0, 0x0
                        return v0
                        :ignore
                        nop
                        """
                )

            val watchNextResponseProcessingDelayField = run {
                val match = playbackStartParametersToStringFingerprint.matchOrThrow()
                val method = match.method
                val classDef = match.classDef
                val mutableMethod = mutableClassDefBy(classDef.type).methods.first {
                    MethodUtil.methodSignaturesMatch(it, method)
                }
                mutableMethod.findFieldFromToString(WATCH_NEXT_RESPONSE_PROCESSING_DELAY_STRING)
            }

            playbackStartParametersConstructorFingerprint
                .mutableMethodOrThrow(playbackStartParametersToStringFingerprint)
                .apply {
                    val index = indexOfFirstInstructionReversedOrThrow {
                        opcode == Opcode.IPUT &&
                                getReference<FieldReference>() == watchNextResponseProcessingDelayField
                    }
                    val register = getInstruction<TwoRegisterInstruction>(index).registerA

                    addInstructions(
                        index, """
                            invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->getWatchNextProcessingDelay(I)I
                            move-result v$register
                            """
                    )
                }

            settingArray += "SETTINGS: WATCH_NEXT_PROCESSING_DELAY"
        }

        // region add settings
        addPreference(
            settingArray,
            ENABLE_DEBUG_LOGGING
        )
        // endregion
    }
}