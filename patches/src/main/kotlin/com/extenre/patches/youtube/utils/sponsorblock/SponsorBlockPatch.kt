/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.utils.sponsorblock

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.extensions.InstructionExtensions.replaceInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.patch.resourcePatch
import com.extenre.patches.youtube.player.overlaybuttons.overlayButtonsPatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import com.extenre.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.patch.PatchList.SPONSORBLOCK
import com.extenre.patches.youtube.utils.playercontrols.addTopControl
import com.extenre.patches.youtube.utils.playercontrols.injectControl
import com.extenre.patches.youtube.utils.playercontrols.playerControlsPatch
import com.extenre.patches.youtube.utils.resourceid.insetOverlayViewLayout
import com.extenre.patches.youtube.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.youtube.utils.seekbarFingerprint
import com.extenre.patches.youtube.utils.seekbarOnDrawFingerprint
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.patches.youtube.utils.totalTimeFingerprint
import com.extenre.patches.youtube.utils.youtubeControlsOverlayFingerprint
import com.extenre.patches.youtube.video.information.hookVideoInformation
import com.extenre.patches.youtube.video.information.onCreateHook
import com.extenre.patches.youtube.video.information.videoInformationPatch
import com.extenre.patches.youtube.video.information.videoTimeHook
import com.extenre.util.ResourceGroup
import com.extenre.util.copyResources
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.getStringOptionValue
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.extenre.util.indexOfFirstLiteralInstructionOrThrow
import com.extenre.util.lowerCaseOrThrow
import com.extenre.util.updatePatchStatus
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_SPONSOR_BLOCK_PATH =
    "$EXTENSION_PATH/sponsorblock"

private const val EXTENSION_SPONSOR_BLOCK_UI_PATH =
    "$EXTENSION_SPONSOR_BLOCK_PATH/ui"

private const val EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR =
    "$EXTENSION_SPONSOR_BLOCK_PATH/SegmentPlaybackController;"

private const val EXTENSION_SPONSOR_BLOCK_VIEW_CONTROLLER_CLASS_DESCRIPTOR =
    "$EXTENSION_SPONSOR_BLOCK_UI_PATH/SponsorBlockViewController;"

val sponsorBlockBytecodePatch = bytecodePatch(
    name = "sponsor-Block-Bytecode-Patch",
    description = "sponsorBlockBytecodePatch"
) {
    dependsOn(
        sharedResourceIdPatch,
        videoInformationPatch,
    )

    execute {
        // Hook the video time method
        videoTimeHook(
            EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR,
            "setVideoTime"
        )
        // Initialize the player controller
        onCreateHook(
            EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR,
            "initialize"
        )


        seekbarOnDrawFingerprint.mutableMethodOrThrow(seekbarFingerprint).apply {
            // Get left and right of seekbar rectangle
            val moveObjectIndex = indexOfFirstInstructionOrThrow(Opcode.MOVE_OBJECT_FROM16)

            addInstruction(
                moveObjectIndex + 1,
                "invoke-static/range {p0 .. p0}, " +
                        "$EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarRect(Ljava/lang/Object;)V"
            )

            // Set seekbar thickness
            val roundIndex = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.name == "round"
            } + 1
            val roundRegister = getInstruction<OneRegisterInstruction>(roundIndex).registerA

            addInstruction(
                roundIndex + 1,
                "invoke-static {v$roundRegister}, " +
                        "$EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarThickness(I)V"
            )

            // Draw segment
            val drawCircleIndex = indexOfFirstInstructionReversedOrThrow {
                getReference<MethodReference>()?.name == "drawCircle"
            }
            val drawCircleInstruction = getInstruction<FiveRegisterInstruction>(drawCircleIndex)
            addInstruction(
                drawCircleIndex,
                "invoke-static {v${drawCircleInstruction.registerC}, v${drawCircleInstruction.registerE}}, " +
                        "$EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->drawSponsorTimeBars(Landroid/graphics/Canvas;F)V"
            )
        }

        // Voting & Shield button
        setOf(
            "CreateSegmentButton",
            "VotingButton"
        ).forEach { className ->
            injectControl("$EXTENSION_SPONSOR_BLOCK_UI_PATH/${className};")
        }

        // Skip button
        injectControl(
            descriptor = EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR,
            topControl = false,
            initialize = false
        )

        // Append timestamp
        totalTimeFingerprint.mutableMethodOrThrow().apply {
            val targetIndex = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.name == "getString"
            } + 1
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstructions(
                targetIndex + 1, """
                        invoke-static {v$targetRegister}, $EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->appendTimeWithoutSegments(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$targetRegister
                        """
            )
        }

        // Initialize the SponsorBlock view
        youtubeControlsOverlayFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex =
                    indexOfFirstLiteralInstructionOrThrow(insetOverlayViewLayout)
                val checkCastIndex = indexOfFirstInstructionOrThrow(targetIndex, Opcode.CHECK_CAST)
                val targetRegister =
                    getInstruction<OneRegisterInstruction>(checkCastIndex).registerA

                addInstruction(
                    checkCastIndex + 1,
                    "invoke-static {v$targetRegister}, $EXTENSION_SPONSOR_BLOCK_VIEW_CONTROLLER_CLASS_DESCRIPTOR->initialize(Landroid/view/ViewGroup;)V"
                )
            }
        }

        // Replace strings
        rectangleFieldInvalidatorFingerprint.mutableMethodOrThrow(seekbarFingerprint).apply {
            val invalidateIndex = indexOfInvalidateInstruction(this)
            val rectangleIndex = indexOfFirstInstructionReversedOrThrow(invalidateIndex + 1) {
                getReference<FieldReference>()?.type == "Landroid/graphics/Rect;"
            }
            val rectangleFieldName =
                (getInstruction<ReferenceInstruction>(rectangleIndex).reference as FieldReference).name

            segmentPlaybackControllerFingerprint.matchOrThrow().let {
                it.method.apply {
                    val replaceIndex = it.patternMatch!!.startIndex
                    val replaceRegister =
                        getInstruction<OneRegisterInstruction>(replaceIndex).registerA

                    replaceInstruction(
                        replaceIndex,
                        "const-string v$replaceRegister, \"$rectangleFieldName\""
                    )
                }
            }
        }

        // Set current video id
        hookVideoInformation("$EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")

        updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "SponsorBlock")
    }
}

