/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.ads.general

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.extensions.InstructionExtensions.replaceInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patches.music.navigation.components.navigationBarComponentsPatch
import com.extenre.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.music.utils.extension.Constants.ADS_PATH
import com.extenre.patches.music.utils.extension.Constants.COMPONENTS_PATH
import com.extenre.patches.music.utils.mainactivity.mainActivityResolvePatch
import com.extenre.patches.music.utils.navigation.navigationBarHookPatch
import com.extenre.patches.music.utils.patch.PatchList.HIDE_ADS
import com.extenre.patches.music.utils.playservice.is_7_28_or_greater
import com.extenre.patches.music.utils.playservice.versionCheckPatch
import com.extenre.patches.music.utils.resourceid.buttonContainer
import com.extenre.patches.music.utils.resourceid.floatingLayout
import com.extenre.patches.music.utils.resourceid.privacyTosFooter
import com.extenre.patches.music.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.music.utils.settings.CategoryType
import com.extenre.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import com.extenre.patches.music.utils.settings.addSwitchPreference
import com.extenre.patches.music.utils.settings.settingsPatch
import com.extenre.patches.shared.ads.adsPatch
import com.extenre.patches.shared.litho.addLithoFilter
import com.extenre.patches.shared.litho.lithoFilterPatch
import com.extenre.patches.shared.mainactivity.onStartMethod
import com.extenre.patches.shared.mainactivity.onStopMethod
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
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

private const val ADS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/AdsFilter;"

private const val PREMIUM_PROMOTION_POP_UP_CLASS_DESCRIPTOR =
    "$ADS_PATH/PremiumPromotionPatch;"

private const val PREMIUM_PROMOTION_BANNER_CLASS_DESCRIPTOR =
    "$ADS_PATH/PremiumRenewalPatch;"

