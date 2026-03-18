/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.misc.quic

import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.shared.quic.baseQuicProtocolPatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.patch.PatchList.DISABLE_QUIC_PROTOCOL
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch

@Suppress("unused", "SpellCheckingInspection")
val quicProtocolPatch = bytecodePatch(
    name = DISABLE_QUIC_PROTOCOL.key,
    description = "${DISABLE_QUIC_PROTOCOL.title}: ${DISABLE_QUIC_PROTOCOL.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        baseQuicProtocolPatch(),
    )

    execute {
        addPreference(
            arrayOf(
                "SETTINGS: DISABLE_QUIC_PROTOCOL"
            ),
            DISABLE_QUIC_PROTOCOL
        )
    }
}
