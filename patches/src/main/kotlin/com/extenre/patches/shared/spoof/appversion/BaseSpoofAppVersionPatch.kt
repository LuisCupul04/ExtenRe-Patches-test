/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.shared.spoof.appversion

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.shared.createPlayerRequestBodyWithModelFingerprint
import com.extenre.patches.shared.indexOfReleaseInstruction
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

fun baseSpoofAppVersionPatch(
    descriptor: String,
) = bytecodePatch(
    name = "base-Spoof-App-Version-Patch",
    description = "baseSpoofAppVersionPatch"
) {
    execute {
        createPlayerRequestBodyWithModelFingerprint.mutableMethodOrThrow().apply {
            val versionIndex = indexOfReleaseInstruction(this) + 1
            val insertIndex =
                indexOfFirstInstructionReversedOrThrow(versionIndex, Opcode.IPUT_OBJECT)
            val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

            addInstructions(
                insertIndex, """
                    invoke-static {v$insertRegister}, $descriptor
                    move-result-object v$insertRegister
                    """
            )
        }
    }
}
