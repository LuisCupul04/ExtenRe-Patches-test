/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.general.autocaptions

import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.shared.captions.baseAutoCaptionsPatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.patch.PatchList.DISABLE_FORCED_AUTO_CAPTIONS
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val autoCaptionsPatch = bytecodePatch(
    name = DISABLE_FORCED_AUTO_CAPTIONS.key,
    description = "${DISABLE_FORCED_AUTO_CAPTIONS.title}: ${DISABLE_FORCED_AUTO_CAPTIONS.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        baseAutoCaptionsPatch,
        settingsPatch,
    )

    execute {

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: DISABLE_AUTO_CAPTIONS"
            ),
            DISABLE_FORCED_AUTO_CAPTIONS
        )

        // endregion

    }
}
