/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.general.loadingscreen

import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.patch.PatchList.ENABLE_GRADIENT_LOADING_SCREEN
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.fingerprint.injectLiteralInstructionBooleanCall

@Suppress("unused")
val gradientLoadingScreenPatch = bytecodePatch(
    name = ENABLE_GRADIENT_LOADING_SCREEN.key,
    description = "${ENABLE_GRADIENT_LOADING_SCREEN.title}: ${ENABLE_GRADIENT_LOADING_SCREEN.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        useGradientLoadingScreenFingerprint.injectLiteralInstructionBooleanCall(
            GRADIENT_LOADING_SCREEN_AB_CONSTANT,
            "$GENERAL_CLASS_DESCRIPTOR->enableGradientLoadingScreen()Z"
        )

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: ENABLE_GRADIENT_LOADING_SCREEN"
            ),
            ENABLE_GRADIENT_LOADING_SCREEN
        )

        // endregion

    }
}
