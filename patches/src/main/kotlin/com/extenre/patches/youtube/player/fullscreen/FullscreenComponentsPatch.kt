/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.player.fullscreen

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.extensions.InstructionExtensions.replaceInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.smali.ExternalLabel
import com.extenre.patches.shared.litho.addLithoFilter
import com.extenre.patches.shared.litho.lithoFilterPatch
import com.extenre.patches.shared.mainactivity.onConfigurationChangedMethod
import com.extenre.patches.shared.mainactivity.onStartMethod
import com.extenre.patches.shared.mainactivity.onStopMethod
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import com.extenre.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.extension.Constants.PLAYER_PATH
import com.extenre.patches.youtube.utils.fix.litho.lithoLayoutPatch
import com.extenre.patches.youtube.utils.fullscreen.fullscreenButtonHookPatch
import com.extenre.patches.youtube.utils.indexOfFocusableInTouchModeInstruction
import com.extenre.patches.youtube.utils.layoutConstructorFingerprint
import com.extenre.patches.youtube.utils.mainactivity.mainActivityResolvePatch
import com.extenre.patches.youtube.utils.patch.PatchList.FULLSCREEN_COMPONENTS
import com.extenre.patches.youtube.utils.playertype.playerTypeHookPatch
import com.extenre.patches.youtube.utils.playservice.is_18_42_or_greater
import com.extenre.patches.youtube.utils.playservice.is_19_41_or_greater
import com.extenre.patches.youtube.utils.playservice.versionCheckPatch
import com.extenre.patches.youtube.utils.resourceid.autoNavPreviewStub
import com.extenre.patches.youtube.utils.resourceid.fullScreenEngagementPanel
import com.extenre.patches.youtube.utils.resourceid.quickActionsElementContainer
import com.extenre.patches.youtube.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.patches.youtube.utils.youtubeControlsOverlayFingerprint
import com.extenre.patches.youtube.video.information.hookBackgroundPlayVideoInformation
import com.extenre.patches.youtube.video.information.videoEndMethod
import com.extenre.patches.youtube.video.information.videoInformationPatch
import com.extenre.util.addInstructionsAtControlFlowLabel
import com.extenre.util.fingerprint.methodOrThrow
import com.extenre.util.fingerprint.mutableClassOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstLiteralInstructionOrThrow
import com.extenre.util.indexOfFirstStringInstructionOrThrow
import com.extenre.util.updatePatchStatus
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/QuickActionFilter;"

private const val EXTENSION_ENTER_FULLSCREEN_CLASS_DESCRIPTOR =
    "$PLAYER_PATH/EnterFullscreenPatch;"

private const val EXTENSION_EXIT_FULLSCREEN_CLASS_DESCRIPTOR =
    "$PLAYER_PATH/ExitFullscreenPatch;"

