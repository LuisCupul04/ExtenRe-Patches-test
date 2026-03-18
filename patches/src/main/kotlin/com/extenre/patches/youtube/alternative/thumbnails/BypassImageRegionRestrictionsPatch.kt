/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.alternative.thumbnails

import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.shared.imageurl.addImageUrlHook
import com.extenre.patches.shared.imageurl.cronetImageUrlHookPatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.patch.PatchList.BYPASS_IMAGE_REGION_RESTRICTIONS
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val bypassImageRegionRestrictionsPatch = bytecodePatch(
    name = BYPASS_IMAGE_REGION_RESTRICTIONS.key,
    description = "${BYPASS_IMAGE_REGION_RESTRICTIONS.title}: ${BYPASS_IMAGE_REGION_RESTRICTIONS.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        cronetImageUrlHookPatch(true),
        settingsPatch,
    )
    execute {

        addImageUrlHook()

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: ALTERNATIVE_THUMBNAILS",
                "SETTINGS: BYPASS_IMAGE_REGION_RESTRICTIONS"
            ),
            BYPASS_IMAGE_REGION_RESTRICTIONS
        )

        // endregion

    }
}
