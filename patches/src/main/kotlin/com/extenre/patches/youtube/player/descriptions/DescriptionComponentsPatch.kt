/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.player.descriptions

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.extensions.InstructionExtensions.replaceInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.smali.ExternalLabel
import com.extenre.patches.shared.litho.addLithoFilter
import com.extenre.patches.shared.litho.lithoFilterPatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.engagement.engagementPanelHookPatch
import com.extenre.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import com.extenre.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.fix.hype.hypeButtonIconPatch
import com.extenre.patches.youtube.utils.fix.litho.lithoLayoutPatch
import com.extenre.patches.youtube.utils.patch.PatchList.DESCRIPTION_COMPONENTS
import com.extenre.patches.youtube.utils.playertype.playerTypeHookPatch
import com.extenre.patches.youtube.utils.playservice.is_18_49_or_greater
import com.extenre.patches.youtube.utils.playservice.is_19_05_or_greater
import com.extenre.patches.youtube.utils.playservice.versionCheckPatch
import com.extenre.patches.youtube.utils.recyclerview.recyclerViewTreeObserverHook
import com.extenre.patches.youtube.utils.recyclerview.recyclerViewTreeObserverPatch
import com.extenre.patches.youtube.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.youtube.utils.rollingNumberTextViewAnimationUpdateFingerprint
import com.extenre.patches.youtube.utils.rollingNumberTextViewFingerprint
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/DescriptionsFilter;"

@Suppress("unused")
val descriptionComponentsPatch = bytecodePatch(
    name = DESCRIPTION_COMPONENTS.key,
    description = "${DESCRIPTION_COMPONENTS.title}: ${DESCRIPTION_COMPONENTS.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        playerTypeHookPatch,
        recyclerViewTreeObserverPatch,
        engagementPanelHookPatch,
        hypeButtonIconPatch,
        sharedResourceIdPatch,
        versionCheckPatch,
    )

    execute {

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: PLAYER",
            "SETTINGS: DESCRIPTION_COMPONENTS"
        )

        // region patch for disable rolling number animation

        // RollingNumber is applied to YouTube v18.49.37+.
        // In order to maintain compatibility with YouTube v18.48.39 or previous versions,
        // This patch is applied only to the version after YouTube v18.49.37.
        if (is_18_49_or_greater) {
            rollingNumberTextViewAnimationUpdateFingerprint.matchOrThrow(
                rollingNumberTextViewFingerprint
            ).let {
                it.method.apply {
                    val freeRegister = implementation!!.registerCount - parameters.size - 2
                    val imageSpanIndex = it.patternMatch!!.startIndex
                    val setTextIndex = indexOfFirstInstructionOrThrow {
                        opcode == Opcode.INVOKE_VIRTUAL &&
                                getReference<MethodReference>()?.name == "setText"
                    }
                    addInstruction(setTextIndex, "nop")
                    addInstructionsWithLabels(
                        imageSpanIndex, """
                            invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->disableRollingNumberAnimations()Z
                            move-result v$freeRegister
                            if-nez v$freeRegister, :disable_animations
                            """, ExternalLabel("disable_animations", getInstruction(setTextIndex))
                    )
                }
            }

            settingArray += "SETTINGS: DISABLE_ROLLING_NUMBER_ANIMATIONS"
        }

        // endregion

        // region patch for disable video description interaction and expand video description

        if (is_19_05_or_greater) {
            textViewComponentFingerprint.mutableMethodOrThrow().apply {
                val insertIndex = indexOfTextIsSelectableInstruction(this)
                val insertInstruction = getInstruction<FiveRegisterInstruction>(insertIndex)

                replaceInstruction(
                    insertIndex,
                    "invoke-static {v${insertInstruction.registerC}, v${insertInstruction.registerD}}, " +
                            "$PLAYER_CLASS_DESCRIPTOR->disableVideoDescriptionInteraction(Landroid/widget/TextView;Z)V"
                )
            }

            recyclerViewTreeObserverHook("$PLAYER_CLASS_DESCRIPTOR->onVideoDescriptionCreate(Landroid/support/v7/widget/RecyclerView;)V")

            settingArray += "SETTINGS: DESCRIPTION_INTERACTION"
        }

        // endregion

        addLithoFilter(FILTER_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(settingArray, DESCRIPTION_COMPONENTS)

        // endregion

    }
}
