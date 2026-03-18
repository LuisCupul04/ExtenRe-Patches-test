/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.reddit.layout.premiumicon

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.reddit.utils.fix.signature.spoofSignaturePatch
import com.extenre.patches.reddit.utils.patch.PatchList.PREMIUM_ICON
import com.extenre.patches.reddit.utils.settings.updatePatchStatus
import com.extenre.util.fingerprint.mutableMethodOrThrow

@Suppress("unused")
val premiumIconPatch = bytecodePatch(
    name = PREMIUM_ICON.key,
    description = "${PREMIUM_ICON.title}: ${PREMIUM_ICON.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(spoofSignaturePatch)

    execute {
        premiumIconFingerprint.mutableMethodOrThrow().addInstructions(
            0, """
                const/4 v0, 0x1
                return v0
                """
        )

        updatePatchStatus(PREMIUM_ICON)
    }
}
