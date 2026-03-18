/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.reddit.misc.openlink

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.reddit.utils.extension.Constants.PATCHES_PATH
import com.extenre.patches.reddit.utils.patch.PatchList.OPEN_LINKS_DIRECTLY
import com.extenre.patches.reddit.utils.settings.settingsPatch
import com.extenre.patches.reddit.utils.settings.updatePatchStatus

private const val EXTENSION_METHOD_DESCRIPTOR =
    "$PATCHES_PATH/OpenLinksDirectlyPatch;" +
            "->" +
            "parseRedirectUri(Landroid/net/Uri;)Landroid/net/Uri;"

@Suppress("unused")
val openLinksDirectlyPatch = bytecodePatch(
    name = OPEN_LINKS_DIRECTLY.key,
    description = "${OPEN_LINKS_DIRECTLY.title}: ${OPEN_LINKS_DIRECTLY.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        screenNavigatorMethodResolverPatch
    )

    execute {
        screenNavigatorMethod.addInstructions(
            0, """
                invoke-static {p2}, $EXTENSION_METHOD_DESCRIPTOR
                move-result-object p2
                """
        )

        updatePatchStatus(
            "enableOpenLinksDirectly",
            OPEN_LINKS_DIRECTLY
        )
    }
}
