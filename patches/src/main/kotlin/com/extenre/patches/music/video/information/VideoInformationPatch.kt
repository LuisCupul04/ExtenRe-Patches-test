/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.video.information

import com.extenre.patcher.Fingerprint
import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.proxy.mutableTypes.MutableClass
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.extenre.patcher.util.smali.toInstructions
import com.extenre.patches.music.utils.extension.Constants.SHARED_PATH
import com.extenre.patches.music.utils.playbackSpeedFingerprint
import com.extenre.patches.music.utils.playbackSpeedParentFingerprint
import com.extenre.patches.music.video.playerresponse.Hook
import com.extenre.patches.music.video.playerresponse.addPlayerResponseMethodHook
import com.extenre.patches.music.video.playerresponse.playerResponseMethodHookPatch
import com.extenre.patches.shared.mdxPlayerDirectorSetVideoStageFingerprint
import com.extenre.patches.shared.videoLengthFingerprint
import com.extenre.util.addStaticFieldToExtension
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.fingerprint.mutableClassOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.util.MethodUtil

const val EXTENSION_CLASS_DESCRIPTOR =
    "$SHARED_PATH/VideoInformation;"

private const val REGISTER_PLAYER_RESPONSE_MODEL = 4
private const val REGISTER_VIDEO_ID = 0
private const val REGISTER_VIDEO_LENGTH = 1
private const val REGISTER_VIDEO_LENGTH_DUMMY = 2

private lateinit var PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR: String
private lateinit var videoIdMethodCall: String
private lateinit var videoLengthMethodCall: String

private lateinit var videoInformationMethod: MutableMethod

private var seekSourceEnumType = ""
private var seekSourceMethodName = ""

private lateinit var playerConstructorMethod: MutableMethod
private var playerConstructorInsertIndex = -1

private lateinit var mdxConstructorMethod: MutableMethod
private var mdxConstructorInsertIndex = -1

private lateinit var videoTimeConstructorMethod: MutableMethod
private var videoTimeConstructorInsertIndex = 2

