/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.utils.playercontrols

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.patch.resourcePatch
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod
import com.extenre.patches.youtube.utils.extension.Constants.UTILS_PATH
import com.extenre.patches.youtube.utils.extension.sharedExtensionPatch
import com.extenre.patches.youtube.utils.playservice.is_19_25_or_greater
import com.extenre.patches.youtube.utils.playservice.is_19_36_or_greater
import com.extenre.patches.youtube.utils.playservice.is_20_19_or_greater
import com.extenre.patches.youtube.utils.playservice.is_20_20_or_greater
import com.extenre.patches.youtube.utils.playservice.is_20_28_or_greater
import com.extenre.patches.youtube.utils.playservice.is_20_30_or_greater
import com.extenre.patches.youtube.utils.playservice.versionCheckPatch
import com.extenre.patches.youtube.utils.resourceid.fullScreenButton
import com.extenre.patches.youtube.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.youtube.utils.youtubeControlsOverlayFingerprint
import com.extenre.util.copyXmlNode
import com.extenre.util.findElementByAttributeValueOrThrow
import com.extenre.util.fingerprint.injectLiteralInstructionBooleanCall
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstLiteralInstructionReversedOrThrow
import com.extenre.util.inputStreamFromBundledResourceOrThrow
import com.extenre.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

/**
 * Add a new top to the bottom of the YouTube player.
 *
 * @param resourceDirectoryName The name of the directory containing the hosting resource.
 */
@Suppress("KDocUnresolvedReference")
// Internal until this is modified to work with any patch (and not just SponsorBlock).
internal lateinit var addTopControl: (String, String, String) -> Unit
    private set

private var insertElementId = "@id/player_video_heading"

val playerControlsResourcePatch = resourcePatch {
    execute {
        addTopControl = { resourceDirectoryName, startElementId, endElementId ->
            val resourceFileName = "host/layout/youtube_controls_layout.xml"
            val hostingResourceStream = inputStreamFromBundledResourceOrThrow(
                resourceDirectoryName,
                resourceFileName,
            )

            val document = document("res/layout/youtube_controls_layout.xml")
            val androidId = "android:id"
            val androidLayoutToStartOf = "android:layout_toStartOf"

            "RelativeLayout".copyXmlNode(
                document(hostingResourceStream),
                document,
            ).use {
                val insertElement = document.childNodes.findElementByAttributeValueOrThrow(
                    androidId,
                    insertElementId,
                )
                val endElement = document.childNodes.findElementByAttributeValueOrThrow(
                    androidId,
                    endElementId,
                )

                val insertElementLayoutToStartOf =
                    insertElement.attributes.getNamedItem(androidLayoutToStartOf).nodeValue!!

                insertElement.attributes.getNamedItem(androidLayoutToStartOf).nodeValue =
                    startElementId
                endElement.attributes.getNamedItem(androidLayoutToStartOf).nodeValue =
                    insertElementLayoutToStartOf

                insertElementId = endElementId
            }
        }
    }
}

/**
 * @param descriptor The descriptor of the method which should be called.
 * @param topControl Whether the button is positioned at the top.
 * @param initialize Whether the control needs to be initialized.
 */
fun injectControl(
    descriptor: String,
    topControl: Boolean = true,
    initialize: Boolean = true
) {
    if (initialize) {
        // Injects the code to initialize the controls.
        if (topControl) {
            inflateTopControlMethod.addInstruction(
                inflateTopControlInsertIndex++,
                "invoke-static { v$inflateTopControlRegister }, $descriptor->initializeButton(Landroid/view/View;)V",
            )
        } else {
            inflateBottomControlMethod.addInstruction(
                inflateBottomControlInsertIndex++,
                "invoke-static { v$inflateBottomControlRegister }, $descriptor->initializeButton(Landroid/view/View;)V",
            )
        }
    }

    visibilityMethod.addInstruction(
        visibilityInsertIndex++,
        "invoke-static { p1 , p2 }, $descriptor->setVisibility(ZZ)V",
    )

    if (!visibilityImmediateCallbacksExistModified) {
        visibilityImmediateCallbacksExistModified = true
        visibilityImmediateCallbacksExistMethod.returnEarly(true)
    }

    visibilityImmediateMethod.addInstruction(
        visibilityImmediateInsertIndex++,
        "invoke-static { p0 }, $descriptor->setVisibilityImmediate(Z)V",
    )

    visibilityNegatedImmediateMethod.addInstruction(
        visibilityNegatedImmediateInsertIndex++,
        "invoke-static { }, $descriptor->setVisibilityNegatedImmediate()V",
    )
}

internal const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/PlayerControlsPatch;"

private const val EXTENSION_PLAYER_CONTROLS_VISIBILITY_HOOK_CLASS_DESCRIPTOR =
    "$UTILS_PATH/PlayerControlsVisibilityHookPatch;"

private lateinit var inflateTopControlMethod: MutableMethod
private var inflateTopControlInsertIndex: Int = -1
private var inflateTopControlRegister: Int = -1

private lateinit var inflateBottomControlMethod: MutableMethod
private var inflateBottomControlInsertIndex: Int = -1
private var inflateBottomControlRegister: Int = -1

private lateinit var visibilityMethod: MutableMethod
private var visibilityInsertIndex: Int = 0

private var visibilityImmediateCallbacksExistModified = false
private lateinit var visibilityImmediateCallbacksExistMethod: MutableMethod

private lateinit var visibilityImmediateMethod: MutableMethod
private var visibilityImmediateInsertIndex: Int = 0

private lateinit var visibilityNegatedImmediateMethod: MutableMethod
private var visibilityNegatedImmediateInsertIndex: Int = 0

