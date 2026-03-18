/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.player.comments

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.shared.comments.commentsPanelPatch
import com.extenre.patches.shared.litho.addLithoFilter
import com.extenre.patches.shared.litho.lithoFilterPatch
import com.extenre.patches.shared.spans.addSpanFilter
import com.extenre.patches.shared.spans.inclusiveSpanPatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.componentlist.hookElementList
import com.extenre.patches.youtube.utils.componentlist.lazilyConvertedElementHookPatch
import com.extenre.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import com.extenre.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.extension.Constants.SPANS_PATH
import com.extenre.patches.youtube.utils.fix.litho.lithoLayoutPatch
import com.extenre.patches.youtube.utils.patch.PatchList.HIDE_COMMENTS_COMPONENTS
import com.extenre.patches.youtube.utils.playertype.playerTypeHookPatch
import com.extenre.patches.youtube.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getWalkerMethod
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val COMMENTS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/CommentsFilter;"
private const val SEARCH_LINKS_FILTER_CLASS_DESCRIPTOR =
    "$SPANS_PATH/SearchLinksFilter;"

@Suppress("unused")
val commentsComponentPatch = bytecodePatch(
    name = HIDE_COMMENTS_COMPONENTS.key,
    description = "${HIDE_COMMENTS_COMPONENTS.title}: ${HIDE_COMMENTS_COMPONENTS.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        inclusiveSpanPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        lazilyConvertedElementHookPatch,
        playerTypeHookPatch,
        sharedResourceIdPatch,
        commentsPanelPatch,
    )

    execute {

        // region patch for emoji picker button in shorts

        shortsLiveStreamEmojiPickerOpacityFingerprint.mutableMethodOrThrow().apply {
            val insertIndex = implementation!!.instructions.lastIndex
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstruction(
                insertIndex,
                "invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->changeEmojiPickerOpacity(Landroid/widget/ImageView;)V"
            )
        }

        shortsLiveStreamEmojiPickerOnClickListenerFingerprint.mutableMethodOrThrow().apply {
            val emojiPickerEndpointIndex =
                indexOfFirstLiteralInstructionOrThrow(126326492L)
            val emojiPickerOnClickListenerIndex =
                indexOfFirstInstructionOrThrow(emojiPickerEndpointIndex, Opcode.INVOKE_DIRECT)
            val emojiPickerOnClickListenerMethod =
                getWalkerMethod(emojiPickerOnClickListenerIndex)

            emojiPickerOnClickListenerMethod.apply {
                val insertIndex = indexOfFirstInstructionOrThrow(Opcode.IF_EQZ)
                val insertRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->disableEmojiPickerOnClickListener(Ljava/lang/Object;)Ljava/lang/Object;
                        move-result-object v$insertRegister
                        """
                )
            }
        }

        // endregion

        addSpanFilter(SEARCH_LINKS_FILTER_CLASS_DESCRIPTOR)
        addLithoFilter(COMMENTS_FILTER_CLASS_DESCRIPTOR)
        hookElementList("$PLAYER_CLASS_DESCRIPTOR->sanitizeCommentsCategoryBar")

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: HIDE_COMMENTS_COMPONENTS"
            ),
            HIDE_COMMENTS_COMPONENTS
        )

        // endregion

    }
}