val videoInformationPatch = bytecodePatch(
    name = "video-information-patch",
    description = "videoInformationPatch"
) {
    dependsOn(playerResponseMethodHookPatch)

    execute {
        fun addSeekInterfaceMethods(
            targetClass: MutableClass,
            targetMethod: MutableMethod,
            seekMethodName: String,
            methodName: String,
            fieldName: String
        ) {
            targetMethod.apply {
                targetClass.methods.add(
                    ImmutableMethod(
                        definingClass,
                        "seekTo",
                        listOf(ImmutableMethodParameter("J", annotations, "time")),
                        "Z",
                        AccessFlags.PUBLIC or AccessFlags.FINAL,
                        annotations,
                        null,
                        ImmutableMethodImplementation(
                            4, """
                                # first enum (field a) is SEEK_SOURCE_UNKNOWN
                                sget-object v0, $seekSourceEnumType->a:$seekSourceEnumType
                                invoke-virtual {p0, p1, p2, v0}, $definingClass->$seekMethodName(J$seekSourceEnumType)Z
                                move-result p1
                                return p1
                                """.toInstructions(),
                            null,
                            null
                        )
                    ).toMutable()
                )

                val smaliInstructions =
                    """
                        if-eqz v0, :ignore
                        invoke-virtual {v0, p0, p1}, $definingClass->seekTo(J)Z
                        move-result v0
                        return v0
                        :ignore
                        const/4 v0, 0x0
                        return v0
                        """

                addStaticFieldToExtension(
                    EXTENSION_CLASS_DESCRIPTOR,
                    methodName,
                    fieldName,
                    definingClass,
                    smaliInstructions
                )
            }
        }

        fun Pair<String, Fingerprint>.getPlayerResponseInstruction(returnType: String): String {
            mutableMethodOrThrow().apply {
                val targetReference = getInstruction<ReferenceInstruction>(
                    indexOfFirstInstructionOrThrow {
                        val reference = getReference<MethodReference>()
                        (opcode == Opcode.INVOKE_INTERFACE_RANGE || opcode == Opcode.INVOKE_INTERFACE) &&
                                reference?.definingClass == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR &&
                                reference.returnType == returnType
                    }
                ).reference

                return "invoke-interface {v$REGISTER_PLAYER_RESPONSE_MODEL}, $targetReference"
            }
        }

        videoEndFingerprint.mutableMethodOrThrow().apply {
            val mutableClass = mutableClassDefBy(definingClass)
            playerConstructorMethod = mutableClass.methods.first {
                MethodUtil.methodSignaturesMatch(it, this)
            }
            playerConstructorInsertIndex = playerConstructorMethod.indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_DIRECT && getReference<MethodReference>()?.name == "<init>"
            } + 1

            onCreateHook(EXTENSION_CLASS_DESCRIPTOR, "initialize")

            seekSourceEnumType = parameterTypes[1].toString()
            seekSourceMethodName = name

            addSeekInterfaceMethods(
                mutableClassDefBy(definingClass),
                this,
                seekSourceMethodName,
                "overrideVideoTime",
                "videoInformationClass"
            )
        }

        mdxPlayerDirectorSetVideoStageFingerprint.mutableMethodOrThrow().apply {
            val mutableClass = mutableClassDefBy(definingClass)
            mdxConstructorMethod = mutableClass.methods.first {
                MethodUtil.methodSignaturesMatch(it, this)
            }
            mdxConstructorInsertIndex = mdxConstructorMethod.indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_DIRECT && getReference<MethodReference>()?.name == "<init>"
            } + 1

            onCreateHookMdx(EXTENSION_CLASS_DESCRIPTOR, "initializeMdx")

            addSeekInterfaceMethods(
                mutableClassDefBy(definingClass),
                this,
                seekSourceMethodName,
                "overrideMDXVideoTime",
                "videoInformationMDXClass"
            )
        }

        /**
         * Set current video information
         */
        videoIdFingerprint.matchOrThrow().let { match ->
            val method = match.method
            val classDef = match.classDef
            val mutableMethod = mutableClassDefBy(classDef.type).methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.apply {
                val playerResponseModelIndex = indexOfFirstInstructionOrThrow {
                    val reference = getReference<MethodReference>()
                    (opcode == Opcode.INVOKE_INTERFACE_RANGE || opcode == Opcode.INVOKE_INTERFACE) &&
                            reference?.returnType == "Ljava/lang/String;" &&
                            reference.parameterTypes.isEmpty()
                }

                PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR =
                    getInstruction(playerResponseModelIndex)
                        .getReference<MethodReference>()
                        ?.definingClass
                        ?: throw PatchException("Could not find Player Response Model class")

                videoIdMethodCall =
                    videoIdFingerprint.getPlayerResponseInstruction("Ljava/lang/String;")
                videoLengthMethodCall =
                    videoLengthFingerprint.getPlayerResponseInstruction("J")

                videoInformationMethod = getVideoInformationMethod()
                classDef.methods.add(videoInformationMethod)

                addInstruction(
                    playerResponseModelIndex + 2,
                    "invoke-direct/range {p0 .. p1}, $definingClass->setVideoInformation($PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR)V"
                )
            }
        }

        /**
         * Set the video time method
         */
        playerControllerSetTimeReferenceFingerprint.matchOrThrow().let { match ->
            // Obtener la instrucción en la posición startIndex del patrón
            val method = match.method
            val startIndex = match.patternMatch!!.startIndex
            val referenceInstruction = method.getInstruction<ReferenceInstruction>(startIndex)
            val methodRef = referenceInstruction.reference as? MethodReference
                ?: throw PatchException("No method reference at index $startIndex")

            // Obtener la clase mutable del método referenciado
            val targetClass = methodRef.definingClass
            videoTimeConstructorMethod = mutableClassDefBy(targetClass).methods.first {
                it.name == methodRef.name && it.parameterTypes == methodRef.parameterTypes
            }
        }

        /**
         * Set current video time
         */
        videoTimeHook(EXTENSION_CLASS_DESCRIPTOR, "setVideoTime")

        /**
         * Set current video length
         */
        videoLengthHook("$EXTENSION_CLASS_DESCRIPTOR->setVideoLength(J)V")

        /**
         * Set current video id
         */
        videoIdHook("$EXTENSION_CLASS_DESCRIPTOR->setVideoId(Ljava/lang/String;)V")
        addPlayerResponseMethodHook(
            Hook.VideoId(
                "$EXTENSION_CLASS_DESCRIPTOR->setPlayerResponseVideoId(Ljava/lang/String;)V"
            ),
        )
        addPlayerResponseMethodHook(
            Hook.PlayerParameterBeforeVideoId(
                "$EXTENSION_CLASS_DESCRIPTOR->newPlayerResponseParameter(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
            )
        )
        /**
         * Hook current playback speed
         */
        playbackSpeedFingerprint.matchOrThrow(playbackSpeedParentFingerprint).let { match ->
            val method = match.method
            val endIndex = match.patternMatch!!.endIndex
            val referenceInstruction = method.getInstruction<ReferenceInstruction>(endIndex)
            val methodRef = referenceInstruction.reference as? MethodReference
                ?: throw PatchException("No method reference at index $endIndex")

            val targetClass = methodRef.definingClass
            val targetMethod = mutableClassDefBy(targetClass).methods.first {
                it.name == methodRef.name && it.parameterTypes == methodRef.parameterTypes
            }

            targetMethod.apply {
                addInstruction(
                    implementation!!.instructions.lastIndex,
                    "invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->setPlaybackSpeed(F)V"
                )
            }
        }
    }
}

