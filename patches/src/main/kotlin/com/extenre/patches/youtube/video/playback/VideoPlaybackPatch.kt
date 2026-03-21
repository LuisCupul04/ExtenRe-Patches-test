/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.video.playback

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.smali.ExternalLabel
import com.extenre.patches.shared.customspeed.customPlaybackSpeedPatch
import com.extenre.patches.shared.drc.drcAudioPatch
import com.extenre.patches.shared.litho.addLithoFilter
import com.extenre.patches.shared.litho.lithoFilterPatch
import com.extenre.patches.shared.opus.baseOpusCodecsPatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import com.extenre.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.extension.Constants.VIDEO_PATH
import com.extenre.patches.youtube.utils.fix.litho.lithoLayoutPatch
import com.extenre.patches.youtube.utils.fix.shortsplayback.shortsPlaybackPatch
import com.extenre.patches.youtube.utils.flyoutmenu.flyoutMenuHookPatch
import com.extenre.patches.youtube.utils.patch.PatchList.VIDEO_PLAYBACK
import com.extenre.patches.youtube.utils.playertype.playerTypeHookPatch
import com.extenre.patches.youtube.utils.playservice.is_19_30_or_greater
import com.extenre.patches.youtube.utils.playservice.versionCheckPatch
import com.extenre.patches.youtube.utils.qualityMenuViewInflateFingerprint
import com.extenre.patches.youtube.utils.recyclerview.recyclerViewTreeObserverHook
import com.extenre.patches.youtube.utils.recyclerview.recyclerViewTreeObserverPatch
import com.extenre.patches.youtube.utils.request.buildRequestPatch
import com.extenre.patches.youtube.utils.request.hookBuildRequest
import com.extenre.patches.youtube.utils.request.hookInitPlaybackBuildRequest
import com.extenre.patches.youtube.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.patches.youtube.video.information.hookBackgroundPlayVideoInformation
import com.extenre.patches.youtube.video.information.hookVideoInformation
import com.extenre.patches.youtube.video.information.speedSelectionInsertMethod
import com.extenre.patches.youtube.video.information.videoInformationPatch
import com.extenre.patches.youtube.video.videoid.videoIdPatch
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstruction
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.extenre.util.updatePatchStatus
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val PLAYBACK_SPEED_MENU_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/PlaybackSpeedMenuFilter;"
private const val VIDEO_QUALITY_MENU_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/VideoQualityMenuFilter;"
private const val EXTENSION_ADVANCED_VIDEO_QUALITY_MENU_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/AdvancedVideoQualityMenuPatch;"
private const val EXTENSION_VP9_CODEC_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/VP9CodecPatch;"
private const val EXTENSION_CUSTOM_PLAYBACK_SPEED_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/CustomPlaybackSpeedPatch;"
private const val EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/PlaybackSpeedPatch;"
private const val EXTENSION_SPOOF_DEVICE_DIMENSIONS_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/SpoofDeviceDimensionsPatch;"
private const val EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/VideoQualityPatch;"

