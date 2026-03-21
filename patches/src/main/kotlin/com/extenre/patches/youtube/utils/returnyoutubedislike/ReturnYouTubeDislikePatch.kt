/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.utils.returnyoutubedislike

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.extensions.InstructionExtensions.removeInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.smali.ExternalLabel
import com.extenre.patches.shared.dislikeFingerprint
import com.extenre.patches.shared.likeFingerprint
import com.extenre.patches.shared.litho.addLithoFilter
import com.extenre.patches.shared.litho.lithoFilterPatch
import com.extenre.patches.shared.removeLikeFingerprint
import com.extenre.patches.shared.textcomponent.hookSpannableString
import com.extenre.patches.shared.textcomponent.hookTextComponent
import com.extenre.patches.shared.textcomponent.textComponentPatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import com.extenre.patches.youtube.utils.extension.Constants.UTILS_PATH
import com.extenre.patches.youtube.utils.fix.litho.lithoLayoutPatch
import com.extenre.patches.youtube.utils.patch.PatchList.RETURN_YOUTUBE_DISLIKE
import com.extenre.patches.youtube.utils.playservice.is_18_34_or_greater
import com.extenre.patches.youtube.utils.playservice.is_18_49_or_greater
import com.extenre.patches.youtube.utils.playservice.is_20_07_or_greater
import com.extenre.patches.youtube.utils.playservice.versionCheckPatch
import com.extenre.patches.youtube.utils.rollingNumberTextViewAnimationUpdateFingerprint
import com.extenre.patches.youtube.utils.rollingNumberTextViewFingerprint
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.patches.youtube.video.information.hookShortsVideoInformation
import com.extenre.patches.youtube.video.information.videoInformationPatch
import com.extenre.patches.youtube.video.videoid.hookPlayerResponseVideoId
import com.extenre.patches.youtube.video.videoid.hookVideoId
import com.extenre.util.findFreeRegister
import com.extenre.util.fingerprint.injectLiteralInstructionBooleanCall
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_RYD_CLASS_DESCRIPTOR =
    "$UTILS_PATH/ReturnYouTubeDislikePatch;"

private val returnYouTubeDislikeRollingNumberPatch = bytecodePatch(
    name = "return-YouTube-Dislike-Rolling-Number-Patch",
    description = "returnYouTubeDislikeRollingNumberPatch"
) {
    dependsOn(versionCheckPatch)

    execute {
        if (!is_18_49_or_greater) {
            return@execute
        }

        rollingNumberSetterFingerprint.matchOrThrow().let { match ->
            val method = match.method
            val classDef = match.classDef
            val mutableMethod = mutableClassDefBy(classDef.type).methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.apply {
                val rollingNumberClassIndex = match.patternMatch!!.startIndex
                val rollingNumberClassReference =
                    getInstruction<ReferenceInstruction>(rollingNumberClassIndex).reference.toString()
                val rollingNumberConstructorMethod = mutableClassDefBy(rollingNumberClassReference).methods.first { m ->
                    MethodUtil.isConstructor(m)
                }
                val charSequenceFieldReference = with(rollingNumberConstructorMethod) {
                    getInstruction<ReferenceInstruction>(
                        indexOfFirstInstructionOrThrow(Opcode.IPUT_OBJECT)
                    ).reference
                }

                val insertIndex = rollingNumberClassIndex + 1
                val charSequenceInstanceRegister =
                    getInstruction<OneRegisterInstruction>(rollingNumberClassIndex).registerA

                val conversionContextRegister = implementation!!.registerCount - parameters.size + 1
                val freeRegister = findFreeRegister(
                    insertIndex,
                    charSequenceInstanceRegister,
                    conversionContextRegister
                )

                addInstructions(
                    insertIndex, """
                        iget-object v$freeRegister, v$charSequenceInstanceRegister, $charSequenceFieldReference
                        invoke-static {v$conversionContextRegister, v$freeRegister}, $EXTENSION_RYD_CLASS_DESCRIPTOR->onRollingNumberLoaded(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$freeRegister
                        iput-object v$freeRegister, v$charSequenceInstanceRegister, $charSequenceFieldReference
                        """
                )
            }
        }

        rollingNumberMeasureAnimatedTextFingerprint.matchOrThrow().let { match ->
            val method = match.method
            val classDef = match.classDef
            val mutableMethod = mutableClassDefBy(classDef.type).methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.apply {
                val endIndex = match.patternMatch!!.endIndex
                val measuredTextWidthIndex = endIndex - 2
                val measuredTextWidthRegister =
                    getInstruction<TwoRegisterInstruction>(measuredTextWidthIndex).registerA

                addInstructions(
                    endIndex + 1, """
                        invoke-static {p1, v$measuredTextWidthRegister}, $EXTENSION_RYD_CLASS_DESCRIPTOR->onRollingNumberMeasured(Ljava/lang/String;F)F
                        move-result v$measuredTextWidthRegister
                        """
                )

                val ifGeIndex = indexOfFirstInstructionOrThrow(Opcode.IF_GE)
                val ifGeInstruction = getInstruction<TwoRegisterInstruction>(ifGeIndex)

                removeInstruction(ifGeIndex)
                addInstructionsWithLabels(
                    ifGeIndex, """
                        if-ge v${ifGeInstruction.registerA}, v${ifGeInstruction.registerB}, :jump
                        """, ExternalLabel("jump", getInstruction(endIndex))
                )
            }
        }

        rollingNumberMeasureStaticLabelFingerprint.matchOrThrow(
            rollingNumberMeasureTextParentFingerprint
        ).let { match ->
            val method = match.method
            val classDef = match.classDef
            val mutableMethod = mutableClassDefBy(classDef.type).methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.apply {
                val measureTextIndex = match.patternMatch!!.startIndex + 1
                val freeRegister = getInstruction<TwoRegisterInstruction>(0).registerA

                addInstructions(
                    measureTextIndex + 1, """
                        move-result v$freeRegister
                        invoke-static {p1, v$freeRegister}, $EXTENSION_RYD_CLASS_DESCRIPTOR->onRollingNumberMeasured(Ljava/lang/String;F)F
                        """
                )
            }
        }

        arrayOf(
            rollingNumberTextViewFingerprint.mutableMethodOrThrow(),
            rollingNumberTextViewAnimationUpdateFingerprint.mutableMethodOrThrow(rollingNumberTextViewFingerprint)
        ).forEach { mutableMethod ->
            mutableMethod.apply {
                val setTextIndex = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.name == "setText"
                }
                val textViewRegister = getInstruction<FiveRegisterInstruction>(setTextIndex).registerC
                val textSpanRegister = getInstruction<FiveRegisterInstruction>(setTextIndex).registerD

                addInstructions(
                    setTextIndex, """
                        invoke-static {v$textViewRegister, v$textSpanRegister}, $EXTENSION_RYD_CLASS_DESCRIPTOR->updateRollingNumber(Landroid/widget/TextView;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                        move-result-object v$textSpanRegister
                        """
                )
            }
        }
    }
}

