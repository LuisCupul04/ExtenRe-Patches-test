/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.utils.playertype

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.music.utils.extension.Constants.UTILS_PATH
import com.extenre.util.fingerprint.mutableMethodOrThrow

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/PlayerTypeHookPatch;"

@Suppress("unused")
val playerTypeHookPatch = bytecodePatch(
    name = "player-type-hook-patch",
    description = "playerTypeHookPatch"
) {

    execute {

        playerTypeFingerprint.mutableMethodOrThrow().addInstruction(
            0,
            "invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->setPlayerType(Ljava/lang/Enum;)V"
        )

    }
}