@Suppress("unused")
val videoPlaybackPatch = bytecodePatch(
    name = VIDEO_PLAYBACK.key,
    description = "${VIDEO_PLAYBACK.title}: ${VIDEO_PLAYBACK.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        versionCheckPatch,
        customPlaybackSpeedPatch(
            "$VIDEO_PATH/CustomPlaybackSpeedPatch;",
            8.0f
        ),
        baseOpusCodecsPatch(),
        drcAudioPatch { is_19_30_or_greater },
        flyoutMenuHookPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        buildRequestPatch,
        disableHdrPatch,
        playerTypeHookPatch,
        recyclerViewTreeObserverPatch,
        shortsPlaybackPatch,
        videoIdPatch,
        videoInformationPatch,
        sharedResourceIdPatch,
    )

    execute {
        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: VIDEO"
        )

        // region patch for custom playback speed
        recyclerViewTreeObserverHook("$EXTENSION_CUSTOM_PLAYBACK_SPEED_CLASS_DESCRIPTOR->onFlyoutMenuCreate(Landroid/support/v7/widget/RecyclerView;)V")
        addLithoFilter(PLAYBACK_SPEED_MENU_FILTER_CLASS_DESCRIPTOR)
        // endregion

        // region patch for default playback speed
        val newMethod =
            playbackSpeedChangedFromRecyclerViewFingerprint.mutableMethodOrThrow(
                qualityChangedFromRecyclerViewFingerprint
            )

        arrayOf(
            newMethod,
            speedSelectionInsertMethod
        ).forEach { method ->
            method.apply {
                val speedSelectionValueInstructionIndex =
                    indexOfFirstInstructionOrThrow(Opcode.IGET)
                val speedSelectionValueRegister =
                    getInstruction<TwoRegisterInstruction>(speedSelectionValueInstructionIndex).registerA

                addInstruction(
                    speedSelectionValueInstructionIndex + 1,
                    "invoke-static {v$speedSelectionValueRegister}, " +
                            "$EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->userSelectedPlaybackSpeed(F)V"
                )
            }
        }

        mediaLibPlayerLoadVideoFingerprint.matchOrThrow().let { match ->
            val method = match.method
            val classDef = match.classDef
            val mutableMethod = mutableClassDefBy(classDef.type).methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.apply {
                val startIndex = match.patternMatch!!.endIndex
                val targetIndex = indexOfPlaybackSpeedInstruction(this, startIndex)
                val targetReference =
                    getInstruction<ReferenceInstruction>(targetIndex).reference as FieldReference

                // Obtener el método mutable que lee ese campo
                val targetMethod = mutableClassDefBy(targetReference.definingClass).methods.find { m ->
                    m.returnType == "F" &&
                            m.indexOfFirstInstruction {
                                opcode == Opcode.IGET &&
                                        getReference<FieldReference>() == targetReference
                            } >= 0
                } ?: throw PatchException("Method reading field $targetReference not found")

                targetMethod.apply {
                    val insertIndex = implementation!!.instructions.lastIndex
                    val insertRegister =
                        getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$insertRegister}, $EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->getPlaybackSpeed(F)F
                            move-result v$insertRegister
                            """
                    )
                }
            }
        }

        hookBackgroundPlayVideoInformation("$EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
        hookVideoInformation("$EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
        hookBuildRequest("$EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->fetchRequest(Ljava/lang/String;Ljava/util/Map;)V")
        hookInitPlaybackBuildRequest("$EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->fetchRequest(Ljava/lang/String;Ljava/util/Map;)V")

        updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "VideoPlayback")
        // endregion

        // region patch for default video quality
        qualityChangedFromRecyclerViewFingerprint.matchOrThrow().let { match ->
            val method = match.method
            val classDef = match.classDef
            val mutableMethod = mutableClassDefBy(classDef.type).methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.apply {
                val index = match.patternMatch!!.startIndex
                val register = getInstruction<TwoRegisterInstruction>(index).registerA

                addInstruction(
                    index + 1,
                    "invoke-static { v$register }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->userChangedQualityInNewFlyout(I)V"
                )
            }
        }

        videoQualityItemOnClickFingerprint.mutableMethodOrThrow(
            videoQualityItemOnClickParentFingerprint
        ).addInstruction(
            0,
            "invoke-static { p3 }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->userChangedQualityInOldFlyout(I)V"
        )
        // endregion

        // region patch for show advanced video quality menu
        qualityMenuViewInflateFingerprint.mutableMethodOrThrow().apply {
            val insertIndex = indexOfFirstInstructionOrThrow(Opcode.CHECK_CAST)
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstruction(
                insertIndex + 1,
                "invoke-static { v$insertRegister }, " +
                        "$EXTENSION_ADVANCED_VIDEO_QUALITY_MENU_CLASS_DESCRIPTOR->showAdvancedVideoQualityMenu(Landroid/widget/ListView;)V"
            )
        }

        qualityMenuViewInflateOnItemClickFingerprint
            .mutableMethodOrThrow(qualityMenuViewInflateFingerprint)
            .apply {
                val contextIndex = indexOfContextInstruction(this)
                val contextField =
                    getInstruction<ReferenceInstruction>(contextIndex).reference as FieldReference
                val castIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.CHECK_CAST &&
                            getReference<TypeReference>()?.type == contextField.definingClass
                }
                val castRegister = getInstruction<OneRegisterInstruction>(castIndex).registerA

                val insertIndex = indexOfFirstInstructionOrThrow(castIndex, Opcode.IGET_OBJECT)
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                val jumpIndex = indexOfFirstInstructionReversedOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "dismiss"
                }

                addInstructionsWithLabels(
                    insertIndex, """
                        iget-object v$insertRegister, v$castRegister, $contextField
                        invoke-static {v$insertRegister}, $EXTENSION_ADVANCED_VIDEO_QUALITY_MENU_CLASS_DESCRIPTOR->showAdvancedVideoQualityMenu(Landroid/content/Context;)Z
                        move-result v$insertRegister
                        if-nez v$insertRegister, :dismiss
                        """, ExternalLabel("dismiss", getInstruction(jumpIndex))
                )
            }

        recyclerViewTreeObserverHook("$EXTENSION_ADVANCED_VIDEO_QUALITY_MENU_CLASS_DESCRIPTOR->onFlyoutMenuCreate(Landroid/support/v7/widget/RecyclerView;)V")
        addLithoFilter(VIDEO_QUALITY_MENU_FILTER_CLASS_DESCRIPTOR)
        // endregion

        // region patch for spoof device dimensions
        val deviceDimensionsModelMatch = deviceDimensionsModelToStringFingerprint.matchOrThrow()
        val deviceDimensionsModelClass = deviceDimensionsModelMatch.classDef.type
        val mutableClass = mutableClassDefBy(deviceDimensionsModelClass)
        val constructor = mutableClass.methods.find { MethodUtil.isConstructor(it) }
            ?: throw PatchException("Constructor not found in $deviceDimensionsModelClass")
        constructor.addInstructions(
            1, // Add after super call.
            mapOf(
                1 to "MinHeightOrWidth", // p1 = min height
                2 to "MaxHeightOrWidth", // p2 = max height
                3 to "MinHeightOrWidth", // p3 = min width
                4 to "MaxHeightOrWidth"  // p4 = max width
            ).map { (parameter, method) ->
                """
                    invoke-static { p$parameter }, $EXTENSION_SPOOF_DEVICE_DIMENSIONS_CLASS_DESCRIPTOR->get$method(I)I
                    move-result p$parameter
                    """
            }.joinToString("\n") { it }
        )
        // endregion

        // region patch for disable VP9 codec
        vp9CapabilityFingerprint.mutableMethodOrThrow().apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $EXTENSION_VP9_CODEC_CLASS_DESCRIPTOR->disableVP9Codec()Z
                    move-result v0
                    if-nez v0, :default
                    return v0
                    """, ExternalLabel("default", getInstruction(0))
            )
        }
        // endregion

        // region add settings
        addPreference(settingArray, VIDEO_PLAYBACK)
        // endregion
    }
}