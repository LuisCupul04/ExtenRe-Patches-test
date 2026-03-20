/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.general.startpage

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.patch.resourcePatch
import com.extenre.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.music.utils.extension.Constants.GENERAL_PATH
import com.extenre.patches.music.utils.patch.PatchList.CHANGE_START_PAGE
import com.extenre.patches.music.utils.playservice.is_6_27_or_greater
import com.extenre.patches.music.utils.playservice.versionCheckPatch
import com.extenre.patches.music.utils.settings.CategoryType
import com.extenre.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import com.extenre.patches.music.utils.settings.addPreferenceWithIntent
import com.extenre.patches.music.utils.settings.settingsPatch
import com.extenre.util.addEntryValues
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.extenre.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/ChangeStartPagePatch;"

private val changeStartPageResourcePatch = resourcePatch(
    name = "change-start-page-resource-patch",
    description = "changeStartPageResourcePatch"
) {
    dependsOn(
        settingsPatch,
        versionCheckPatch,
    )

    execute {
        fun appendStartPage(startPage: String) {
            addEntryValues(
                "extenre_change_start_page_entries",
                "@string/extenre_change_start_page_entry_$startPage",
            )
            addEntryValues(
                "extenre_change_start_page_entry_values",
                startPage.uppercase(),
            )
        }

        if (is_6_27_or_greater) {
            appendStartPage("search")
        }
        appendStartPage("subscriptions")
    }
}

@Suppress("unused")
val changeStartPagePatch = bytecodePatch(
    name = CHANGE_START_PAGE.key,
    description = "${CHANGE_START_PAGE.title}: ${CHANGE_START_PAGE.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        changeStartPageResourcePatch,
        settingsPatch,
    )

    execute {

        coldStartIntentFingerprint.mutableMethodOrThrow().addInstruction(
            0,
            "invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->overrideIntent(Landroid/content/Intent;)V"
        )

        coldStartUpFingerprint.mutableMethodOrThrow().apply {
            val defaultBrowseIdIndex = indexOfFirstStringInstructionOrThrow(DEFAULT_BROWSE_ID)
            val browseIdIndex = indexOfFirstInstructionReversedOrThrow(defaultBrowseIdIndex) {
                opcode == Opcode.IGET_OBJECT &&
                        getReference<FieldReference>()?.type == "Ljava/lang/String;"
            }
            val browseIdRegister = getInstruction<TwoRegisterInstruction>(browseIdIndex).registerA

            addInstructions(
                browseIdIndex + 1, """
                    invoke-static {v$browseIdRegister}, $EXTENSION_CLASS_DESCRIPTOR->overrideBrowseId(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$browseIdRegister
                    """
            )
        }

        addPreferenceWithIntent(
            CategoryType.GENERAL,
            "extenre_change_start_page"
        )

        updatePatchStatus(CHANGE_START_PAGE)

    }
}
