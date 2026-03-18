/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.player.flyoutmenu.hide

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.shared.litho.addLithoFilter
import com.extenre.patches.shared.litho.lithoFilterPatch
import com.extenre.patches.youtube.utils.YOUTUBE_VIDEO_QUALITY_CLASS_TYPE
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import com.extenre.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.fix.litho.lithoLayoutPatch
import com.extenre.patches.youtube.utils.indexOfAddHeaderViewInstruction
import com.extenre.patches.youtube.utils.patch.PatchList.HIDE_PLAYER_FLYOUT_MENU
import com.extenre.patches.youtube.utils.playertype.playerTypeHookPatch
import com.extenre.patches.youtube.utils.playservice.is_18_39_or_greater
import com.extenre.patches.youtube.utils.playservice.is_19_30_or_greater
import com.extenre.patches.youtube.utils.playservice.versionCheckPatch
import com.extenre.patches.youtube.utils.qualityMenuViewInflateFingerprint
import com.extenre.patches.youtube.utils.resourceid.bottomSheetFooterText
import com.extenre.patches.youtube.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.patches.youtube.video.information.videoInformationPatch
import com.extenre.util.REGISTER_TEMPLATE_REPLACEMENT
import com.extenre.util.fingerprint.injectLiteralInstructionBooleanCall
import com.extenre.util.fingerprint.injectLiteralInstructionViewCall
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val PANELS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/PlayerFlyoutMenuFilter;"

@Suppress("unused")
val playerFlyoutMenuPatch = bytecodePatch(
    name = HIDE_PLAYER_FLYOUT_MENU.key,
    description = "${HIDE_PLAYER_FLYOUT_MENU.title}: ${HIDE_PLAYER_FLYOUT_MENU.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        playerTypeHookPatch,
        sharedResourceIdPatch,
        videoInformationPatch,
        versionCheckPatch
    )

    execute {
        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: PLAYER",
            "PREFERENCE_SCREENS: FLYOUT_MENU",
            "SETTINGS: HIDE_PLAYER_FLYOUT_MENU"
        )

        // region hide player flyout menu header, footer (non-litho)

        mapOf(
            advancedQualityBottomSheetFingerprint to "hidePlayerFlyoutMenuQualityFooter",
            captionsBottomSheetFingerprint to "hidePlayerFlyoutMenuCaptionsFooter",
            qualityMenuViewInflateFingerprint to "hidePlayerFlyoutMenuQualityFooter"
        ).forEach { (fingerprint, name) ->
            val smaliInstruction = """
                    invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $PLAYER_CLASS_DESCRIPTOR->$name(Landroid/view/View;)V
                    """
            fingerprint.injectLiteralInstructionViewCall(bottomSheetFooterText, smaliInstruction)
        }

        arrayOf(
            advancedQualityBottomSheetFingerprint,
            qualityMenuViewInflateFingerprint
        ).forEach { fingerprint ->
            fingerprint.mutableMethodOrThrow().apply {
                val insertIndex = indexOfAddHeaderViewInstruction(this)
                val insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->hidePlayerFlyoutMenuQualityHeader(Landroid/view/View;)Landroid/view/View;
                        move-result-object v$insertRegister
                        """
                )
            }
        }

        // endregion

        // region patch for hide '1080p Premium' label

        currentVideoFormatConstructorFingerprint.mutableMethodOrThrow(
            currentVideoFormatToStringFingerprint
        ).apply {
            val videoQualitiesIndex =
                indexOfVideoQualitiesInstruction(this)
            val videoQualitiesRegister =
                getInstruction<TwoRegisterInstruction>(videoQualitiesIndex).registerA

            addInstructions(
                1, """
                    invoke-static/range { v$videoQualitiesRegister .. v$videoQualitiesRegister }, $PLAYER_CLASS_DESCRIPTOR->hidePlayerFlyoutMenuEnhancedBitrate([$YOUTUBE_VIDEO_QUALITY_CLASS_TYPE)[$YOUTUBE_VIDEO_QUALITY_CLASS_TYPE
                    move-result-object v$videoQualitiesRegister
                    """
            )
        }

        // endregion

        // region patch for hide pip mode menu

        if (is_18_39_or_greater) {
            pipModeConfigFingerprint.injectLiteralInstructionBooleanCall(
                45427407L,
                "$PLAYER_CLASS_DESCRIPTOR->hidePiPModeMenu(Z)Z"
            )
            settingArray += "SETTINGS: HIDE_PIP_MODE_MENU"
        }

        // endregion

        // region patch for hide sleep timer menu

        if (is_19_30_or_greater) {
            // Sleep timer menu in Additional settings (deprecated)
            // TODO: A patch will be implemented to assign this deprecated menu to another action.
            // mapOf(
            //     sleepTimerConstructorFingerprint to SLEEP_TIMER_CONSTRUCTOR_FEATURE_FLAG,
            //     sleepTimerFingerprint to SLEEP_TIMER_FEATURE_FLAG
            // ).forEach { (fingerprint, literal) ->
            //     fingerprint.injectLiteralInstructionBooleanCall(
            //         literal,
            //         "$PLAYER_CLASS_DESCRIPTOR->hideDeprecatedSleepTimerMenu(Z)Z"
            //     )
            // }
            settingArray += "SETTINGS: HIDE_SLEEP_TIMER_MENU"
        }

        // endregion

        addLithoFilter(PANELS_FILTER_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(settingArray, HIDE_PLAYER_FLYOUT_MENU)

        // endregion

    }
}
