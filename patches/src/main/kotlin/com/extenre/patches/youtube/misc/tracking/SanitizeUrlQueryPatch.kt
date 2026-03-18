/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.misc.tracking

import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.shared.tracking.baseSanitizeUrlQueryPatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.patch.PatchList.SANITIZE_SHARING_LINKS
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val sanitizeUrlQueryPatch = bytecodePatch(
    name = SANITIZE_SHARING_LINKS.key,
    description = "${SANITIZE_SHARING_LINKS.title}: ${SANITIZE_SHARING_LINKS.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        baseSanitizeUrlQueryPatch,
        settingsPatch,
    )

    execute {

        // region add settings

        addPreference(
            arrayOf(
                "SETTINGS: SANITIZE_SHARING_LINKS"
            ),
            SANITIZE_SHARING_LINKS
        )

        // endregion

    }
}
