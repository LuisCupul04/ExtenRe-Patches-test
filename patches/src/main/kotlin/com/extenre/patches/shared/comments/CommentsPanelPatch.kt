/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.shared.comments

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.patch.resourcePatch
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.extenre.patches.shared.extension.Constants.PATCHES_PATH
import com.extenre.patches.shared.mapping.ResourceType.ID
import com.extenre.patches.shared.mapping.getResourceId
import com.extenre.patches.shared.mapping.resourceMappingPatch
import com.extenre.util.REGISTER_TEMPLATE_REPLACEMENT
import com.extenre.util.addInstructionsAtControlFlowLabel
import com.extenre.util.findFreeRegister
import com.extenre.util.findMethodOrThrow
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.methodCall
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.injectLiteralInstructionViewCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.util.MethodUtil

var informationButton = -1L
    private set
var modernTitle = -1L
    private set
var title = -1L
    private set

private val commentsPanelResourcePatch = resourcePatch(
    name = "comments-panel-resource-patch",
    description = "Resource patch for comments panel"
) {
    dependsOn(resourceMappingPatch)

    execute {
        informationButton = getResourceId(ID, "information_button")
        modernTitle = getResourceId(ID, "modern_title")
        title = getResourceId(ID, "title")
    }
}

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/CommentsPanelPatch;"

val commentsPanelPatch = bytecodePatch(
    name = "comments-Panel-Patch",
    description = "commentsPanelPatch"
) {
    dependsOn(commentsPanelResourcePatch)

    execute {
        // Method to find the engagement panel id.
        val (engagementPanelIdMethodCall, engagementPanelMessageClass) =
            with(engagementPanelIdFingerprint.mutableMethodOrThrow()) {
                Pair(methodCall(), parameterTypes.first().toString())
            }

        // Method that finds the RecyclerView to which comments will be bound.
        val recyclerViewOptionalMethodCall = recyclerViewOptionalFingerprint
            .mutableMethodOrThrow(engagementPanelRecyclerViewFingerprint)
            .methodCall()

        engagementPanelRecyclerViewFingerprint.matchOrThrow().let { result ->
            result.method.apply {
                val setRecyclerViewMethodName = "patch_setRecyclerView"
                val insertIndex = indexOfIfPresentInstruction(this) + 1

                // Find the index of the class required to get the engagement panel id.
                val engagementPanelMessageIndex = indexOfFirstInstructionOrThrow(insertIndex) {
                    getReference<MethodReference>()?.parameterTypes?.firstOrNull() == engagementPanelMessageClass
                }
                val engagementPanelMessageRegister =
                    getInstruction<FiveRegisterInstruction>(engagementPanelMessageIndex).let { instruction ->
                        if (getInstruction(engagementPanelMessageIndex).opcode == Opcode.INVOKE_STATIC)
                            instruction.registerC
                        else // YouTube Music 6.20.51
                            instruction.registerD
                    }
                val freeRegister = findFreeRegister(insertIndex, false)

                addInstructionsAtControlFlowLabel(
                    insertIndex, """
                        move-object/from16 v$freeRegister, p0
                        invoke-direct { v$freeRegister, v$engagementPanelMessageRegister }, $definingClass->$setRecyclerViewMethodName($engagementPanelMessageClass)V
                        """
                )

                result.classDef.methods.add(
                    ImmutableMethod(
                        result.classDef.type,
                        setRecyclerViewMethodName,
                        listOf(
                            ImmutableMethodParameter(
                                engagementPanelMessageClass,
                                annotations,
                                "engagementPanelMessageClass"
                            )
                        ),
                        "V",
                        AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                        annotations,
                        null,
                        MutableMethodImplementation(4),
                    ).toMutable().apply {
                        addInstructionsWithLabels(
                            0,
                            """
                                    invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->isCommentsScrollTopEnabled()Z
                                    move-result v0
                                    if-eqz v0, :ignore
                                    
                                    # Get engagement panel id.
                                    invoke-static { p1 }, $engagementPanelIdMethodCall
                                    move-result-object v0
                                    
                                    # Check if engagement panel id is not null.
                                    if-eqz v0, :ignore

                                    const-string v1, "comment"
                                    invoke-virtual { v0, v1 }, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                                    move-result v0
                                    
                                    # Check if engagement panel id is a comment.
                                    if-eqz v0, :ignore
                                    
                                    invoke-virtual { p0 }, $recyclerViewOptionalMethodCall
                                    move-result-object v0
                                    invoke-virtual { v0 }, Lj${'$'}/util/Optional;->isPresent()Z
                                    move-result v1
                                    
                                    # Check if recycler view is not null.
                                    if-eqz v1, :ignore
                                    invoke-virtual { v0 }, Lj${'$'}/util/Optional;->get()Ljava/lang/Object;
                                    move-result-object v0
                                    check-cast v0, Landroid/support/v7/widget/RecyclerView;
                                    invoke-static { v0 }, $EXTENSION_CLASS_DESCRIPTOR->onCommentsCreate(Landroid/support/v7/widget/RecyclerView;)V
                                    
                                    :ignore
                                    return-void
                                    """,
                        )
                    }
                )
            }
        }

        mapOf(
            informationButton to "hideInformationButton",
            modernTitle to "setContentHeader",
            title to "setContentHeader"
        ).forEach { (literal, methodName) ->
            engagementPanelTitleFingerprint
                .mutableMethodOrThrow(engagementPanelTitleParentFingerprint)
                .injectLiteralInstructionViewCall(
                    literal,
                    "invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $EXTENSION_CLASS_DESCRIPTOR->$methodName(Landroid/view/View;)V"
                )
        }

        // Reemplazar findmutableMethodOrThrow por búsqueda manual
        run {
            val method = findMethodOrThrow(EXTENSION_CLASS_DESCRIPTOR) {
                name == "smoothScrollToPosition"
            }
            val classDef = classes.find { it.type == EXTENSION_CLASS_DESCRIPTOR }
                ?: throw PatchException("Class not found: $EXTENSION_CLASS_DESCRIPTOR")
            val mutableMethod = proxy(classDef).mutableClass.methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.addInstruction(
                0,
                "invoke-virtual {p0, p1}, ${recyclerViewSmoothScrollToPositionFingerprint.methodCall()}"
            )
        }
    }
}
