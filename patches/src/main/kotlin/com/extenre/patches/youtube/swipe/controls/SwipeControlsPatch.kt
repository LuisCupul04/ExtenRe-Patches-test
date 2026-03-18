/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.swipe.controls

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.extensions.InstructionExtensions.removeInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.smali.ExternalLabel
import com.extenre.patches.shared.mainactivity.mainActivityMutableClass
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.SWIPE_PATH
import com.extenre.patches.youtube.utils.lockmodestate.lockModeStateHookPatch
import com.extenre.patches.youtube.utils.mainactivity.mainActivityResolvePatch
import com.extenre.patches.youtube.utils.patch.PatchList.SWIPE_CONTROLS
import com.extenre.patches.youtube.utils.playertype.playerTypeHookPatch
import com.extenre.patches.youtube.utils.playservice.is_19_09_or_greater
import com.extenre.patches.youtube.utils.playservice.is_19_15_or_greater
import com.extenre.patches.youtube.utils.playservice.is_19_23_or_greater
import com.extenre.patches.youtube.utils.playservice.is_19_36_or_greater
import com.extenre.patches.youtube.utils.playservice.is_19_46_or_greater
import com.extenre.patches.youtube.utils.playservice.versionCheckPatch
import com.extenre.patches.youtube.utils.resourceid.autoNavScrollCancelPadding
import com.extenre.patches.youtube.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.ResourceUtils.getContext
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.ResourceGroup
import com.extenre.util.copyResources
import com.extenre.util.fingerprint.injectLiteralInstructionBooleanCall
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.fingerprint.mutableClassOrThrow
import com.extenre.util.getReference
import com.extenre.util.getWalkerMethod
import com.extenre.util.hookClassHierarchy
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstLiteralInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_SWIPE_CONTROLS_PATCH_CLASS_DESCRIPTOR =
    "$SWIPE_PATH/SwipeControlsPatch;"

