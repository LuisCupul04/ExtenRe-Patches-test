/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.video.playback

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.music.utils.extension.Constants.VIDEO_PATH
import com.extenre.patches.music.utils.patch.PatchList.VIDEO_PLAYBACK
import com.extenre.patches.music.utils.playbackSpeedBottomSheetFingerprint
import com.extenre.patches.music.utils.playbackSpeedFingerprint
import com.extenre.patches.music.utils.playbackSpeedParentFingerprint
import com.extenre.patches.music.utils.playservice.is_7_13_or_greater
import com.extenre.patches.music.utils.playservice.versionCheckPatch
import com.extenre.patches.music.utils.resourceid.qualityAuto
import com.extenre.patches.music.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.music.utils.settings.CategoryType
import com.extenre.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import com.extenre.patches.music.utils.settings.addPreferenceWithIntent
import com.extenre.patches.music.utils.settings.addSwitchPreference
import com.extenre.patches.music.utils.settings.settingsPatch
import com.extenre.patches.music.video.information.onCreateHook
import com.extenre.patches.music.video.information.videoInformationPatch
import com.extenre.patches.shared.FIXED_RESOLUTION_STRING
import com.extenre.patches.shared.customspeed.customPlaybackSpeedPatch
import com.extenre.patches.shared.drc.drcAudioPatch
import com.extenre.patches.shared.opus.baseOpusCodecsPatch
import com.extenre.patches.shared.playbackStartParametersConstructorFingerprint
import com.extenre.patches.shared.playbackStartParametersToStringFingerprint
import com.extenre.util.findFieldFromToString
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.fingerprint.mutableClassOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstruction
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.extenre.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/PlaybackSpeedPatch;"
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
            5.0f
        ),
        baseOpusCodecsPatch(),
        drcAudioPatch { is_7_13_or_greater },
        sharedResourceIdPatch,
        videoInformationPatch,
    )

    execute {
        // region patch for default playback speed

        playbackSpeedBottomSheetFingerprint.mutableClassOrThrow().let {
            val onItemClickMethod =
                it.methods.find { method -> method.name == "onItemClick" }
                    ?: throw PatchException("Failed to find onItemClick method")

            onItemClickMethod.apply {
                val targetIndex = indexOfFirstInstructionOrThrow(Opcode.IGET)
                val targetRegister =
                    getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->userSelectedPlaybackSpeed(F)V"
                )
            }
        }

        playbackSpeedFingerprint.matchOrThrow(playbackSpeedParentFingerprint).let { match ->
            val method = match.method
            val classDef = match.classDef
            val mutableMethod = mutableClassDefBy(classDef.type).methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.apply {
                val startIndex = match.patternMatch!!.startIndex
                val speedRegister =
                    getInstruction<OneRegisterInstruction>(startIndex + 1).registerA

                addInstructions(
                    startIndex + 2, """
                        invoke-static {v$speedRegister}, $EXTENSION_PLAYBACK_SPEED_CLASS_DESCRIPTOR->getPlaybackSpeed(F)F
                        move-result v$speedRegister
                        """
                )
            }
        }

        // endregion

        // region patch for default video quality

        val videoQualityMatch = videoQualityListFingerprint.matchOrThrow()
        val videoQualityMethod = videoQualityMatch.method
        val videoQualityClassDef = videoQualityMatch.classDef
        val videoQualityMutableMethod = mutableClassDefBy(videoQualityClassDef.type).methods.first {
            MethodUtil.methodSignaturesMatch(it, videoQualityMethod)
        }

        // Obtenemos el descriptor de la clase de calidad (ej: "Lcom/example/VideoQuality;")
        val videoQualityClass = videoQualityMutableMethod.run {
            val listIndex = videoQualityMatch.patternMatch!!.startIndex
            val listRegister = getInstruction<FiveRegisterInstruction>(listIndex).registerD

            addInstruction(
                listIndex,
                "invoke-static {v$listRegister}, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->setVideoQualities([Ljava/lang/Object;)V"
            )

            val literalIndex = indexOfFirstLiteralInstructionOrThrow(qualityAuto)
            val qualityClassIndex =
                indexOfFirstInstructionReversedOrThrow(literalIndex + 1, Opcode.NEW_INSTANCE)
            // Extraemos la referencia de la clase (String)
            getInstruction<ReferenceInstruction>(qualityClassIndex).reference.toString()
        }

        // Funciones auxiliares (no usan contexto de mutabilidad)
        fun indexOfVideoQualityNameFieldInstruction(method: Method) =
            method.indexOfFirstInstruction {
                val reference = getReference<FieldReference>()
                opcode == Opcode.IPUT_OBJECT &&
                        reference?.type == "Ljava/lang/String;" &&
                        reference.definingClass == method.definingClass
            }

        fun indexOfVideoQualityResolutionFieldInstruction(method: Method) =
            method.indexOfFirstInstruction {
                val reference = getReference<FieldReference>()
                opcode == Opcode.IPUT &&
                        reference?.type == "I" &&
                        reference.definingClass == method.definingClass
            }

        // Obtenemos la clase mutable de la calidad
        val videoQualityMutableClass = mutableClassDefBy(videoQualityClass)

        // Buscamos el constructor que tiene los campos de nombre y resolución
        val qualityConstructor = videoQualityMutableClass.methods.first { method ->
            MethodUtil.isConstructor(method) &&
                    method.parameterTypes.size > 3 &&
                    indexOfVideoQualityNameFieldInstruction(method) >= 0 &&
                    indexOfVideoQualityResolutionFieldInstruction(method) >= 0
        }

        // Modificamos el constructor
        qualityConstructor.apply {
            val qualityNameIndex = indexOfVideoQualityNameFieldInstruction(this)
            val resolutionIndex = indexOfVideoQualityResolutionFieldInstruction(this)
            val resolutionField = getInstruction<ReferenceInstruction>(resolutionIndex).reference
            val qualityNameRegister = getInstruction<TwoRegisterInstruction>(qualityNameIndex).registerA
            val resolutionRegister = getInstruction<TwoRegisterInstruction>(resolutionIndex).registerA

            addInstructions(
                0, """
                    invoke-static { v$resolutionRegister, v$qualityNameRegister }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->fixVideoQualityResolution(ILjava/lang/String;)I
                    move-result v$resolutionRegister
                    """
            )
        }

        // Añadimos el método en la extensión
        val extensionClass = mutableClassDefBy(EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR)
        val extensionMethod = extensionClass.methods.find { it.name == "getVideoQualityResolution" }
            ?: throw PatchException("Method getVideoQualityResolution not found in $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR")
        extensionMethod.addInstructions(
            0, """
                check-cast p0, $videoQualityClass
                iget p0, p0, ${qualityConstructor.getInstruction<ReferenceInstruction>(indexOfVideoQualityResolutionFieldInstruction(qualityConstructor)).reference}
                return p0
                """
        )

        // Campo de resolución inicial (desde el fingerprint)
        val initialResolutionField = run {
            val match = playbackStartParametersToStringFingerprint.matchOrThrow()
            val method = match.method
            val classDef = match.classDef
            val mutableMethod = mutableClassDefBy(classDef.type).methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.findFieldFromToString(FIXED_RESOLUTION_STRING)
        }

        // Hook para el constructor de parámetros de inicio
        playbackStartParametersConstructorFingerprint
            .mutableMethodOrThrow(playbackStartParametersToStringFingerprint)
            .apply {
                val index = indexOfFirstInstructionReversedOrThrow {
                    opcode == Opcode.IPUT_OBJECT &&
                            getReference<FieldReference>() == initialResolutionField
                }
                val register = getInstruction<TwoRegisterInstruction>(index).registerA

                addInstructions(
                    index, """
                        invoke-static {v$register}, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->getInitialVideoQuality(Lj${'$'}/util/Optional;)Lj${'$'}/util/Optional;
                        move-result-object v$register
                        """
                )
            }

        // Hook para cuando se inicia un video
        onCreateHook(EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR, "newVideoStarted")

        // Hook para cuando el usuario cambia la calidad
        userQualityChangeFingerprint.matchOrThrow().let { match ->
            val method = match.method
            val classDef = match.classDef
            val mutableMethod = mutableClassDefBy(classDef.type).methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.apply {
                val endIndex = match.patternMatch!!.endIndex
                val qualityChangedClass =
                    getInstruction<ReferenceInstruction>(endIndex).reference.toString()

                val changedClass = mutableClassDefBy(qualityChangedClass)
                val onItemClickMethod = changedClass.methods.find { it.name == "onItemClick" }
                    ?: throw PatchException("Method onItemClick not found in $qualityChangedClass")
                onItemClickMethod.addInstruction(
                    0,
                    "invoke-static { p3 }, $EXTENSION_VIDEO_QUALITY_CLASS_DESCRIPTOR->userSelectedVideoQuality(I)V"
                )
            }
        }

        // endregion

        addSwitchPreference(
            CategoryType.VIDEO,
            "extenre_disable_drc_audio",
            "false"
        )
        addPreferenceWithIntent(
            CategoryType.VIDEO,
            "extenre_custom_playback_speeds"
        )
        addSwitchPreference(
            CategoryType.VIDEO,
            "extenre_enable_opus_codec",
            "true"
        )
        addSwitchPreference(
            CategoryType.VIDEO,
            "extenre_remember_playback_speed_last_selected",
            "true"
        )
        addSwitchPreference(
            CategoryType.VIDEO,
            "extenre_remember_playback_speed_last_selected_toast",
            "true",
            "extenre_remember_playback_speed_last_selected"
        )
        addSwitchPreference(
            CategoryType.VIDEO,
            "extenre_remember_video_quality_last_selected",
            "true"
        )
        addSwitchPreference(
            CategoryType.VIDEO,
            "extenre_remember_video_quality_last_selected_toast",
            "true",
            "extenre_remember_video_quality_last_selected"
        )

        updatePatchStatus(VIDEO_PLAYBACK)
    }
}