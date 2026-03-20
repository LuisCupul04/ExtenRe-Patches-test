/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.ads.general

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.extensions.InstructionExtensions.removeInstruction
import com.extenre.patcher.extensions.InstructionExtensions.replaceInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.util.smali.ExternalLabel
import com.extenre.patches.shared.ANDROID_AUTOMOTIVE_STRING
import com.extenre.patches.shared.ads.adsPatch
import com.extenre.patches.shared.autoMotiveFingerprint
import com.extenre.patches.shared.litho.addLithoFilter
import com.extenre.patches.shared.litho.lithoFilterPatch
import com.extenre.patches.shared.spoof.guide.addClientOSVersionHook
import com.extenre.patches.shared.spoof.guide.spoofClientGuideEndpointPatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.ADS_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import com.extenre.patches.youtube.utils.fix.litho.lithoLayoutPatch
import com.extenre.patches.youtube.utils.patch.PatchList.HIDE_ADS
import com.extenre.patches.youtube.utils.playservice.is_20_06_or_greater
import com.extenre.patches.youtube.utils.playservice.versionCheckPatch
import com.extenre.patches.youtube.utils.resourceid.adAttribution
import com.extenre.patches.youtube.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.findMutableMethodOf
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.indexOfFirstStringInstructionOrThrow
import com.extenre.util.injectHideViewCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31i
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val ADS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/AdsFilter;"

@Suppress("unused")
val adsPatch = adsPatch(
    block = {
        compatibleWith(COMPATIBLE_PACKAGE)

        dependsOn(
            settingsPatch,
            lithoFilterPatch,
            lithoLayoutPatch,
            sharedResourceIdPatch,
            spoofClientGuideEndpointPatch,
            versionCheckPatch,
        )
    },
    classDescriptor = ADS_CLASS_DESCRIPTOR,
    methodDescriptor = "hideVideoAds",
    executeBlock = {
        addLithoFilter(ADS_FILTER_CLASS_DESCRIPTOR)

        // region patch for hide general ads

        classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                method.implementation.apply {
                    this?.instructions?.forEachIndexed { index, instruction ->
                        if (instruction.opcode != Opcode.CONST)
                            return@forEachIndexed
                        // Instruction to store the id adAttribution into a register
                        if ((instruction as Instruction31i).wideLiteral != adAttribution)
                            return@forEachIndexed

                        val insertIndex = index + 1

                        // Call to get the view with the id adAttribution
                        (instructions.elementAt(insertIndex)).apply {
                            if (opcode != Opcode.INVOKE_VIRTUAL)
                                return@forEachIndexed

                            // Hide the view
                            val viewRegister = (this as Instruction35c).registerC
                            proxy(classDef)
                                .mutableClass
                                .findMutableMethodOf(method)
                                .injectHideViewCall(
                                    insertIndex,
                                    viewRegister,
                                    ADS_CLASS_DESCRIPTOR,
                                    "hideAdAttributionView"
                                )
                        }
                    }
                }
            }
        }

        // endregion

        // region patch for hide get premium

        val compactMatch = compactYpcOfferModuleViewFingerprint.matchOrThrow()
        val compactMethod = compactMatch.method
        val compactClassDef = compactMatch.classDef
        val compactMutableMethod = proxy(compactClassDef).mutableClass.methods.first {
            MethodUtil.methodSignaturesMatch(it, compactMethod)
        }
        compactMutableMethod.apply {
            val startIndex = compactMatch.patternMatch!!.startIndex
            val measuredWidthRegister =
                getInstruction<TwoRegisterInstruction>(startIndex).registerA
            val measuredHeightInstruction =
                getInstruction<TwoRegisterInstruction>(startIndex + 1)
            val measuredHeightRegister = measuredHeightInstruction.registerA
            val tempRegister = measuredHeightInstruction.registerB

            addInstructionsWithLabels(
                startIndex + 2, """
                    invoke-static {}, $ADS_CLASS_DESCRIPTOR->hideGetPremium()Z
                    move-result v$tempRegister
                    if-eqz v$tempRegister, :show
                    const/4 v$measuredWidthRegister, 0x0
                    const/4 v$measuredHeightRegister, 0x0
                    """,
                ExternalLabel("show", getInstruction(startIndex + 2))
            )
        }

        // endregion

        // region patch for hide end screen store banner

        fullScreenEngagementAdContainerFingerprint.mutableMethodOrThrow().apply {
            val addListIndex = indexOfAddListInstruction(this)
            val addListInstruction =
                getInstruction<FiveRegisterInstruction>(addListIndex)
            val listRegister = addListInstruction.registerC
            val objectRegister = addListInstruction.registerD

            replaceInstruction(
                addListIndex,
                "invoke-static { v$listRegister, v$objectRegister }, " +
                        "$ADS_CLASS_DESCRIPTOR->hideEndScreenStoreBanner(Ljava/util/List;Ljava/lang/Object;)V"
            )
        }

        // endregion

        // region patch for hide shorts ad

        // Hide Shorts ads by changing 'OSName' to 'Android Automotive'
        autoMotiveFingerprint.mutableMethodOrThrow().apply {
            val insertIndex = indexOfFirstStringInstructionOrThrow(ANDROID_AUTOMOTIVE_STRING) - 1
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstructions(
                insertIndex, """
                    invoke-static {v$insertRegister}, $ADS_CLASS_DESCRIPTOR->hideShortsAds(Z)Z
                    move-result v$insertRegister
                    """
            )
        }

        // If 'OSName' is changed to 'Android Automotive' in all requests, a Notification button will appear in the navigation bar
        // To fix this side effect, requests to the '/guide' endpoint, which are related to navigation buttons, use the original 'OSName'
        addClientOSVersionHook(
            "patch_setClientOSNameByAdsPatch",
            "$ADS_CLASS_DESCRIPTOR->overrideOSName()Ljava/lang/String;",
            is_20_06_or_greater
        )

        // endregion

        // region patch for hide paid promotion label in Shorts (non-litho)

        shortsPaidPromotionFingerprint.mutableMethodOrThrow().apply {
            when (returnType) {
                "Landroid/widget/TextView;" -> {
                    val insertIndex = implementation!!.instructions.lastIndex
                    val insertRegister =
                        getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex + 1, """
                            invoke-static {v$insertRegister}, $ADS_CLASS_DESCRIPTOR->hideShortsPaidPromotionLabel(Landroid/widget/TextView;)V
                            return-object v$insertRegister
                            """
                    )
                    removeInstruction(insertIndex)
                }

                "V" -> {
                    addInstructionsWithLabels(
                        0, """
                            invoke-static {}, $ADS_CLASS_DESCRIPTOR->hideShortsPaidPromotionLabel()Z
                            move-result v0
                            if-eqz v0, :show
                            return-void
                            """,
                        ExternalLabel("show", getInstruction(0))
                    )
                }

                else -> {
                    throw PatchException("Unknown returnType: $returnType")
                }
            }
        }

        // endregion

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: ADS"
            ),
            HIDE_ADS
        )

        // endregion
    }
)