@Suppress("unused")
val fullscreenComponentsPatch = bytecodePatch(
    name = FULLSCREEN_COMPONENTS.key,
    description = "${FULLSCREEN_COMPONENTS.title}: ${FULLSCREEN_COMPONENTS.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        playerTypeHookPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        mainActivityResolvePatch,
        fullscreenButtonHookPatch,
        videoInformationPatch,
        sharedResourceIdPatch,
        versionCheckPatch,
    )

    execute {
        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: PLAYER",
            "SETTINGS: FULLSCREEN_COMPONENTS"
        )

        // region patch for disable engagement panel
        fullScreenEngagementPanelFingerprint.mutableMethodOrThrow().apply {
            val literalIndex = indexOfFirstLiteralInstructionOrThrow(fullScreenEngagementPanel)
            val targetIndex = indexOfFirstInstructionOrThrow(literalIndex, Opcode.CHECK_CAST)
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, " +
                        "$PLAYER_CLASS_DESCRIPTOR->disableEngagementPanels(Landroidx/coordinatorlayout/widget/CoordinatorLayout;)V"
            )
        }

        playerTitleViewFingerprint.mutableMethodOrThrow().apply {
            val insertIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.name == "addView"
            }
            val insertReference =
                getInstruction<ReferenceInstruction>(insertIndex).reference.toString()
            if (!insertReference.startsWith("Landroid/widget/FrameLayout;"))
                throw PatchException("Reference does not match: $insertReference")
            val insertInstruction = getInstruction<FiveRegisterInstruction>(insertIndex)

            replaceInstruction(
                insertIndex,
                "invoke-static { v${insertInstruction.registerC}, v${insertInstruction.registerD} }, " +
                        "$PLAYER_CLASS_DESCRIPTOR->showVideoTitleSection(Landroid/widget/FrameLayout;Landroid/view/View;)V"
            )
        }
        // endregion

        // region patch for enter fullscreen
        mapOf(
            onStartMethod to "onAppForegrounded",
            onStopMethod to "onAppBackgrounded"
        ).forEach { (method, name) ->
            method.addInstruction(
                0,
                "invoke-static {}, $EXTENSION_ENTER_FULLSCREEN_CLASS_DESCRIPTOR->$name()V"
            )
        }

        hookBackgroundPlayVideoInformation("$EXTENSION_ENTER_FULLSCREEN_CLASS_DESCRIPTOR->enterFullscreen(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
        // endregion

        // region patch for exit fullscreen
        videoEndMethod.apply {
            addInstructionsAtControlFlowLabel(
                implementation!!.instructions.lastIndex,
                "invoke-static {}, $EXTENSION_EXIT_FULLSCREEN_CLASS_DESCRIPTOR->endOfVideoReached()V",
            )
        }
        // endregion

        // region patch for hide autoplay preview
        layoutConstructorFingerprint.mutableMethodOrThrow().apply {
            val constIndex = indexOfFirstLiteralInstructionOrThrow(autoNavPreviewStub)
            val constRegister = getInstruction<OneRegisterInstruction>(constIndex).registerA
            val jumpIndex =
                indexOfFirstInstructionOrThrow(constIndex + 2, Opcode.INVOKE_VIRTUAL) + 1

            addInstructionsWithLabels(
                constIndex, """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideAutoPlayPreview()Z
                    move-result v$constRegister
                    if-nez v$constRegister, :hidden
                    """, ExternalLabel("hidden", getInstruction(jumpIndex))
            )
        }
        // endregion

        // region patch for hide related video overlay
        relatedEndScreenResultsFingerprint.mutableClassOrThrow().let {
            it.methods.find { method -> method.parameters == listOf("I", "Z", "I") }
                ?.apply {
                    addInstructionsWithLabels(
                        0, """
                            invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideRelatedVideoOverlay()Z
                            move-result v0
                            if-eqz v0, :show
                            return-void
                            """, ExternalLabel("show", getInstruction(0))
                    )
                } ?: throw PatchException("Could not find targetMethod")
        }
        // endregion

        // region patch for quick actions
        quickActionsElementSyntheticFingerprint.mutableMethodOrThrow().apply {
            val containerCalls = implementation!!.instructions.withIndex()
                .filter { instruction ->
                    (instruction.value as? WideLiteralInstruction)?.wideLiteral == quickActionsElementContainer
                }
            val constIndex = containerCalls.elementAt(containerCalls.size - 1).index

            val checkCastIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.CHECK_CAST)
            val insertRegister =
                getInstruction<OneRegisterInstruction>(checkCastIndex).registerA

            addInstruction(
                checkCastIndex + 1,
                "invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->setQuickActionMargin(Landroid/view/View;)V"
            )

            addInstruction(
                checkCastIndex,
                "invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->hideQuickActions(Landroid/view/View;)V"
            )
        }

        updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "QuickActions")
        // endregion

        // region patch for compact control overlay
        youtubeControlsOverlayFingerprint.mutableMethodOrThrow().apply {
            val targetIndex = indexOfFocusableInTouchModeInstruction(this)
            val walkerIndex = indexOfFirstInstructionOrThrow(targetIndex) {
                val reference = getReference<MethodReference>()
                opcode == Opcode.INVOKE_STATIC &&
                        reference?.returnType == "Z" &&
                        reference.parameterTypes.size == 1
            }

            // Obtener el método llamado en esa instrucción
            val invokeInstruction = getInstruction<ReferenceInstruction>(walkerIndex)
            val methodRef = invokeInstruction.reference as? MethodReference
                ?: throw PatchException("No method reference at index $walkerIndex")
            val targetMethod = mutableClassDefBy(methodRef.definingClass).methods.first {
                it.name == methodRef.name && it.parameterTypes == methodRef.parameterTypes
            }
            targetMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val targetRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->enableCompactControlsOverlay(Z)Z
                        move-result v$targetRegister
                        """
                )
            }
        }
        // endregion

        // region patch for disable landscape mode
        onConfigurationChangedMethod.apply {
            val walkerIndex = indexOfFirstInstructionOrThrow {
                val reference = getReference<MethodReference>()
                reference?.parameterTypes == listOf("Landroid/content/res/Configuration;") &&
                        reference.returnType == "V" &&
                        reference.name != "onConfigurationChanged"
            }

            val invokeInstruction = getInstruction<ReferenceInstruction>(walkerIndex)
            val methodRef = invokeInstruction.reference as? MethodReference
                ?: throw PatchException("No method reference at index $walkerIndex")
            val walkerMethod = mutableClassDefBy(methodRef.definingClass).methods.first {
                it.name == methodRef.name && it.parameterTypes == methodRef.parameterTypes
            }

            val constructorMethod = mutableClassDefBy(walkerMethod.definingClass).methods.find {
                it.name == "<init>" && it.parameterTypes == listOf("Landroid/app/Activity;")
            } ?: throw PatchException("Constructor not found in ${walkerMethod.definingClass}")

            arrayOf(
                walkerMethod,
                constructorMethod
            ).forEach { method ->
                method.apply {
                    val index = indexOfFirstInstructionOrThrow {
                        val reference = getReference<MethodReference>()
                        reference?.parameterTypes == listOf("Landroid/content/Context;") &&
                                reference.returnType == "Z"
                    } + 1
                    val register = getInstruction<OneRegisterInstruction>(index).registerA

                    addInstructions(
                        index + 1, """
                            invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->disableLandScapeMode(Z)Z
                            move-result v$register
                            """
                    )
                }
            }
        }
        // endregion

        // region patch for keep landscape mode
        if (is_18_42_or_greater && !is_19_41_or_greater) {
            landScapeModeConfigFingerprint.mutableMethodOrThrow().apply {
                val insertIndex = implementation!!.instructions.lastIndex
                val insertRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->keepFullscreen(Z)Z
                        move-result v$insertRegister
                        """
                )
            }
            broadcastReceiverFingerprint.mutableMethodOrThrow().apply {
                val stringIndex =
                    indexOfFirstStringInstructionOrThrow("android.intent.action.SCREEN_ON")
                val insertIndex =
                    indexOfFirstInstructionOrThrow(stringIndex, Opcode.IF_EQZ) + 1

                addInstruction(
                    insertIndex,
                    "invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->setScreenOn()V"
                )
            }

            settingArray += "SETTINGS: KEEP_LANDSCAPE_MODE"
        }
        // endregion

        addLithoFilter(FILTER_CLASS_DESCRIPTOR)

        // region add settings
        addPreference(settingArray, FULLSCREEN_COMPONENTS)
        // endregion
    }
}