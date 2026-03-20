/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.actionbar.components

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.extensions.InstructionExtensions.removeInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.smali.ExternalLabel
import com.extenre.patches.music.utils.ACTION_BAR_POSITION_FEATURE_FLAG
import com.extenre.patches.music.utils.actionBarPositionFeatureFlagFingerprint
import com.extenre.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.music.utils.extension.Constants.ACTIONBAR_CLASS_DESCRIPTOR
import com.extenre.patches.music.utils.extension.Constants.COMPONENTS_PATH
import com.extenre.patches.music.utils.patch.PatchList.HIDE_ACTION_BAR_COMPONENTS
import com.extenre.patches.music.utils.playservice.is_7_17_or_greater
import com.extenre.patches.music.utils.playservice.is_7_25_or_greater
import com.extenre.patches.music.utils.playservice.is_7_33_or_greater
import com.extenre.patches.music.utils.playservice.versionCheckPatch
import com.extenre.patches.music.utils.resourceid.elementsLottieAnimationViewTagId
import com.extenre.patches.music.utils.resourceid.likeDislikeContainer
import com.extenre.patches.music.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.music.utils.settings.CategoryType
import com.extenre.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import com.extenre.patches.music.utils.settings.addPreferenceWithIntent
import com.extenre.patches.music.utils.settings.addSwitchPreference
import com.extenre.patches.music.utils.settings.settingsPatch
import com.extenre.patches.music.video.information.videoInformationPatch
import com.extenre.patches.shared.litho.addLithoFilter
import com.extenre.patches.shared.litho.lithoFilterPatch
import com.extenre.patches.shared.textcomponent.hookSpannableString
import com.extenre.patches.shared.textcomponent.textComponentPatch
import com.extenre.util.fingerprint.injectLiteralInstructionBooleanCall
import com.extenre.util.fingerprint.legacyFingerprint
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.methodOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.extenre.util.indexOfFirstLiteralInstructionOrThrow
import com.extenre.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil
import kotlin.math.min

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/ActionButtonsFilter;"

