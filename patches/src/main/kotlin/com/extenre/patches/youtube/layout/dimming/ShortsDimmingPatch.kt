/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.layout.dimming

import com.extenre.patcher.patch.resourcePatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.patch.PatchList.HIDE_SHORTS_DIMMING
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.removeOverlayBackground

@Suppress("unused")
val shortsDimmingPatch = resourcePatch(
    name = HIDE_SHORTS_DIMMING.key,
    description = "${HIDE_SHORTS_DIMMING.title}: ${HIDE_SHORTS_DIMMING.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        removeOverlayBackground(
            arrayOf("reel_player_overlay_scrims.xml"),
            arrayOf("reel_player_overlay_v2_scrims_vertical")
        )
        removeOverlayBackground(
            arrayOf("reel_watch_fragment.xml"),
            arrayOf("reel_scrim_shorts_while_top")
        )

        addPreference(HIDE_SHORTS_DIMMING)

    }
}