private val returnYouTubeDislikeShortsPatch = bytecodePatch(
    name = "return-YouTube-Dislike-Shorts-Patch",
    description = "returnYouTubeDislikeShortsPatch"
) {
    dependsOn(textComponentPatch, versionCheckPatch)

    execute {
        shortsTextViewFingerprint.matchOrThrow().let { match ->
            val method = match.method
            val classDef = match.classDef
            val mutableMethod = mutableClassDefBy(classDef.type).methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.apply {
                val startIndex = match.patternMatch!!.startIndex

                val isDisLikesBooleanIndex = indexOfFirstInstructionReversedOrThrow(startIndex, Opcode.IGET_BOOLEAN)
                val textViewFieldIndex = indexOfFirstInstructionReversedOrThrow(startIndex, Opcode.IGET_OBJECT)

                val isDisLikesBooleanReference = getInstruction<ReferenceInstruction>(isDisLikesBooleanIndex).reference
                val textViewFieldReference = getInstruction<ReferenceInstruction>(textViewFieldIndex).reference

                val insertIndex = indexOfFirstInstructionOrThrow(Opcode.CHECK_CAST) + 1

                addInstructionsWithLabels(
                    insertIndex, """
                    iget-boolean v0, p0, $isDisLikesBooleanReference
                    if-eqz v0, :ryd_disabled
                    iget-object v0, p0, $textViewFieldReference
                    invoke-static {v0}, $EXTENSION_RYD_CLASS_DESCRIPTOR->setShortsDislikes(Landroid/view/View;)Z
                    move-result v0
                    if-eqz v0, :ryd_disabled
                    return-void
                    """, ExternalLabel("ryd_disabled", getInstruction(insertIndex))
                )
            }
        }

        if (is_18_34_or_greater) {
            hookSpannableString(EXTENSION_RYD_CLASS_DESCRIPTOR, "onCharSequenceLoaded")
        }
    }
}

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/ReturnYouTubeDislikeFilterPatch;"

@Suppress("unused")
val returnYouTubeDislikePatch = bytecodePatch(
    name = RETURN_YOUTUBE_DISLIKE.key,
    description = "${RETURN_YOUTUBE_DISLIKE.title}: ${RETURN_YOUTUBE_DISLIKE.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        returnYouTubeDislikeRollingNumberPatch,
        returnYouTubeDislikeShortsPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        videoInformationPatch,
    )

    execute {
        mapOf(
            likeFingerprint to Vote.LIKE,
            dislikeFingerprint to Vote.DISLIKE,
            removeLikeFingerprint to Vote.REMOVE_LIKE,
        ).forEach { (fingerprint, vote) ->
            fingerprint.mutableMethodOrThrow().addInstructions(
                0, """
                    const/4 v0, ${vote.value}
                    invoke-static {v0}, $EXTENSION_RYD_CLASS_DESCRIPTOR->sendVote(I)V
                    """
            )
        }

        hookTextComponent(EXTENSION_RYD_CLASS_DESCRIPTOR)

        hookVideoId("$EXTENSION_RYD_CLASS_DESCRIPTOR->newVideoLoaded(Ljava/lang/String;)V")
        hookPlayerResponseVideoId("$EXTENSION_RYD_CLASS_DESCRIPTOR->preloadVideoId(Ljava/lang/String;Z)V")

        if (is_18_34_or_greater) {
            addLithoFilter(FILTER_CLASS_DESCRIPTOR)
            hookPlayerResponseVideoId("$FILTER_CLASS_DESCRIPTOR->newPlayerResponseVideoId(Ljava/lang/String;Z)V")
            hookShortsVideoInformation("$FILTER_CLASS_DESCRIPTOR->newShortsVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
        }

        if (is_20_07_or_greater) {
            textComponentFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                LITHO_NEW_TEXT_COMPONENT_FEATURE_FLAG,
                "0x0"
            )
        }

        addPreference(
            arrayOf("PREFERENCE_SCREEN: RETURN_YOUTUBE_DISLIKE"),
            RETURN_YOUTUBE_DISLIKE
        )
    }
}

enum class Vote(val value: Int) {
    LIKE(1),
    DISLIKE(-1),
    REMOVE_LIKE(0),
}