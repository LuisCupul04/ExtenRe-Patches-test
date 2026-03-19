/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.layout.playerbuttonbg

import com.extenre.patcher.patch.resourcePatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.patch.PatchList.FORCE_HIDE_PLAYER_BUTTONS_BACKGROUND
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.doRecursively
import org.w3c.dom.Element

@Suppress("unused")
val playerButtonBackgroundPatch = resourcePatch(
    name = FORCE_HIDE_PLAYER_BUTTONS_BACKGROUND.key,
    description = "${FORCE_HIDE_PLAYER_BUTTONS_BACKGROUND.title}: ${FORCE_HIDE_PLAYER_BUTTONS_BACKGROUND.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        document("res/drawable/player_button_circle_background.xml").use { document ->

            document.doRecursively node@{ node ->
                if (node !is Element) return@node

                node.getAttributeNode("android:color")?.let { attribute ->
                    attribute.textContent = "@android:color/transparent"
                }
            }
        }

        addPreference(FORCE_HIDE_PLAYER_BUTTONS_BACKGROUND)

    }
}