// Las siguientes funciones auxiliares permanecen sin cambios
private fun MutableMethod.getVideoInformationMethod(): MutableMethod =
    ImmutableMethod(
        definingClass,
        "setVideoInformation",
        listOf(
            ImmutableMethodParameter(
                PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR,
                annotations,
                null
            )
        ),
        "V",
        AccessFlags.PRIVATE or AccessFlags.FINAL,
        annotations,
        null,
        ImmutableMethodImplementation(
            REGISTER_PLAYER_RESPONSE_MODEL + 1, """
                $videoIdMethodCall
                move-result-object v$REGISTER_VIDEO_ID
                $videoLengthMethodCall
                move-result-wide v$REGISTER_VIDEO_LENGTH
                return-void
                """.toInstructions(),
            null,
            null
        )
    ).toMutable()

private fun MutableMethod.insert(insertIndex: Int, register: String, descriptor: String) =
    addInstruction(insertIndex, "invoke-static { $register }, $descriptor")

private fun MutableMethod.insertTimeHook(insertIndex: Int, descriptor: String) =
    insert(insertIndex, "p1, p2", descriptor)

internal fun onCreateHook(targetMethodClass: String, targetMethodName: String) =
    playerConstructorMethod.addInstruction(
        playerConstructorInsertIndex++,
        "invoke-static { }, $targetMethodClass->$targetMethodName()V"
    )

internal fun onCreateHookMdx(targetMethodClass: String, targetMethodName: String) =
    mdxConstructorMethod.addInstruction(
        mdxConstructorInsertIndex++,
        "invoke-static { }, $targetMethodClass->$targetMethodName()V"
    )

internal fun videoIdHook(
    descriptor: String
) = videoInformationMethod.apply {
    addInstruction(
        implementation!!.instructions.lastIndex,
        "invoke-static {v$REGISTER_VIDEO_ID}, $descriptor"
    )
}

internal fun videoLengthHook(
    descriptor: String
) = videoInformationMethod.apply {
    addInstruction(
        implementation!!.instructions.lastIndex,
        "invoke-static {v$REGISTER_VIDEO_LENGTH, v$REGISTER_VIDEO_LENGTH_DUMMY}, $descriptor"
    )
}

internal fun videoTimeHook(targetMethodClass: String, targetMethodName: String) =
    videoTimeConstructorMethod.insertTimeHook(
        videoTimeConstructorInsertIndex++,
        "$targetMethodClass->$targetMethodName(J)V"
    )