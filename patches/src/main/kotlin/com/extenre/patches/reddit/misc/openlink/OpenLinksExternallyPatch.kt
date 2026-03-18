/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.reddit.misc.openlink

import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.smali.ExternalLabel
import com.extenre.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.reddit.utils.extension.Constants.PATCHES_PATH
import com.extenre.patches.reddit.utils.patch.PatchList.OPEN_LINKS_EXTERNALLY
import com.extenre.patches.reddit.utils.settings.settingsPatch
import com.extenre.patches.reddit.utils.settings.updatePatchStatus
import com.extenre.util.indexOfFirstStringInstructionOrThrow

private const val EXTENSION_METHOD_DESCRIPTOR =
    "$PATCHES_PATH/OpenLinksExternallyPatch;" +
            "->" +
            "openLinksExternally(Landroid/app/Activity;Landroid/net/Uri;)Z"

@Suppress("unused")
val openLinksExternallyPatch = bytecodePatch(
    name = OPEN_LINKS_EXTERNALLY.key,
    description = "${OPEN_LINKS_EXTERNALLY.title}: ${OPEN_LINKS_EXTERNALLY.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        screenNavigatorMethodResolverPatch
    )

    execute {
        screenNavigatorMethod.apply {
            val insertIndex = indexOfFirstStringInstructionOrThrow("uri") + 2

            addInstructionsWithLabels(
                insertIndex, """
                    invoke-static {p1, p2}, $EXTENSION_METHOD_DESCRIPTOR
                    move-result v0
                    if-eqz v0, :dismiss
                    return-void
                    """, ExternalLabel("dismiss", getInstruction(insertIndex))
            )
        }

        updatePatchStatus(
            "enableOpenLinksExternally",
            OPEN_LINKS_EXTERNALLY
        )
    }
}
