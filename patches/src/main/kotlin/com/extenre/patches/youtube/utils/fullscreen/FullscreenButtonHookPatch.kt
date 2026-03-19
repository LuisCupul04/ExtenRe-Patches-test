/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.utils.fullscreen

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod
import com.extenre.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import com.extenre.patches.youtube.utils.extension.sharedExtensionPatch
import com.extenre.patches.youtube.utils.playservice.is_20_02_or_greater
import com.extenre.patches.youtube.utils.playservice.versionCheckPatch
import com.extenre.util.addStaticFieldToExtension
import com.extenre.util.findMethodOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.getWalkerMethod
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/utils/VideoUtils;"

internal var enterFullscreenMethods = mutableListOf<MutableMethod>()

val fullscreenButtonHookPatch = bytecodePatch(
    name = "fullscreen-Button-Hook-Patch",
    description = "fullscreenButtonHookPatch"
) {

    dependsOn(
        sharedExtensionPatch,
        versionCheckPatch,
    )

    execute {
        fun getParameters(): Pair<MutableMethod, String> {
            nextGenWatchLayoutFullscreenModeFingerprint.mutableMethodOrThrow().apply {
                val methodIndex = indexOfFirstInstructionReversedOrThrow {
                    opcode == Opcode.INVOKE_DIRECT &&
                            getReference<MethodReference>()?.parameterTypes?.size == 2
                }
                val fieldIndex =
                    indexOfFirstInstructionReversedOrThrow(methodIndex, Opcode.IGET_OBJECT)
                val fullscreenActionClass =
                    (getInstruction<ReferenceInstruction>(fieldIndex).reference as FieldReference).type

                if (is_20_02_or_greater) {
                    val setAnimatorListenerIndex =
                        indexOfFirstInstructionOrThrow(methodIndex, Opcode.INVOKE_VIRTUAL)
                    getWalkerMethod(setAnimatorListenerIndex).apply {
                        val addListenerIndex = indexOfFirstInstructionOrThrow {
                            opcode == Opcode.INVOKE_VIRTUAL &&
                                    getReference<MethodReference>()?.name == "addListener"
                        }
                        val animatorListenerAdapterClass = getInstruction<ReferenceInstruction>(
                            indexOfFirstInstructionReversedOrThrow(
                                addListenerIndex,
                                Opcode.NEW_INSTANCE
                            )
                        ).reference.toString()
                        // Reemplazar findmutableMethodOrThrow por búsqueda manual
                        val method = findMethodOrThrow(animatorListenerAdapterClass) { parameters.isEmpty() }
                        val classDef = classes.find { it.type == animatorListenerAdapterClass }
                            ?: throw PatchException("Class not found: $animatorListenerAdapterClass")
                        val mutableMethod = proxy(classDef).mutableClass.methods.first {
                            MethodUtil.methodSignaturesMatch(it, method)
                        }
                        return Pair(mutableMethod, fullscreenActionClass)
                    }
                } else {
                    val animatorListenerClass =
                        (getInstruction<ReferenceInstruction>(methodIndex).reference as MethodReference).definingClass
                    // Reemplazar findmutableMethodOrThrow por búsqueda manual
                    val method = findMethodOrThrow(animatorListenerClass) { parameters == listOf("I") }
                    val classDef = classes.find { it.type == animatorListenerClass }
                        ?: throw PatchException("Class not found: $animatorListenerClass")
                    val mutableMethod = proxy(classDef).mutableClass.methods.first {
                        MethodUtil.methodSignaturesMatch(it, method)
                    }
                    return Pair(mutableMethod, fullscreenActionClass)
                }
            }
        }

        val (animatorListenerMethod, fullscreenActionClass) = getParameters()

        val (enterFullscreenReference, exitFullscreenReference, opcodeName) =
            with(animatorListenerMethod) {
                val enterFullscreenIndex = indexOfFirstInstructionOrThrow {
                    val reference = getReference<MethodReference>()
                    reference?.returnType == "V" &&
                            reference.definingClass == fullscreenActionClass &&
                            reference.parameterTypes.isEmpty()
                }
                val exitFullscreenIndex = indexOfFirstInstructionReversedOrThrow {
                    val reference = getReference<MethodReference>()
                    reference?.returnType == "V" &&
                            reference.definingClass == fullscreenActionClass &&
                            reference.parameterTypes.isEmpty()
                }

                val enterFullscreenReference =
                    getInstruction<ReferenceInstruction>(enterFullscreenIndex).reference
                val exitFullscreenReference =
                    getInstruction<ReferenceInstruction>(exitFullscreenIndex).reference
                val opcode = getInstruction(enterFullscreenIndex).opcode

                val enterFullscreenClass =
                    (enterFullscreenReference as MethodReference).definingClass

                if (opcode == Opcode.INVOKE_INTERFACE) {
                    classes.forEach { classDef ->
                        if (enterFullscreenMethods.size >= 2)
                            return@forEach
                        if (!classDef.interfaces.contains(enterFullscreenClass))
                            return@forEach

                        val enterFullscreenMethod =
                            proxy(classDef)
                                .mutableClass
                                .methods
                                .find { method -> method.name == enterFullscreenReference.name }
                                ?: throw PatchException("No matching classes: $enterFullscreenClass")

                        enterFullscreenMethods.add(enterFullscreenMethod)
                    }
                } else {
                    // Reemplazar findmutableMethodOrThrow por búsqueda manual
                    val method = findMethodOrThrow(enterFullscreenClass) {
                        name == enterFullscreenReference.name
                    }
                    val classDef = classes.find { it.type == enterFullscreenClass }
                        ?: throw PatchException("Class not found: $enterFullscreenClass")
                    val mutableMethod = proxy(classDef).mutableClass.methods.first {
                        MethodUtil.methodSignaturesMatch(it, method)
                    }
                    enterFullscreenMethods.add(mutableMethod)
                }

                Triple(
                    enterFullscreenReference,
                    exitFullscreenReference,
                    opcode.name
                )
            }

        nextGenWatchLayoutConstructorFingerprint.mutableMethodOrThrow().apply {
            val targetIndex = indexOfFirstInstructionReversedOrThrow {
                opcode == Opcode.CHECK_CAST &&
                        getReference<TypeReference>()?.type == fullscreenActionClass
            }
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "sput-object v$targetRegister, $EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR->fullscreenActionClass:$fullscreenActionClass"
            )

            val enterFullscreenModeSmaliInstructions =
                """
                    if-eqz v0, :ignore
                    $opcodeName {v0}, $enterFullscreenReference
                    :ignore
                    return-void
                    """

            val exitFullscreenModeSmaliInstructions =
                """
                    if-eqz v0, :ignore
                    $opcodeName {v0}, $exitFullscreenReference
                    :ignore
                    return-void
                    """

            addStaticFieldToExtension(
                EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR,
                "enterFullscreenMode",
                "fullscreenActionClass",
                fullscreenActionClass,
                enterFullscreenModeSmaliInstructions,
                false
            )

            addStaticFieldToExtension(
                EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR,
                "exitFullscreenMode",
                "fullscreenActionClass",
                fullscreenActionClass,
                exitFullscreenModeSmaliInstructions,
                false
            )
        }
    }
}
