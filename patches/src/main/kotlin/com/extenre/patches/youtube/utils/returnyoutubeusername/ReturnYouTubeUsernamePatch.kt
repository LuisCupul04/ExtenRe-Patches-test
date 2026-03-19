/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.utils.returnyoutubeusername

import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.shared.returnyoutubeusername.baseReturnYouTubeUsernamePatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.patch.PatchList.RETURN_YOUTUBE_USERNAME
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val returnYouTubeUsernamePatch = bytecodePatch(
    name = RETURN_YOUTUBE_USERNAME.key,
    description = "${RETURN_YOUTUBE_USERNAME.title}: ${RETURN_YOUTUBE_USERNAME.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        baseReturnYouTubeUsernamePatch,
        settingsPatch,
    )

    execute {

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: RETURN_YOUTUBE_USERNAME"
            ),
            RETURN_YOUTUBE_USERNAME
        )

        // endregion
    }
}