@Suppress("unused")
val actionBarComponentsPatch = bytecodePatch(
    name = HIDE_ACTION_BAR_COMPONENTS.key,
    description = "${HIDE_ACTION_BAR_COMPONENTS.title}: ${HIDE_ACTION_BAR_COMPONENTS.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        sharedResourceIdPatch,
        textComponentPatch,
        videoInformationPatch,
        versionCheckPatch,
    )

    execute {
        if (is_7_17_or_greater) {
            addLithoFilter(FILTER_CLASS_DESCRIPTOR)
            hookSpannableString(ACTIONBAR_CLASS_DESCRIPTOR, "onLithoTextLoaded")

            commandResolverFingerprint.mutableMethodOrThrow().addInstruction(
                0,
                "invoke-static {p2}, $ACTIONBAR_CLASS_DESCRIPTOR->inAppDownloadButtonOnClick(Ljava/util/Map;)Z"
            )

            offlineVideoEndpointFingerprint.mutableMethodOrThrow().addInstructionsWithLabels(
                0, """
                    invoke-static {p2}, $ACTIONBAR_CLASS_DESCRIPTOR->inAppDownloadButtonOnClick(Ljava/util/Map;)Z
                    move-result v0
                    if-eqz v0, :ignore
                    return-void
                    :ignore
                    nop
                    """
            )

            if (is_7_25_or_greater) {
                actionBarPositionFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                    ACTION_BAR_POSITION_FEATURE_FLAG,
                    "$ACTIONBAR_CLASS_DESCRIPTOR->changeActionBarPosition(Z)Z"
                )

                addSwitchPreference(
                    CategoryType.ACTION_BAR,
                    "extenre_change_action_bar_position",
                    "false"
                )
            }
        }

        if (!is_7_25_or_greater) {
            val actionBarComponentMatch = actionBarComponentFingerprint.matchOrThrow()
            val actionBarComponentMethod = actionBarComponentMatch.method
            val actionBarComponentClassDef = actionBarComponentMatch.classDef
            val actionBarComponentMutableMethod = proxy(actionBarComponentClassDef).mutableClass.methods.first {
                MethodUtil.methodSignaturesMatch(it, actionBarComponentMethod)
            }

            actionBarComponentMutableMethod.apply {
                // hook download button
                val addViewIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "addView"
                }
                val addViewRegister =
                    getInstruction<FiveRegisterInstruction>(addViewIndex).registerD

                addInstruction(
                    addViewIndex + 1,
                    "invoke-static {v$addViewRegister}, $ACTIONBAR_CLASS_DESCRIPTOR->inAppDownloadButtonOnClick(Landroid/view/View;)V"
                )

                // hide action button label
                val noLabelIndex = indexOfFirstInstructionOrThrow {
                    val reference = (this as? ReferenceInstruction)?.reference.toString()
                    opcode == Opcode.INVOKE_DIRECT &&
                            reference.endsWith("<init>(Landroid/content/Context;)V") &&
                            !reference.contains("Lcom/google/android/libraries/youtube/common/ui/YouTubeButton;")
                } - 2
                val replaceIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_DIRECT &&
                            (this as? ReferenceInstruction)?.reference.toString()
                                    .endsWith("Lcom/google/android/libraries/youtube/common/ui/YouTubeButton;-><init>(Landroid/content/Context;)V")
                } - 2
                val replaceInstruction = getInstruction<TwoRegisterInstruction>(replaceIndex)
                val replaceReference =
                    getInstruction<ReferenceInstruction>(replaceIndex).reference

                addInstructionsWithLabels(
                    replaceIndex + 1, """
                            invoke-static {}, $ACTIONBAR_CLASS_DESCRIPTOR->hideActionBarLabel()Z
                            move-result v${replaceInstruction.registerA}
                            if-nez v${replaceInstruction.registerA}, :hidden
                            iget-object v${replaceInstruction.registerA}, v${replaceInstruction.registerB}, $replaceReference
                            """, ExternalLabel("hidden", getInstruction(noLabelIndex))
                )
                removeInstruction(replaceIndex)

                // hide action button
                val hasNextIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_INTERFACE &&
                            getReference<MethodReference>()?.name == "hasNext"
                }
                val freeRegister = min(implementation!!.registerCount - parameters.size - 2, 15)

                val spannedIndex = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.returnType == "Landroid/text/Spanned;"
                }
                val spannedRegister =
                    getInstruction<FiveRegisterInstruction>(spannedIndex).registerC
                val spannedReference =
                    getInstruction<ReferenceInstruction>(spannedIndex).reference

                addInstructionsWithLabels(
                    spannedIndex + 1, """
                            invoke-static {}, $ACTIONBAR_CLASS_DESCRIPTOR->hideActionButton()Z
                            move-result v$freeRegister
                            if-nez v$freeRegister, :hidden
                            invoke-static {v$spannedRegister}, $spannedReference
                            """, ExternalLabel("hidden", getInstruction(hasNextIndex))
                )
                removeInstruction(spannedIndex)

                // set action button identifier
                val buttonTypeDownloadIndex = actionBarComponentMatch.patternMatch!!.startIndex + 1
                val buttonTypeDownloadRegister =
                    getInstruction<OneRegisterInstruction>(buttonTypeDownloadIndex).registerA

                val buttonTypeIndex = actionBarComponentMatch.patternMatch!!.endIndex - 1
                val buttonTypeRegister =
                    getInstruction<OneRegisterInstruction>(buttonTypeIndex).registerA

                addInstruction(
                    buttonTypeIndex + 2,
                    "invoke-static {v$buttonTypeRegister}, $ACTIONBAR_CLASS_DESCRIPTOR->setButtonType(Ljava/lang/Object;)V"
                )

                addInstruction(
                    buttonTypeDownloadIndex,
                    "invoke-static {v$buttonTypeDownloadRegister}, $ACTIONBAR_CLASS_DESCRIPTOR->setButtonTypeDownload(I)V"
                )
            }
        }

        likeDislikeContainerFingerprint.mutableMethodOrThrow().apply {
            val insertIndex =
                indexOfFirstLiteralInstructionOrThrow(likeDislikeContainer) + 2
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstruction(
                insertIndex + 1,
                "invoke-static {v$insertRegister}, $ACTIONBAR_CLASS_DESCRIPTOR->hideLikeDislikeButton(Landroid/view/View;)V"
            )
        }

        val (abstractClass, lottieAnimationUrlMethodName) =
            with (lottieAnimationViewTagFingerprint.mutableMethodOrThrow()) {
                val literalIndex =
                    indexOfFirstLiteralInstructionOrThrow(elementsLottieAnimationViewTagId)
                val lottieAnimationUrlIndex =
                    indexOfFirstInstructionReversedOrThrow(literalIndex) {
                        val reference = getReference<MethodReference>()
                        opcode == Opcode.INVOKE_INTERFACE &&
                                reference?.returnType == "Ljava/lang/String;" &&
                                reference.parameterTypes.isEmpty()
                    }

                val lottieAnimationUrlMethodReference =
                    getInstruction<ReferenceInstruction>(lottieAnimationUrlIndex).reference as MethodReference

                Pair(
                    lottieAnimationUrlMethodReference.definingClass,
                    lottieAnimationUrlMethodReference.name,
                )
            }

        val lottieAnimationUrlFingerprint = legacyFingerprint(
            name = "lottieAnimationUrlFingerprint",
            returnType = "Ljava/lang/String;",
            accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
            parameters = emptyList(),
            customFingerprint = { method, classDef ->
                classDef.interfaces.contains(abstractClass) &&
                        method.name == lottieAnimationUrlMethodName &&
                        classDef.fields.find { it.type.endsWith("Lcom/google/android/libraries/elements/adl/UpbMiniTable;") } == null
            }
        )

        lottieAnimationUrlFingerprint.mutableMethodOrThrow().apply {
            val index = implementation!!.instructions.lastIndex
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            addInstructions(
                index, """
                    invoke-static { v$register }, $ACTIONBAR_CLASS_DESCRIPTOR->replaceLikeButton(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$register
                    """
            )
        }

        addSwitchPreference(
            CategoryType.ACTION_BAR,
            "extenre_hide_action_button_like_dislike",
            "false"
        )
        addSwitchPreference(
            CategoryType.ACTION_BAR,
            "extenre_hide_action_button_comment",
            "false"
        )
        addSwitchPreference(
            CategoryType.ACTION_BAR,
            "extenre_hide_action_button_add_to_playlist",
            "false"
        )
        addSwitchPreference(
            CategoryType.ACTION_BAR,
            "extenre_hide_action_button_download",
            "false"
        )
        addSwitchPreference(
            CategoryType.ACTION_BAR,
            "extenre_hide_action_button_radio",
            "false"
        )
        addSwitchPreference(
            CategoryType.ACTION_BAR,
            "extenre_hide_action_button_share",
            "false"
        )
        if (is_7_33_or_greater) {
            addSwitchPreference(
                CategoryType.ACTION_BAR,
                "extenre_hide_action_button_song_video",
                "false"
            )
        }
        if (is_7_25_or_greater) {
            addSwitchPreference(
                CategoryType.ACTION_BAR,
                "extenre_hide_action_button_disabled",
                "false"
            )
        } else {
            addSwitchPreference(
                CategoryType.ACTION_BAR,
                "extenre_hide_action_button_label",
                "false"
            )
        }
        addSwitchPreference(
            CategoryType.ACTION_BAR,
            "extenre_external_downloader_action",
            "false"
        )
        addPreferenceWithIntent(
            CategoryType.ACTION_BAR,
            "extenre_external_downloader_package_name",
            "extenre_external_downloader_action"
        )
        addSwitchPreference(
            CategoryType.ACTION_BAR,
            "extenre_replace_action_button_like",
            "false"
        )
        addSwitchPreference(
            CategoryType.ACTION_BAR,
            "extenre_replace_action_button_like_type",
            "false",
            "extenre_replace_action_button_like"
        )

        updatePatchStatus(HIDE_ACTION_BAR_COMPONENTS)

    }
}
