/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.general.components

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.extensions.InstructionExtensions.removeInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.shared.litho.addLithoFilter
import com.extenre.patches.shared.litho.lithoFilterPatch
import com.extenre.patches.shared.settingmenu.settingsMenuPatch
import com.extenre.patches.shared.viewgroup.viewGroupMarginLayoutParamsHookPatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import com.extenre.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.extension.Constants.GENERAL_PATH
import com.extenre.patches.youtube.utils.fix.litho.lithoLayoutPatch
import com.extenre.patches.youtube.utils.patch.PatchList.HIDE_LAYOUT_COMPONENTS
import com.extenre.patches.youtube.utils.playservice.is_19_25_or_greater
import com.extenre.patches.youtube.utils.playservice.versionCheckPatch
import com.extenre.patches.youtube.utils.resourceid.accountSwitcherAccessibility
import com.extenre.patches.youtube.utils.resourceid.fab
import com.extenre.patches.youtube.utils.resourceid.pairWithTVKey
import com.extenre.patches.youtube.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.youtube.utils.resourceid.ytCallToAction
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.fingerprint.injectLiteralInstructionBooleanCall
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.fingerprint.mutableClassOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_SETTINGS_MENU_DESCRIPTOR =
    "$GENERAL_PATH/SettingsMenuPatch;"
private const val CUSTOM_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/CustomFilter;"
private const val LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/LayoutComponentsFilter;"

