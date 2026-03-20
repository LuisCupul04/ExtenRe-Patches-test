/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.shared.tracking

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.extensions.InstructionExtensions.replaceInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod
import com.extenre.patches.shared.extension.Constants.PATCHES_PATH
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/SanitizeUrlQueryPatch;"

internal fun MutableMethod.hookQueryParameters(index: Int) {
    val invokeInstruction = getInstruction(index) as FiveRegisterInstruction

    replaceInstruction(
        index,
        "invoke-static {v${invokeInstruction.registerC}, v${invokeInstruction.registerD}, v${invokeInstruction.registerE}}, " +
                "$EXTENSION_CLASS_DESCRIPTOR->stripQueryParameters(Landroid/content/Intent;Ljava/lang/String;Ljava/lang/String;)V"
    )
}

val baseSanitizeUrlQueryPatch = bytecodePatch(
    name = "base-Sanitize-Url-Query-Patch",
    description = "baseSanitizeUrlQueryPatch"
) {
    execute {
        val copyTextMatch = copyTextEndpointFingerprint.matchOrThrow()
        val method = copyTextMatch.method
        val classDef = copyTextMatch.classDef
        val mutableMethod = proxy(classDef).mutableClass.methods.first {
            MethodUtil.methodSignaturesMatch(it, method)
        }
        mutableMethod.apply {
            val targetIndex = copyTextMatch.patternMatch!!.startIndex
            val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

            addInstructions(
                targetIndex + 2, """
                        invoke-static {v$targetRegister}, $EXTENSION_CLASS_DESCRIPTOR->stripQueryParameters(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$targetRegister
                        """
            )
        }

        setOf(
            shareLinkFormatterFingerprint,
            systemShareLinkFormatterFingerprint
        ).forEach { fingerprint ->
            fingerprint.mutableMethodOrThrow().apply {
                for ((index, instruction) in implementation!!.instructions.withIndex()) {
                    if (instruction.opcode != Opcode.INVOKE_VIRTUAL)
                        continue

                    if ((instruction as ReferenceInstruction).reference.toString() != "Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;")
                        continue

                    if (getInstruction(index + 1).opcode != Opcode.GOTO)
                        continue

                    hookQueryParameters(index)
                }
            }
        }
    }
}
