/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.utils.playertype

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.extenre.patches.shared.litho.addLithoFilter
import com.extenre.patches.shared.litho.lithoFilterPatch
import com.extenre.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import com.extenre.patches.youtube.utils.extension.Constants.SHARED_PATH
import com.extenre.patches.youtube.utils.extension.Constants.UTILS_PATH
import com.extenre.patches.youtube.utils.extension.sharedExtensionPatch
import com.extenre.patches.youtube.utils.fix.litho.lithoLayoutPatch
import com.extenre.patches.youtube.utils.resourceid.reelWatchPlayer
import com.extenre.patches.youtube.utils.resourceid.sharedResourceIdPatch
import com.extenre.util.addInstructionsAtControlFlowLabel
import com.extenre.util.addStaticFieldToExtension
import com.extenre.util.findMethodOrThrow
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstLiteralInstructionOrThrow
import com.extenre.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_PLAYER_TYPE_HOOK_CLASS_DESCRIPTOR =
    "$UTILS_PATH/PlayerTypeHookPatch;"

private const val EXTENSION_ROOT_VIEW_HOOK_CLASS_DESCRIPTOR =
    "$SHARED_PATH/RootView;"

private const val EXTENSION_ROOT_VIEW_TOOLBAR_INTERFACE =
    "$SHARED_PATH/RootView${'$'}AppCompatToolbarPatchInterface;"

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/LayoutReloadObserverFilter;"

