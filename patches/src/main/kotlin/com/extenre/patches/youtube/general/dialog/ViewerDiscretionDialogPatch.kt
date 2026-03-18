/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.general.dialog

import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.shared.dialog.baseViewerDiscretionDialogPatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.patch.PatchList.REMOVE_VIEWER_DISCRETION_DIALOG
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val viewerDiscretionDialogPatch = bytecodePatch(
    name = REMOVE_VIEWER_DISCRETION_DIALOG.key,
    description = "${REMOVE_VIEWER_DISCRETION_DIALOG.title}: ${REMOVE_VIEWER_DISCRETION_DIALOG.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        baseViewerDiscretionDialogPatch(
            GENERAL_CLASS_DESCRIPTOR,
            true
        ),
        settingsPatch,
    )

    execute {

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: REMOVE_VIEWER_DISCRETION_DIALOG"
            ),
            REMOVE_VIEWER_DISCRETION_DIALOG
        )

        // endregion

    }
}
