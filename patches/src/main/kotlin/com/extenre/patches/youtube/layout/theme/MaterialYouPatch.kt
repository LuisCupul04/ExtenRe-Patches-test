/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.layout.theme

import com.extenre.patcher.patch.resourcePatch
import com.extenre.patches.shared.materialyou.baseMaterialYou
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.patch.PatchList.MATERIALYOU
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusTheme
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.copyXmlNode

@Suppress("unused")
val materialYouPatch = resourcePatch(
    name = MATERIALYOU.key,
    description = "${MATERIALYOU.title}: ${MATERIALYOU.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        sharedThemePatch,
        settingsPatch,
    )

    execute {
        baseMaterialYou()

        copyXmlNode("youtube/materialyou/host", "values-v31/colors.xml", "resources")

        updatePatchStatusTheme("MaterialYou")

        addPreference(MATERIALYOU)

    }
}
