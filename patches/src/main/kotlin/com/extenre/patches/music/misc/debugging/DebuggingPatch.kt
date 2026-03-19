/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.misc.debugging

import com.extenre.patcher.patch.resourcePatch
import com.extenre.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.music.utils.patch.PatchList.ENABLE_DEBUG_LOGGING
import com.extenre.patches.music.utils.settings.CategoryType
import com.extenre.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import com.extenre.patches.music.utils.settings.addSwitchPreference
import com.extenre.patches.music.utils.settings.settingsPatch

@Suppress("unused")
val debuggingPatch = resourcePatch(
    name = ENABLE_DEBUG_LOGGING.key,
    description = "${ENABLE_DEBUG_LOGGING.title}: ${ENABLE_DEBUG_LOGGING.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        addSwitchPreference(
            CategoryType.MISC,
            "extenre_debug",
            "false"
        )
        addSwitchPreference(
            CategoryType.MISC,
            "extenre_debug_protobuffer",
            "false",
            "extenre_debug"
        )
        addSwitchPreference(
            CategoryType.MISC,
            "extenre_debug_spannable",
            "false",
            "extenre_debug"
        )

        updatePatchStatus(ENABLE_DEBUG_LOGGING)

    }
}