val playerControlsPatch = bytecodePatch(
    name = "player-Controls-Patch",
    description = "playerControlsPatch"
) {
    dependsOn(
        playerControlsResourcePatch,
        sharedExtensionPatch,
        sharedResourceIdPatch,
        versionCheckPatch,
    )

    execute {
        playerControlsVisibilityEntityModelFingerprint.matchOrThrow().let {
            it.method.apply {
                val startIndex = it.patternMatch!!.startIndex
                val iGetReference = getInstruction<ReferenceInstruction>(startIndex).reference
                val staticReference = getInstruction<ReferenceInstruction>(startIndex + 1).reference

                it.classDef.methods.find { method -> method.name == "<init>" }?.apply {
                    val targetIndex = indexOfFirstInstructionOrThrow(Opcode.IPUT_OBJECT)
                    val targetRegister =
                        getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                    addInstructions(
                        targetIndex + 1, """
                            iget v$targetRegister, v$targetRegister, $iGetReference
                            invoke-static {v$targetRegister}, $staticReference
                            move-result-object v$targetRegister
                            invoke-static {v$targetRegister}, $EXTENSION_PLAYER_CONTROLS_VISIBILITY_HOOK_CLASS_DESCRIPTOR->setPlayerControlsVisibility(Ljava/lang/Enum;)V
                            """
                    )
                } ?: throw PatchException("Constructor method not found")
            }
        }

        motionEventFingerprint.mutableMethodOrThrow(youtubeControlsOverlayFingerprint).apply {
            visibilityNegatedImmediateMethod = this
            visibilityNegatedImmediateInsertIndex = indexOfTranslationInstruction(this) + 1
        }

        fun MutableMethod.indexOfFirstViewInflateOrThrow() = indexOfFirstInstructionOrThrow {
            val reference = getReference<MethodReference>()
            reference?.definingClass == "Landroid/view/ViewStub;" &&
                    reference.name == "inflate"
        }

        playerBottomControlsInflateFingerprint.mutableMethodOrThrow().apply {
            inflateBottomControlMethod = this

            val inflateReturnObjectIndex = indexOfFirstViewInflateOrThrow() + 1
            inflateBottomControlRegister =
                getInstruction<OneRegisterInstruction>(inflateReturnObjectIndex).registerA
            inflateBottomControlInsertIndex = inflateReturnObjectIndex + 1
        }

        playerTopControlsInflateFingerprint.mutableMethodOrThrow().apply {
            inflateTopControlMethod = this

            val inflateReturnObjectIndex = indexOfFirstViewInflateOrThrow() + 1
            inflateTopControlRegister =
                getInstruction<OneRegisterInstruction>(inflateReturnObjectIndex).registerA
            inflateTopControlInsertIndex = inflateReturnObjectIndex + 1
        }

        visibilityMethod = controlsOverlayVisibilityFingerprint.mutableMethodOrThrow(
            playerTopControlsInflateFingerprint,
        )

        // Hook the fullscreen close button.  Used to fix visibility
        // when seeking and other situations.
        overlayViewInflateFingerprint.mutableMethodOrThrow().apply {
            val resourceIndex = indexOfFirstLiteralInstructionReversedOrThrow(fullScreenButton)

            val index = indexOfFirstInstructionOrThrow(resourceIndex) {
                opcode == Opcode.CHECK_CAST &&
                        getReference<TypeReference>()?.type ==
                        "Landroid/widget/ImageView;"
            }
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            addInstruction(
                index + 1,
                "invoke-static { v$register }, " +
                        "$EXTENSION_CLASS_DESCRIPTOR->setFullscreenCloseButton(Landroid/widget/ImageView;)V",
            )
        }

        visibilityImmediateCallbacksExistMethod =
            playerControlsExtensionHookListenersExistFingerprint.mutableMethodOrThrow()
        visibilityImmediateMethod = playerControlsExtensionHookFingerprint.mutableMethodOrThrow()

        // A/B test for a slightly different bottom overlay controls,
        // that uses layout file youtube_video_exploder_controls_bottom_ui_container.xml
        // The change to support this is simple and only requires adding buttons to both layout files,
        // but for now force this different layout off since it's still an experimental test.
        if (is_19_36_or_greater) {
            playerBottomControlsExploderFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                PLAYER_BOTTOM_CONTROLS_EXPLODER_FEATURE_FLAG,
                "0x0"
            )
        }

        // A/B test of different top overlay controls. Two different layouts can be used:
        // youtube_cf_navigation_improvement_controls_layout.xml
        // youtube_cf_minimal_impact_controls_layout.xml
        //
        // Flag was removed in 20.19+
        if (is_19_25_or_greater && !is_20_19_or_greater) {
            playerTopControlsExperimentalLayoutFeatureFlagFingerprint.mutableMethodOrThrow().apply {
                val index = indexOfFirstInstructionOrThrow(Opcode.MOVE_RESULT_OBJECT)
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index + 1,
                    """
                        invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->getPlayerTopControlsLayoutResourceName(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$register
                        """,
                )
            }
        } else if (is_20_20_or_greater) { // Turn off a/b tests of ugly player buttons that don't match the style of custom player buttons.
            playerControlsFullscreenLargeButtonsFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                PLAYER_CONTROLS_FULLSCREEN_LARGE_BUTTON_FEATURE_FLAG,
                "0x0"
            )

            if (is_20_28_or_greater) {
                playerControlsLargeOverlayButtonsFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                    PLAYER_CONTROLS_FULLSCREEN_LARGE_OVERLAY_BUTTON_FEATURE_FLAG,
                    "0x0"
                )

                if (is_20_30_or_greater) {
                    playerControlsButtonStrokeFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                        PLAYER_CONTROLS_BUTTON_STROKE_FEATURE_FLAG,
                        "0x0"
                    )
                }
            }
        }
    }
}