@Suppress("unused")
val sponsorBlockPatch = resourcePatch(
    name = SPONSORBLOCK.key,
    description = "${SPONSORBLOCK.title}: ${SPONSORBLOCK.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        playerControlsPatch,
        sponsorBlockBytecodePatch,
        settingsPatch,
    )

    execute {
        /**
         * merge SponsorBlock drawables to main drawables
         */
        arrayOf(
            ResourceGroup(
                "layout",
                "extenre_sb_inline_sponsor_overlay.xml",
                "extenre_sb_skip_sponsor_button.xml"
            )
        ).forEach { resourceGroup ->
            copyResources("youtube/sponsorblock/shared", resourceGroup)
        }

        val iconType = overlayButtonsPatch
            .getStringOptionValue("iconType")
            .lowerCaseOrThrow()
        val outlineIcon = iconType == "thin"

        if (outlineIcon) {
            arrayOf(
                ResourceGroup(
                    "layout",
                    "extenre_sb_new_segment.xml"
                ),
                ResourceGroup(
                    "drawable",
                    "extenre_sb_adjust.xml",
                    "extenre_sb_backward.xml",
                    "extenre_sb_compare.xml",
                    "extenre_sb_edit.xml",
                    "extenre_sb_forward.xml",
                    "extenre_sb_logo.xml",
                    "extenre_sb_publish.xml",
                    "extenre_sb_voting.xml"
                )
            ).forEach { resourceGroup ->
                copyResources("youtube/sponsorblock/outline", resourceGroup)
            }
        } else {
            arrayOf(
                ResourceGroup(
                    "layout",
                    "extenre_sb_new_segment.xml"
                ),
                ResourceGroup(
                    "drawable",
                    "extenre_sb_adjust.xml",
                    "extenre_sb_compare.xml",
                    "extenre_sb_edit.xml",
                    "extenre_sb_logo.xml",
                    "extenre_sb_publish.xml",
                    "extenre_sb_voting.xml"
                )
            ).forEach { resourceGroup ->
                copyResources("youtube/sponsorblock/default", resourceGroup)
            }
        }

        /**
         * merge xml nodes from the host to their real xml files
         */
        addTopControl(
            "youtube/sponsorblock/shared",
            "@+id/extenre_sb_voting_button",
            "@+id/extenre_sb_create_segment_button"
        )

        /**
         * Add settings
         */
        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: SPONSOR_BLOCK"
            ),
            SPONSORBLOCK
        )
    }
}