@Suppress("unused")
val swipeControlsPatch = bytecodePatch(
    name = SWIPE_CONTROLS.key,
    description = "${SWIPE_CONTROLS.title}: ${SWIPE_CONTROLS.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        lockModeStateHookPatch,
        mainActivityResolvePatch,
        playerTypeHookPatch,
        sharedResourceIdPatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {

        // region patch for swipe controls patch

        hookClassHierarchy(
            swipeControlsHostActivityFingerprint.mutableClassOrThrow(),
            mainActivityMutableClass
        )

        // endregion

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: SWIPE_CONTROLS",
            "SETTINGS: DISABLE_SWIPE_TO_ENTER_FULLSCREEN_MODE_BELOW_THE_PLAYER"
        )

        // region patch for disable HDR auto brightness

        // Since it does not support all versions,
        // add settings only if the patch is successful.
        if (!is_19_09_or_greater) {
            hdrBrightnessFingerprint.mutableMethodOrThrow().apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $EXTENSION_SWIPE_CONTROLS_PATCH_CLASS_DESCRIPTOR->disableHDRAutoBrightness()Z
                        move-result v0
                        if-eqz v0, :default
                        return-void
                        """, ExternalLabel("default", getInstruction(0))
                )
                settingArray += "SETTINGS: DISABLE_HDR_BRIGHTNESS"
            }
        }

        // endregion

        // region patch for enable swipe to switch video

        // For some reason, enabling this flag in YT 19.47 causes a specific bug related to the miniplayer:
        // https://github.com/inotia00/ReVanced_Extended/issues/2871.
        //
        // It's likely caused by another experimental flag, but it's not yet known which flag is causing the bug.
        // This flag has been temporarily restricted to YT 19.43 and 19.44.
        if (is_19_23_or_greater) {
            val descriptor = if (is_19_46_or_greater) {
                "0x0"
            } else {
                settingArray += "SETTINGS: ENABLE_SWIPE_TO_SWITCH_VIDEO"

                "$EXTENSION_SWIPE_CONTROLS_PATCH_CLASS_DESCRIPTOR->enableSwipeToSwitchVideo()Z"
            }

            swipeToSwitchVideoFingerprint.injectLiteralInstructionBooleanCall(
                SWIPE_TO_SWITCH_VIDEO_FEATURE_FLAG,
                descriptor
            )
        }

        // endregion

        // region patch for disable swipe to enter fullscreen mode (below the player)

        if (!is_19_36_or_greater) {
            watchPanelGesturesFingerprint.injectLiteralInstructionBooleanCall(
                WATCH_PANEL_GESTURES_PRIMARY_FEATURE_FLAG,
                "$EXTENSION_SWIPE_CONTROLS_PATCH_CLASS_DESCRIPTOR->disableSwipeToEnterFullscreenModeBelowThePlayer()Z"
            )
        } else {
            watchPanelGesturesAlternativeFingerprint.mutableMethodOrThrow().apply {
                val literalIndex = indexOfFirstLiteralInstruction(autoNavScrollCancelPadding)
                val middleIndex = indexOfFirstInstructionOrThrow(literalIndex) {
                    val reference = getReference<MethodReference>()
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            reference?.returnType == "V" &&
                            reference.parameterTypes.isEmpty()
                }
                val targetIndex = indexOfFirstInstructionOrThrow(middleIndex + 1) {
                    val reference = getReference<MethodReference>()
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            reference?.returnType == "V" &&
                            reference.parameterTypes.isEmpty()
                }
                if (getInstruction(targetIndex - 1).opcode != Opcode.IGET_OBJECT) {
                    throw PatchException(
                        "Previous Opcode pattern does not match: ${
                            getInstruction(
                                targetIndex - 1
                            ).opcode
                        }"
                    )
                }
                if (getInstruction(targetIndex + 1).opcode != Opcode.IF_EQZ) {
                    throw PatchException(
                        "Next Opcode pattern does not match: ${
                            getInstruction(
                                targetIndex + 1
                            ).opcode
                        }"
                    )
                }
                val fieldReference = getInstruction<ReferenceInstruction>(targetIndex - 1).reference
                val fieldInstruction = getInstruction<TwoRegisterInstruction>(targetIndex - 1)

                addInstructionsWithLabels(
                    targetIndex, """
                        invoke-static {}, $EXTENSION_SWIPE_CONTROLS_PATCH_CLASS_DESCRIPTOR->disableSwipeToEnterFullscreenModeBelowThePlayer()Z
                        move-result v${fieldInstruction.registerA}
                        if-eqz v${fieldInstruction.registerA}, :disable
                        iget-object v${fieldInstruction.registerA}, v${fieldInstruction.registerB}, $fieldReference
                        """, ExternalLabel("disable", getInstruction(targetIndex + 1))
                )
                removeInstruction(targetIndex - 1)
            }
        }

        if (is_19_15_or_greater) {
            watchPanelGesturesChannelBarFingerprint.injectLiteralInstructionBooleanCall(
                WATCH_PANEL_GESTURES_SECONDARY_FEATURE_FLAG,
                "$EXTENSION_SWIPE_CONTROLS_PATCH_CLASS_DESCRIPTOR->disableSwipeToEnterFullscreenModeBelowThePlayer()Z"
            )
        }

        // endregion

        // region patch for disable swipe to enter fullscreen mode (in the player) and disable swipe to exit fullscreen mode

        playerGestureConfigSyntheticFingerprint.mutableMethodOrThrow().apply {
            val disableSwipeToExitFullscreenModeIndex =
                indexOfPlayerConfigModelBooleanInstruction(this)
            val disableSwipeToEnterFullscreenModeInThePlayerIndex =
                indexOfPlayerConfigModelBooleanInstruction(
                    this,
                    disableSwipeToExitFullscreenModeIndex + 1
                )

            mapOf(
                disableSwipeToExitFullscreenModeIndex to "disableSwipeToExitFullscreenMode",
                disableSwipeToEnterFullscreenModeInThePlayerIndex to "disableSwipeToEnterFullscreenModeInThePlayer"
            ).forEach { (walkerIndex, methodName) ->
                getWalkerMethod(walkerIndex).apply {
                    val index = implementation!!.instructions.lastIndex
                    val register = getInstruction<OneRegisterInstruction>(index).registerA

                    addInstructions(
                        index,
                        """
                            invoke-static {v$register}, $EXTENSION_SWIPE_CONTROLS_PATCH_CLASS_DESCRIPTOR->$methodName(Z)Z
                            move-result v$register
                            """
                    )
                }
            }
        }

        // endregion

        // region copy resources

        getContext().copyResources(
            "youtube/swipecontrols",
            ResourceGroup(
                "drawable",
                // Legacy
                "ic_sc_brightness_auto.xml",
                "ic_sc_brightness_manual.xml",
                "ic_sc_volume_mute.xml",
                "ic_sc_volume_normal.xml",
                // Modern
                "extenre_ic_sc_brightness_auto.xml",
                "extenre_ic_sc_brightness_full.xml",
                "extenre_ic_sc_brightness_high.xml",
                "extenre_ic_sc_brightness_low.xml",
                "extenre_ic_sc_brightness_manual.xml",
                "extenre_ic_sc_brightness_medium.xml",
                "extenre_ic_sc_volume_high.xml",
                "extenre_ic_sc_volume_low.xml",
                "extenre_ic_sc_volume_mute.xml",
                "extenre_ic_sc_volume_normal.xml",
            )
        )

        // endregion

        // region add settings

        addPreference(settingArray, SWIPE_CONTROLS)

        // endregion

    }
}
