/*
 * Copyright (C) 2026 LuisCupul04
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2022 ReVanced LLC
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.layout.branding.name

import com.extenre.patcher.patch.resourcePatch
import com.extenre.patcher.patch.stringOption
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.patch.PatchList.CUSTOM_BRANDING_NAME_FOR_YOUTUBE
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.removeStringsElements
import com.extenre.util.valueOrThrow

private const val APP_NAME = "YT ExtenRe"

@Suppress("unused")
val customBrandingNamePatch = resourcePatch(
    name = CUSTOM_BRANDING_NAME_FOR_YOUTUBE.key,
    description = "${CUSTOM_BRANDING_NAME_FOR_YOUTUBE.title}: ${CUSTOM_BRANDING_NAME_FOR_YOUTUBE.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    val appNameOption = stringOption(
        key = "appName",
        default = APP_NAME,
        values = mapOf(
            "YT ExtenRe" to APP_NAME,
            "RVX" to "RVX",
            "ReVanced Extended" to "ReVanced Extended",
            "YouTube RVX" to "YouTube RVX",
            "YouTube" to "YouTube",
            "YT" to "YT",
            "YT RE" to "YT RE",
            "YT EXTEN" to "YT EXTEN",
            "YT ExtenRe" to "YT ExtenRe",
            "YT EXRE" to "YT EXRE",
            "YT NEKO" to "YT NEKO",
            "YT KITSUNE" to "YT KITSUNE",
        ),
        title = "App name",
        description = "The name of the app.",
        required = true,
    )

    execute {
        // Check patch options first.
        val appName = appNameOption
            .valueOrThrow()

        removeStringsElements(
            arrayOf("application_name")
        )

        document("res/values/strings.xml").use { document ->
            val stringElement = document.createElement("string")

            stringElement.setAttribute("name", "application_name")
            stringElement.textContent = appName

            document.getElementsByTagName("resources").item(0)
                .appendChild(stringElement)
        }

    }
}