@Suppress("unused")
val adsPatch = adsPatch(
    block = {

        compatibleWith(COMPATIBLE_PACKAGE)

        dependsOn(
            settingsPatch,
            lithoFilterPatch,
            navigationBarComponentsPatch,
            navigationBarHookPatch,
            sharedResourceIdPatch,
            versionCheckPatch,
            mainActivityResolvePatch,
        )
    },
    classDescriptor = "$ADS_PATH/MusicAdsPatch;",
    methodDescriptor = "hideMusicAds",
    executeBlock = {
        // region patch for hide premium promotion popup

        floatingLayoutFingerprint.mutableMethodOrThrow().apply {
            val targetIndex = indexOfFirstLiteralInstructionOrThrow(floatingLayout) + 2
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, ${PREMIUM_PROMOTION_POP_UP_CLASS_DESCRIPTOR}->hidePremiumPromotionBottomSheet(Landroid/view/View;)V"
            )
        }

        if (is_7_28_or_greater) {
            mapOf(
                onStartMethod to "onAppForegrounded",
                onStopMethod to "onAppBackgrounded"
            ).forEach { (method, name) ->
                method.addInstruction(
                    0,
                    "invoke-static {}, ${PREMIUM_PROMOTION_POP_UP_CLASS_DESCRIPTOR}->$name()V"
                )
            }

            getPremiumDialogFingerprint
                .mutableMethodOrThrow(getPremiumDialogParentFingerprint)
                .apply {
                    val setContentViewIndex = indexOfSetContentViewInstruction(this)
                    val dialogInstruction =
                        getInstruction<FiveRegisterInstruction>(setContentViewIndex)
                    val dialogRegister = dialogInstruction.registerC
                    val viewRegister = dialogInstruction.registerD

                    replaceInstruction(
                        setContentViewIndex,
                        "invoke-static {v$dialogRegister, v$viewRegister}, " +
                                " ${PREMIUM_PROMOTION_POP_UP_CLASS_DESCRIPTOR}->hidePremiumPromotionDialog(Landroid/app/Dialog;Landroid/view/View;)V"
                    )
                }
        }

        // endregion

        // region patch for hide premium renewal banner

        notifierShelfFingerprint.mutableMethodOrThrow().apply {
            val linearLayoutIndex =
                indexOfFirstLiteralInstructionOrThrow(buttonContainer) + 3
            val linearLayoutRegister =
                getInstruction<OneRegisterInstruction>(linearLayoutIndex).registerA

            addInstruction(
                linearLayoutIndex + 1,
                "invoke-static {v$linearLayoutRegister}, ${PREMIUM_PROMOTION_BANNER_CLASS_DESCRIPTOR}->hidePremiumRenewal(Landroid/widget/LinearLayout;)V"
            )
        }

        // endregion

        // region patch for hide get premium

        // get premium button at the top
        val premiumMatch = getPremiumTextViewFingerprint.matchOrThrow()
        val premiumMethod = premiumMatch.method
        val premiumClassDef = premiumMatch.classDef
        val premiumMutableMethod = mutableClassDefBy(premiumClassDef.type).methods.first {
            MethodUtil.methodSignaturesMatch(it, premiumMethod)
        }
        premiumMutableMethod.apply {
            val insertIndex = premiumMatch.patternMatch!!.startIndex
            val register = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

            addInstruction(
                insertIndex + 1,
                "const/4 v$register, 0x0"
            )
        }

        // get premium button at the bottom
        accountMenuFooterFingerprint.mutableMethodOrThrow().apply {
            val constIndex = indexOfFirstLiteralInstructionOrThrow(privacyTosFooter)
            val walkerIndex = indexOfFirstInstructionOrThrow(constIndex + 2, Opcode.INVOKE_VIRTUAL)
            val viewIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.IGET_OBJECT)
            val viewReference = getInstruction<ReferenceInstruction>(viewIndex).reference.toString()

            // Obtener la instrucción que hace la llamada
            val invokeInstruction = getInstruction<ReferenceInstruction>(walkerIndex)
            val methodRef = invokeInstruction.reference as? MethodReference
                ?: throw PatchException("Could not get method reference at index $walkerIndex")

            // Buscar el método mutable en la clase correspondiente
            val targetClass = methodRef.definingClass
            val targetMethod = mutableClassDefBy(targetClass).methods.first {
                it.name == methodRef.name && it.parameterTypes == methodRef.parameterTypes
            }

            targetMethod.apply {
                val insertIndex = indexOfFirstInstructionOrThrow {
                    getReference<FieldReference>()?.toString() == viewReference
                }
                val nullCheckIndex = indexOfFirstInstructionOrThrow(insertIndex - 1, Opcode.IF_NEZ)
                val nullCheckRegister = getInstruction<OneRegisterInstruction>(nullCheckIndex).registerA

                addInstruction(
                    nullCheckIndex,
                    "const/4 v$nullCheckRegister, 0x0"
                )
            }
        }

        addLithoFilter(ADS_FILTER_CLASS_DESCRIPTOR)

        // endregion

        addSwitchPreference(
            CategoryType.ADS,
            "extenre_hide_fullscreen_ads",
            "false"
        )
        addSwitchPreference(
            CategoryType.ADS,
            "extenre_hide_general_ads",
            "true"
        )
        addSwitchPreference(
            CategoryType.ADS,
            "extenre_hide_music_ads",
            "true"
        )
        addSwitchPreference(
            CategoryType.ADS,
            "extenre_hide_paid_promotion_label",
            "true"
        )
        addSwitchPreference(
            CategoryType.ADS,
            "extenre_hide_premium_promotion",
            "true"
        )
        addSwitchPreference(
            CategoryType.ADS,
            "extenre_hide_premium_renewal",
            "true"
        )
        addSwitchPreference(
            CategoryType.ADS,
            "extenre_hide_promotion_alert_banner",
            "true"
        )

        updatePatchStatus(HIDE_ADS)
    }
)

// Función auxiliar para encontrar la instrucción setContentView
private fun indexOfSetContentViewInstruction(method: com.extenre.patcher.util.proxy.mutableTypes.MutableMethod): Int {
    return method.indexOfFirstInstructionOrThrow {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "setContentView"
    }
}