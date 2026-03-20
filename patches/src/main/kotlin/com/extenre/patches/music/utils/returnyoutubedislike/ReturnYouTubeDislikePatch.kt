/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.utils.returnyoutubedislike

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.patch.resourcePatch
import com.extenre.patches.music.utils.ACTION_BAR_POSITION_FEATURE_FLAG
import com.extenre.patches.music.utils.actionBarPositionFeatureFlagFingerprint
import com.extenre.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.music.utils.extension.Constants.UTILS_PATH
import com.extenre.patches.music.utils.patch.PatchList.RETURN_YOUTUBE_DISLIKE
import com.extenre.patches.music.utils.playservice.is_7_17_or_greater
import com.extenre.patches.music.utils.playservice.is_7_25_or_greater
import com.extenre.patches.music.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.music.utils.settings.CategoryType
import com.extenre.patches.music.utils.settings.ResourceUtils.PREFERENCE_CATEGORY_TAG_NAME
import com.extenre.patches.music.utils.settings.ResourceUtils.SETTINGS_HEADER_PATH
import com.extenre.patches.music.utils.settings.ResourceUtils.addPreferenceCategoryUnderPreferenceScreen
import com.extenre.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import com.extenre.patches.music.utils.settings.addSwitchPreference
import com.extenre.patches.music.utils.settings.settingsPatch
import com.extenre.patches.music.video.information.videoIdHook
import com.extenre.patches.music.video.information.videoInformationPatch
import com.extenre.patches.shared.dislikeFingerprint
import com.extenre.patches.shared.likeFingerprint
import com.extenre.patches.shared.removeLikeFingerprint
import com.extenre.patches.shared.textcomponent.hookSpannableString
import com.extenre.patches.shared.textcomponent.textComponentPatch
import com.extenre.util.adoptChild
import com.extenre.util.fingerprint.injectLiteralInstructionBooleanCall
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import org.w3c.dom.Element

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/ReturnYouTubeDislikePatch;"

private val returnYouTubeDislikeBytecodePatch = bytecodePatch(
    name = "return-youtube-dislike-bytecode-patch",
    description = "returnYouTubeDislikeBytecodePatch"
) {
    dependsOn(
        settingsPatch,
        sharedResourceIdPatch,
        videoInformationPatch,
        textComponentPatch,
    )

    execute {

        mapOf(
            likeFingerprint to Vote.LIKE,
            dislikeFingerprint to Vote.DISLIKE,
            removeLikeFingerprint to Vote.REMOVE_LIKE,
        ).forEach { (fingerprint, vote) ->
            fingerprint.mutableMethodOrThrow().addInstructions(
                0,
                """
                    const/4 v0, ${vote.value}
                    invoke-static {v0}, $EXTENSION_CLASS_DESCRIPTOR->sendVote(I)V
                    """,
            )
        }

        if (!is_7_25_or_greater) {
            textComponentFingerprint.mutableMethodOrThrow().apply {
                val insertIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_STATIC
                            && (this as ReferenceInstruction).reference.toString()
                        .endsWith("Ljava/lang/CharSequence;")
                } + 2
                val insertRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $EXTENSION_CLASS_DESCRIPTOR->onSpannedCreated(Landroid/text/Spanned;)Landroid/text/Spanned;
                        move-result-object v$insertRegister
                        """
                )
            }
        } else {
            actionBarPositionFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                ACTION_BAR_POSITION_FEATURE_FLAG,
                "$EXTENSION_CLASS_DESCRIPTOR->actionBarFeatureFlagLoaded(Z)Z"
            )
        }

        if (is_7_17_or_greater) {
            hookSpannableString(EXTENSION_CLASS_DESCRIPTOR, "onLithoTextLoaded")
        }

        videoIdHook("$EXTENSION_CLASS_DESCRIPTOR->newVideoLoaded(Ljava/lang/String;)V")

    }
}

enum class Vote(val value: Int) {
    LIKE(1),
    DISLIKE(-1),
    REMOVE_LIKE(0),
}

private const val ABOUT_CATEGORY_KEY = "extenre_ryd_about"
private const val RYD_ATTRIBUTION_KEY = "extenre_ryd_attribution"

@Suppress("unused")
val returnYouTubeDislikePatch = resourcePatch(
    name = RETURN_YOUTUBE_DISLIKE.key,
    description = "${RETURN_YOUTUBE_DISLIKE.title}: ${RETURN_YOUTUBE_DISLIKE.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        returnYouTubeDislikeBytecodePatch,
        settingsPatch,
    )

    execute {
        addSwitchPreference(
            CategoryType.RETURN_YOUTUBE_DISLIKE,
            "extenre_ryd_enabled",
            "true"
        )
        addSwitchPreference(
            CategoryType.RETURN_YOUTUBE_DISLIKE,
            "extenre_ryd_dislike_percentage",
            "false",
            "extenre_ryd_enabled"
        )
        addSwitchPreference(
            CategoryType.RETURN_YOUTUBE_DISLIKE,
            "extenre_ryd_compact_layout",
            "false",
            "extenre_ryd_enabled"
        )
        addSwitchPreference(
            CategoryType.RETURN_YOUTUBE_DISLIKE,
            "extenre_ryd_estimated_like",
            "false",
            "extenre_ryd_enabled"
        )
        addSwitchPreference(
            CategoryType.RETURN_YOUTUBE_DISLIKE,
            "extenre_ryd_toast_on_connection_error",
            "true",
            "extenre_ryd_enabled"
        )

        addPreferenceCategoryUnderPreferenceScreen(
            CategoryType.RETURN_YOUTUBE_DISLIKE.value,
            ABOUT_CATEGORY_KEY
        )

        document(SETTINGS_HEADER_PATH).use { document ->
            val tags = document.getElementsByTagName(PREFERENCE_CATEGORY_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains(ABOUT_CATEGORY_KEY) }
                .forEach {
                    it.adoptChild("Preference") {
                        setAttribute("android:title", "@string/$RYD_ATTRIBUTION_KEY" + "_title")
                        setAttribute("android:summary", "@string/$RYD_ATTRIBUTION_KEY" + "_summary")
                        setAttribute("android:key", RYD_ATTRIBUTION_KEY)
                        this.adoptChild("intent") {
                            setAttribute("android:action", "android.intent.action.VIEW")
                            setAttribute("android:data", "https://returnyoutubedislike.com")
                        }
                    }
                }
        }

        updatePatchStatus(RETURN_YOUTUBE_DISLIKE)

    }
}
