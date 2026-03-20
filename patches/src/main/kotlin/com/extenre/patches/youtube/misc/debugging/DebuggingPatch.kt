/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.misc.debugging

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.smali.ExternalLabel
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.extension.Constants.UTILS_PATH
import com.extenre.patches.youtube.utils.patch.PatchList.ENABLE_DEBUG_LOGGING
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.findMethodOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/DebuggingPatch;"

@Suppress("unused")
val debuggingPatch = bytecodePatch(
    name = ENABLE_DEBUG_LOGGING.key,
    description = "${ENABLE_DEBUG_LOGGING.title}: ${ENABLE_DEBUG_LOGGING.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        // region patch for debug logging

        val (debugString, debugBooleanField) = with(
            debuggingFingerprint.mutableMethodOrThrow()
        ) {
            val stringIndex = indexOfFirstInstructionOrThrow(Opcode.CONST_STRING)
            val debugString = getInstruction<ReferenceInstruction>(stringIndex).reference.toString()
            val booleanFieldIndex = indexOfFirstInstructionOrThrow(stringIndex) {
                opcode == Opcode.SGET_BOOLEAN &&
                        getReference<FieldReference>()?.type == "Z"
            }
            val debugBooleanField =
                getInstruction<ReferenceInstruction>(booleanFieldIndex).reference

            Pair(debugString, debugBooleanField)
        }

        debuggingFingerprint.mutableClassOrThrow().let { mutableClass ->
            val staticField = mutableClass.staticFields.find { field ->
                field.type == "Z"
            } ?: throw PatchException("Could not find static boolean field")

            val getterMethod = mutableClass.methods.find { method ->
                method.returnType == "Z" && method.parameters.isEmpty()
            } ?: throw PatchException("Could not find getter method")

            // Reemplazar originalmutableMethodOrThrow por búsqueda manual
            val method = findMethodOrThrow(EXTENSION_CLASS_DESCRIPTOR) {
                name == "getDebugState"
            }
            val classDef = classes.find { it.type == EXTENSION_CLASS_DESCRIPTOR }
                ?: throw PatchException("Class not found: $EXTENSION_CLASS_DESCRIPTOR")
            val mutableMethod = proxy(classDef).mutableClass.methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.addInstructions(
                0, """
                    sget-object v0, $staticField
                    return v0
                    """
            )

            mutableClass.methods.add(getterMethod)
        }

        // endregion

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: ENABLE_DEBUG_LOGGING"
            ),
            ENABLE_DEBUG_LOGGING
        )

        // endregion
    }
}