@Suppress("unused")
val layoutComponentsPatch = bytecodePatch(
    name = HIDE_LAYOUT_COMPONENTS.key,
    description = "${HIDE_LAYOUT_COMPONENTS.title}: ${HIDE_LAYOUT_COMPONENTS.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        sharedResourceIdPatch,
        settingsMenuPatch,
        viewGroupMarginLayoutParamsHookPatch,
        versionCheckPatch,
    )

    execute {

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: GENERAL",
            "SETTINGS: HIDE_LAYOUT_COMPONENTS"
        )

        // region patch for disable pip notification

        pipNotificationFingerprint.matchOrThrow().let {
            it.method.apply {
                val checkCastCalls = implementation!!.instructions.withIndex()
                    .filter { instruction ->
                        (instruction.value as? ReferenceInstruction)?.reference.toString() == "Lcom/google/apps/tiktok/account/AccountId;"
                    }

                val checkCastCallSize = checkCastCalls.size
                if (checkCastCallSize != 3)
                    throw PatchException("Couldn't find target index, size: $checkCastCallSize")

                arrayOf(
                    checkCastCalls.elementAt(1).index,
                    checkCastCalls.elementAt(0).index
                ).forEach { index ->
                    addInstruction(
                        index + 1,
                        "return-void"
                    )
                }
            }
        }

        // endregion

        // region patch for disable translucent status bar

        if (is_19_25_or_greater) {
            mapOf(
                translucentStatusBarPrimaryFeatureFlagFingerprint to TRANSLUCENT_STATUS_BAR_PRIMARY_FEATURE_FLAG,
                translucentStatusBarSecondaryFeatureFlagFingerprint to TRANSLUCENT_STATUS_BAR_SECONDARY_FEATURE_FLAG,
            ).forEach { (fingerprint, literal) ->
                fingerprint.injectLiteralInstructionBooleanCall(
                    literal,
                    "$GENERAL_CLASS_DESCRIPTOR->disableTranslucentStatusBar(Z)Z"
                )
            }

            settingArray += "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS"
            settingArray += "SETTINGS: DISABLE_TRANSLUCENT_STATUS_BAR"
        }

        // endregion

        // region patch for disable update screen

        appBlockingCheckResultToStringFingerprint.mutableClassOrThrow().methods.first { method ->
            MethodUtil.isConstructor(method) &&
                    method.parameters == listOf("Landroid/content/Intent;", "Z")
        }.addInstructions(
            1,
            "const/4 p1, 0x0"
        )

        // endregion

        // region patch for hide account menu

        // for you tab
        accountListFingerprint.mutableMethodOrThrow(accountListParentFingerprint).apply {
            val literalIndex = indexOfFirstLiteralInstructionOrThrow(ytCallToAction)
            val targetIndex = indexOfFirstInstructionOrThrow(literalIndex) {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.name == "setText"
            }
            val targetInstruction = getInstruction<FiveRegisterInstruction>(targetIndex)

            addInstruction(
                targetIndex,
                "invoke-static {v${targetInstruction.registerC}, v${targetInstruction.registerD}}, " +
                        "$GENERAL_CLASS_DESCRIPTOR->hideAccountList(Landroid/view/View;Ljava/lang/CharSequence;)V"
            )
        }

        // for tablet and old clients
        accountMenuFingerprint.matchOrThrow(accountMenuParentFingerprint).let {
            it.method.apply {
                val targetIndex = it.patternMatch!!.startIndex + 2
                val targetInstruction = getInstruction<FiveRegisterInstruction>(targetIndex)

                addInstruction(
                    targetIndex,
                    "invoke-static {v${targetInstruction.registerC}, v${targetInstruction.registerD}}, " +
                            "$GENERAL_CLASS_DESCRIPTOR->hideAccountMenu(Landroid/view/View;Ljava/lang/CharSequence;)V"
                )
            }
        }

        // endregion

        // region patch for hide floating microphone

        floatingMicrophoneFingerprint.mutableMethodOrThrow().apply {
            val literalIndex = indexOfFirstLiteralInstructionOrThrow(fab)
            val booleanIndex = indexOfFirstInstructionOrThrow(literalIndex, Opcode.IGET_BOOLEAN)
            val insertRegister = getInstruction<TwoRegisterInstruction>(booleanIndex).registerA

            addInstructions(
                booleanIndex + 1, """
                    invoke-static {v$insertRegister}, $GENERAL_CLASS_DESCRIPTOR->hideFloatingMicrophone(Z)Z
                    move-result v$insertRegister
                    """
            )
        }

        // endregion

        // region patch for hide handle

        accountSwitcherAccessibilityLabelFingerprint.mutableMethodOrThrow().apply {
            val constIndex =
                indexOfFirstLiteralInstructionOrThrow(accountSwitcherAccessibility)
            val insertIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.IF_EQZ)
            val setVisibilityIndex = indexOfFirstInstructionOrThrow(insertIndex) {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.name == "setVisibility"
            }
            val visibilityRegister =
                getInstruction<FiveRegisterInstruction>(setVisibilityIndex).registerD

            addInstructions(
                insertIndex, """
                    invoke-static {v$visibilityRegister}, $GENERAL_CLASS_DESCRIPTOR->hideHandle(I)I
                    move-result v$visibilityRegister
                    """
            )
        }

        // endregion

        // region patch for hide setting menus

        preferenceScreenFingerprint.mutableMethodOrThrow().apply {
            val targetIndex = indexOfPreferenceScreenInstruction(this)
            val targetRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerC
            val targetReference = getInstruction<ReferenceInstruction>(targetIndex).reference

            val insertIndex = implementation!!.instructions.lastIndex

            addInstructions(
                insertIndex + 1, """
                    invoke-virtual {v$targetRegister}, $targetReference
                    move-result-object v$targetRegister
                    invoke-static {v$targetRegister}, $EXTENSION_SETTINGS_MENU_DESCRIPTOR->hideSettingsMenu(Landroidx/preference/PreferenceScreen;)V
                    return-void
                    """
            )
            removeInstruction(insertIndex)
        }

        preferencePairWithTVFingerprint.mutableMethodOrThrow().apply {
            val literalIndex = indexOfFirstLiteralInstructionOrThrow(pairWithTVKey)
            val setPairWithTVPreferenceIndex = indexOfFirstInstructionOrThrow(literalIndex) {
                opcode == Opcode.IPUT_OBJECT &&
                        getReference<FieldReference>()?.type == "Landroidx/preference/Preference;"
            }
            val pairWithTVField =
                getInstruction<ReferenceInstruction>(setPairWithTVPreferenceIndex).reference as FieldReference
            val getPairWithTVPreferenceIndex =
                indexOfFirstInstructionOrThrow(setPairWithTVPreferenceIndex) {
                    opcode == Opcode.IGET_OBJECT &&
                            getReference<FieldReference>() == pairWithTVField
                }
            val insertRegister =
                getInstruction<TwoRegisterInstruction>(getPairWithTVPreferenceIndex).registerA

            addInstructions(
                getPairWithTVPreferenceIndex + 1, """
                    invoke-static {v$insertRegister}, $EXTENSION_SETTINGS_MENU_DESCRIPTOR->hideWatchOnTVMenu(Landroidx/preference/Preference;)Landroidx/preference/Preference;
                    move-result-object v$insertRegister
                    """
            )
        }

        // endregion

        // region patch for hide tooltip content

        tooltipContentFullscreenFingerprint.mutableMethodOrThrow().apply {
            val literalIndex = indexOfFirstLiteralInstructionOrThrow(45384061L)
            val targetIndex = indexOfFirstInstructionOrThrow(literalIndex, Opcode.MOVE_RESULT)
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "const/4 v$targetRegister, 0x0"
            )
        }

        tooltipContentViewFingerprint.mutableMethodOrThrow().addInstruction(
            0,
            "return-void"
        )

        // endregion

        addLithoFilter(CUSTOM_FILTER_CLASS_DESCRIPTOR)
        addLithoFilter(LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(
            settingArray,
            HIDE_LAYOUT_COMPONENTS
        )

        // endregion

    }
}
