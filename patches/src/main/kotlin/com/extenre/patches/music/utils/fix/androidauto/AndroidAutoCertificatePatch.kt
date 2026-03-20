/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.utils.fix.androidauto

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.music.utils.patch.PatchList.CERTIFICATE_SPOOF
import com.extenre.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import com.extenre.util.fingerprint.mutableMethodOrThrow

@Suppress("unused")
val androidAutoCertificatePatch = bytecodePatch(
    name = CERTIFICATE_SPOOF.key,
    description = "${CERTIFICATE_SPOOF.title}: ${CERTIFICATE_SPOOF.summary}",
) {
    execute {
        certificateCheckFingerprint.mutableMethodOrThrow().addInstructions(
            0, """
                const/4 v0, 0x1
                return v0
                """
        )

        updatePatchStatus(CERTIFICATE_SPOOF)
    }
}
