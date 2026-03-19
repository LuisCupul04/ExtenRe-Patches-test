/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.misc.album

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.music.utils.extension.Constants.MISC_PATH
import com.extenre.patches.music.utils.patch.PatchList.DISABLE_MUSIC_VIDEO_IN_ALBUM
import com.extenre.patches.music.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.music.utils.settings.CategoryType
import com.extenre.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import com.extenre.patches.music.utils.settings.addPreferenceWithIntent
import com.extenre.patches.music.utils.settings.addSwitchPreference
import com.extenre.patches.music.utils.settings.settingsPatch
import com.extenre.patches.music.video.information.videoIdHook
import com.extenre.patches.music.video.information.videoInformationPatch
import com.extenre.patches.music.video.playerresponse.Hook
import com.extenre.patches.music.video.playerresponse.addPlayerResponseMethodHook
import com.extenre.patches.music.video.playerresponse.playerResponseMethodHookPatch
import com.extenre.util.findMethodOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$MISC_PATH/AlbumMusicVideoPatch;"

@Suppress("unused")
val albumMusicVideoPatch = bytecodePatch(
    name = DISABLE_MUSIC_VIDEO_IN_ALBUM.key,
    description = "${DISABLE_MUSIC_VIDEO_IN_ALBUM.title}: ${DISABLE_MUSIC_VIDEO_IN_ALBUM.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        sharedResourceIdPatch,
        videoInformationPatch,
        playerResponseMethodHookPatch,
    )

    execute {

        // region hook player response

        addPlayerResponseMethodHook(
            Hook.VideoIdAndPlaylistId(
                "$EXTENSION_CLASS_DESCRIPTOR->newPlayerResponse(Ljava/lang/String;Ljava/lang/String;I)V"
            ),
        )

        // endregion

        // region hook video id

        videoIdHook("$EXTENSION_CLASS_DESCRIPTOR->newVideoLoaded(Ljava/lang/String;)V")

        // endregion

        // region patch for hide snack bar

        snackBarFingerprint
            .mutableMethodOrThrow(snackBarAttributeFingerprint)
            .addInstructionsWithLabels(
                0, """
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->hideSnackBar()Z
                move-result v0
                if-eqz v0, :hide
                return-void
                :hide
                nop
                """
            )

        // endregion

        // region patch for setOnClick / setOnLongClick listener

        audioVideoSwitchToggleConstructorFingerprint.mutableMethodOrThrow().apply {
            val onClickListenerIndex = indexOfAudioVideoSwitchSetOnClickListenerInstruction(this)
            val viewRegister =
                getInstruction<FiveRegisterInstruction>(onClickListenerIndex).registerC

            addInstruction(
                onClickListenerIndex + 1,
                "invoke-static { v$viewRegister }, " +
                        "$EXTENSION_CLASS_DESCRIPTOR->setAudioVideoSwitchToggleOnLongClickListener(Landroid/view/View;)V"
            )

            val onClickListenerSyntheticIndex =
                indexOfFirstInstructionReversedOrThrow(onClickListenerIndex) {
                    opcode == Opcode.INVOKE_DIRECT &&
                            getReference<MethodReference>()?.name == "<init>"
                }
            val onClickListenerSyntheticClass =
                (getInstruction<ReferenceInstruction>(onClickListenerSyntheticIndex).reference as MethodReference).definingClass

            // Buscar el método onClick y convertirlo a mutable
            val method = findMethodOrThrow(onClickListenerSyntheticClass) {
                name == "onClick"
            }
            val classDef = classes.find { it.type == onClickListenerSyntheticClass }
                ?: throw PatchException("Class not found: $onClickListenerSyntheticClass")
            val mutableMethod = proxy(classDef).mutableClass.methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }

            mutableMethod.addInstructionsWithLabels(
                0, """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->openMusic()Z
                    move-result v0
                    if-eqz v0, :ignore
                    return-void
                    :ignore
                    nop
                    """
            )
        }

        // endregion

        addSwitchPreference(
            CategoryType.MISC,
            "extenre_disable_music_video_in_album",
            "false"
        )
        addPreferenceWithIntent(
            CategoryType.MISC,
            "extenre_disable_music_video_in_album_redirect_type",
            "extenre_disable_music_video_in_album"
        )

        updatePatchStatus(DISABLE_MUSIC_VIDEO_IN_ALBUM)

    }
}
