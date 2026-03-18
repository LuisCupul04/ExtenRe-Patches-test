/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.alternative.thumbnails

import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.shared.imageurl.addImageUrlErrorCallbackHook
import com.extenre.patches.shared.imageurl.addImageUrlHook
import com.extenre.patches.shared.imageurl.addImageUrlSuccessCallbackHook
import com.extenre.patches.shared.imageurl.cronetImageUrlHookPatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.ALTERNATIVE_THUMBNAILS_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.navigation.navigationBarHookPatch
import com.extenre.patches.youtube.utils.patch.PatchList.ALTERNATIVE_THUMBNAILS
import com.extenre.patches.youtube.utils.playertype.playerTypeHookPatch
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val alternativeThumbnailsPatch = bytecodePatch(
    name = ALTERNATIVE_THUMBNAILS.key,
    description = "${ALTERNATIVE_THUMBNAILS.title}: ${ALTERNATIVE_THUMBNAILS.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        cronetImageUrlHookPatch(true),
        navigationBarHookPatch,
        playerTypeHookPatch,
        settingsPatch,
    )
    execute {

        addImageUrlHook(ALTERNATIVE_THUMBNAILS_CLASS_DESCRIPTOR)
        addImageUrlSuccessCallbackHook(ALTERNATIVE_THUMBNAILS_CLASS_DESCRIPTOR)
        addImageUrlErrorCallbackHook(ALTERNATIVE_THUMBNAILS_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: ALTERNATIVE_THUMBNAILS",
                "SETTINGS: ALTERNATIVE_THUMBNAILS"
            ),
            ALTERNATIVE_THUMBNAILS
        )

        // endregion

    }
}
