/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.general.oldstylelibraryshelf

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.music.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import com.extenre.patches.music.utils.patch.PatchList.RESTORE_OLD_STYLE_LIBRARY_SHELF
import com.extenre.patches.music.utils.settings.CategoryType
import com.extenre.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import com.extenre.patches.music.utils.settings.addSwitchPreference
import com.extenre.patches.music.utils.settings.settingsPatch
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.extenre.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
val oldStyleLibraryShelfPatch = bytecodePatch(
    name = RESTORE_OLD_STYLE_LIBRARY_SHELF.key,
    description = "${RESTORE_OLD_STYLE_LIBRARY_SHELF.title}: ${RESTORE_OLD_STYLE_LIBRARY_SHELF.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        browseIdFingerprint.mutableMethodOrThrow().apply {
            val stringIndex = indexOfFirstStringInstructionOrThrow("FEmusic_offline")
            val targetIndex =
                indexOfFirstInstructionReversedOrThrow(stringIndex, Opcode.IGET_OBJECT)
            val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

            addInstructions(
                targetIndex + 1, """
                    invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->restoreOldStyleLibraryShelf(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$targetRegister
                    """
            )
        }

        addSwitchPreference(
            CategoryType.GENERAL,
            "extenre_restore_old_style_library_shelf",
            "false"
        )

        updatePatchStatus(RESTORE_OLD_STYLE_LIBRARY_SHELF)

    }
}
