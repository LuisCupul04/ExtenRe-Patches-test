/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.utils.fix.streamingdata

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patches.music.utils.playservice.is_7_16_or_greater
import com.extenre.patches.music.utils.playservice.is_7_33_or_greater
import com.extenre.patches.music.utils.playservice.is_8_12_or_greater
import com.extenre.patches.music.utils.playservice.is_8_15_or_greater
import com.extenre.patches.music.utils.playservice.versionCheckPatch
import com.extenre.patches.music.utils.settings.CategoryType
import com.extenre.patches.music.utils.settings.addPreferenceWithIntent
import com.extenre.patches.music.utils.settings.addSwitchPreference
import com.extenre.patches.music.utils.settings.settingsPatch
import com.extenre.patches.music.utils.webview.webViewPatch
import com.extenre.patches.music.video.information.videoInformationPatch
import com.extenre.patches.music.video.playerresponse.Hook
import com.extenre.patches.music.video.playerresponse.addPlayerResponseMethodHook
import com.extenre.patches.shared.buildRequestFingerprint
import com.extenre.patches.shared.buildRequestParentFingerprint
import com.extenre.patches.shared.indexOfNewUrlRequestBuilderInstruction
import com.extenre.patches.shared.spoof.streamingdata.EXTENSION_CLASS_DESCRIPTOR
import com.extenre.patches.shared.spoof.streamingdata.spoofStreamingDataPatch
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

val spoofStreamingDataPatch = spoofStreamingDataPatch(
    block = {
        dependsOn(
            settingsPatch,
            versionCheckPatch,
            videoInformationPatch,
            webViewPatch,
        )
    },
    isYouTube = {
        false
    },
    outlineIcon = {
        false
    },
    fixMediaFetchHotConfigChanges = {
        is_7_16_or_greater
    },
    fixMediaFetchHotConfigAlternativeChanges = {
        // In 8.15 the flag was merged with 7.33 start playback flag.
        is_8_12_or_greater && !is_8_15_or_greater
    },
    fixParsePlaybackResponseFeatureFlag = {
        is_7_33_or_greater
    },
    executeBlock = {

        // region Get replacement streams at player requests.

        buildRequestFingerprint.mutableMethodOrThrow(buildRequestParentFingerprint).apply {
            val newRequestBuilderIndex = indexOfNewUrlRequestBuilderInstruction(this)
            val urlRegister =
                getInstruction<FiveRegisterInstruction>(newRequestBuilderIndex).registerD

            addInstructions(
                newRequestBuilderIndex,
                "invoke-static { v$urlRegister, p1 }, $EXTENSION_CLASS_DESCRIPTOR->fetchStreams(Ljava/lang/String;Ljava/util/Map;)V"
            )
        }

        // endregion

        addPlayerResponseMethodHook(
            Hook.PlayerParameterBeforeVideoId(
                "$EXTENSION_CLASS_DESCRIPTOR->newPlayerResponseParameter(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
            )
        )
        addSwitchPreference(
            CategoryType.MISC,
            "extenre_spoof_streaming_data",
            "true"
        )
        addPreferenceWithIntent(
            CategoryType.MISC,
            "extenre_spoof_streaming_data_default_client",
            "extenre_spoof_streaming_data",
        )
        addPreferenceWithIntent(
            CategoryType.MISC,
            "extenre_spoof_streaming_data_sign_in_android_no_sdk_about",
            "extenre_spoof_streaming_data"
        )
        addPreferenceWithIntent(
            CategoryType.MISC,
            "extenre_spoof_streaming_data_sign_in_android_vr_about",
            "extenre_spoof_streaming_data"
        )
    },
)