val playerTypeHookPatch = bytecodePatch(
    name = "player-type-hook-patch",
    description = "playerTypeHookPatch"
) {
    dependsOn(
        sharedExtensionPatch,
        sharedResourceIdPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
    )

    execute {

        // region patch for set ad progress text visibility

        adProgressTextViewVisibilityFingerprint.mutableMethodOrThrow().apply {
            val index =
                indexOfAdProgressTextViewVisibilityInstruction(this)
            val register =
                getInstruction<FiveRegisterInstruction>(index).registerD

            addInstructionsAtControlFlowLabel(
                index,
                "invoke-static { v$register }, " +
                        EXTENSION_ROOT_VIEW_HOOK_CLASS_DESCRIPTOR +
                        "->setAdProgressTextVisibility(I)V"
            )
        }

        // endregion

        // region patch for set context

        componentHostFingerprint.mutableMethodOrThrow().apply {
            val index = indexOfGetContextInstruction(this)
            val register =
                getInstruction<TwoRegisterInstruction>(index).registerA

            addInstructionsAtControlFlowLabel(
                index + 1,
                "invoke-static { v$register }, " +
                        EXTENSION_ROOT_VIEW_HOOK_CLASS_DESCRIPTOR +
                        "->setContext(Landroid/content/Context;)V"
            )
        }

        // endregion

        // region patch for set player type

        playerTypeFingerprint.mutableMethodOrThrow().addInstruction(
            0,
            "invoke-static {p1}, " +
                    "$EXTENSION_PLAYER_TYPE_HOOK_CLASS_DESCRIPTOR->setPlayerType(Ljava/lang/Enum;)V"
        )

        // endregion

        // region patch for set shorts player state

        reelWatchPagerFingerprint.mutableMethodOrThrow().apply {
            val literIndex = indexOfFirstLiteralInstructionOrThrow(reelWatchPlayer) + 2
            val registerIndex = indexOfFirstInstructionOrThrow(literIndex) {
                opcode == Opcode.MOVE_RESULT_OBJECT
            }
            val viewRegister = getInstruction<OneRegisterInstruction>(registerIndex).registerA

            addInstruction(
                registerIndex + 1,
                "invoke-static {v$viewRegister}, " +
                        "$EXTENSION_PLAYER_TYPE_HOOK_CLASS_DESCRIPTOR->onShortsCreate(Landroid/view/View;)V"
            )
        }

        // endregion

        // region patch for set video state

        val videoStateMatch = videoStateFingerprint.matchOrThrow()
        val videoStateMethod = videoStateMatch.method
        val videoStateClassDef = videoStateMatch.classDef
        val videoStateMutableMethod = proxy(videoStateClassDef).mutableClass.methods.first {
            MethodUtil.methodSignaturesMatch(it, videoStateMethod)
        }
        videoStateMutableMethod.apply {
            val endIndex = videoStateMatch.patternMatch!!.startIndex + 1
            val videoStateFieldName =
                getInstruction<ReferenceInstruction>(endIndex).reference

            addInstructions(
                0, """
                        iget-object v0, p1, $videoStateFieldName  # copyvideoState parameter field
                        invoke-static {v0}, $EXTENSION_PLAYER_TYPE_HOOK_CLASS_DESCRIPTOR->setVideoState(Ljava/lang/Enum;)V
                        """
            )
        }

        // endregion

        // region patch for hook browse id

        val browseIdClassMatch = browseIdClassFingerprint.matchOrThrow()
        val browseIdClassMethod = browseIdClassMatch.method
        val browseIdClassClassDef = browseIdClassMatch.classDef
        browseIdClassMethod.apply {
            val targetIndex = indexOfFirstStringInstructionOrThrow("VL") - 1
            val targetClass = getInstruction(targetIndex)
                .getReference<FieldReference>()
                ?.definingClass
                ?: throw PatchException("Could not find browseId class")

            // Reemplazar findmutableMethodOrThrow por búsqueda manual
            run {
                val method = findMethodOrThrow(targetClass)
                val classDef = classes.find { it.type == targetClass }
                    ?: throw PatchException("Class not found: $targetClass")
                val mutableMethod = proxy(classDef).mutableClass.methods.first {
                    MethodUtil.methodSignaturesMatch(it, method)
                }
                mutableMethod.apply {
                    val browseIdFieldReference = getInstruction<ReferenceInstruction>(
                        indexOfFirstInstructionOrThrow(Opcode.IPUT_OBJECT)
                    ).reference
                    val browseIdFieldName = (browseIdFieldReference as FieldReference).name

                    val smaliInstructions =
                        """
                            if-eqz v0, :ignore
                            iget-object v0, v0, $definingClass->$browseIdFieldName:Ljava/lang/String;
                            if-eqz v0, :ignore
                            return-object v0
                            :ignore
                            const-string v0, ""
                            return-object v0
                            """

                    addStaticFieldToExtension(
                        EXTENSION_ROOT_VIEW_HOOK_CLASS_DESCRIPTOR,
                        "getBrowseId",
                        "browseIdClass",
                        definingClass,
                        smaliInstructions
                    )
                }
            }
        }

        // endregion

        // region patch for hook search bar

        searchQueryClassFingerprint.mutableMethodOrThrow().apply {
            val searchQueryIndex = indexOfStringIsEmptyInstruction(this) - 1
            val searchQueryFieldReference =
                getInstruction<ReferenceInstruction>(searchQueryIndex).reference
            val searchQueryClass = (searchQueryFieldReference as FieldReference).definingClass

            // Reemplazar findmutableMethodOrThrow por búsqueda manual
            run {
                val method = findMethodOrThrow(searchQueryClass)
                val classDef = classes.find { it.type == searchQueryClass }
                    ?: throw PatchException("Class not found: $searchQueryClass")
                val mutableMethod = proxy(classDef).mutableClass.methods.first {
                    MethodUtil.methodSignaturesMatch(it, method)
                }
                mutableMethod.apply {
                    val smaliInstructions =
                        """
                        if-eqz v0, :ignore
                        iget-object v0, v0, $searchQueryFieldReference
                        if-eqz v0, :ignore
                        return-object v0
                        :ignore
                        const-string v0, ""
                        return-object v0
                        """

                    addStaticFieldToExtension(
                        EXTENSION_ROOT_VIEW_HOOK_CLASS_DESCRIPTOR,
                        "getSearchQuery",
                        "searchQueryClass",
                        definingClass,
                        smaliInstructions
                    )
                }
            }
        }

        // endregion

        // region patch for hook back button visibility

        toolbarLayoutFingerprint.mutableMethodOrThrow().apply {
            val index = indexOfMainCollapsingToolbarLayoutInstruction(this)
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            addInstruction(
                index + 1,
                "invoke-static { v$register }, $EXTENSION_ROOT_VIEW_HOOK_CLASS_DESCRIPTOR->setToolbar(Landroid/widget/FrameLayout;)V"
            )
        }

        // Add interface for extensions code to call obfuscated methods.
        val appCompatToolbarBackButtonMatch = appCompatToolbarBackButtonFingerprint.matchOrThrow()
        appCompatToolbarBackButtonMatch.classDef.apply {
            interfaces.add(EXTENSION_ROOT_VIEW_TOOLBAR_INTERFACE)

            val definingClass = type
            val obfuscatedMethodName = appCompatToolbarBackButtonMatch.originalMethod.name
            val returnType = "Landroid/graphics/drawable/Drawable;"

            methods.add(
                ImmutableMethod(
                    definingClass,
                    "patch_getToolbarIcon",
                    listOf(),
                    returnType,
                    AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                    null,
                    null,
                    MutableMethodImplementation(2),
                ).toMutable().apply {
                    addInstructions(
                        0,
                        """
                                 invoke-virtual { p0 }, $definingClass->$obfuscatedMethodName()$returnType
                                 move-result-object v0
                                 return-object v0
                             """
                    )
                }
            )
        }

        // endregion

        addLithoFilter(FILTER_CLASS_DESCRIPTOR)

    }
}