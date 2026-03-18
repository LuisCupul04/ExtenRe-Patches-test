/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.misc.watchhistory

import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.shared.trackingurlhook.hookWatchHistory
import com.extenre.patches.shared.trackingurlhook.trackingUrlHookPatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.patch.PatchList.WATCH_HISTORY
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val watchHistoryPatch = bytecodePatch(
    name = WATCH_HISTORY.key,
    description = "${WATCH_HISTORY.title}: ${WATCH_HISTORY.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        trackingUrlHookPatch,
    )

    execute {

        hookWatchHistory()

        // region add settings

        addPreference(
            arrayOf(
                "SETTINGS: WATCH_HISTORY"
            ),
            WATCH_HISTORY
        )

        // endregion

    }
}
