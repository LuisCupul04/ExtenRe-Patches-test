/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.misc.tracking

import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.music.utils.patch.PatchList.SANITIZE_SHARING_LINKS
import com.extenre.patches.music.utils.playservice.is_8_05_or_greater
import com.extenre.patches.music.utils.playservice.versionCheckPatch
import com.extenre.patches.music.utils.settings.CategoryType
import com.extenre.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import com.extenre.patches.music.utils.settings.addSwitchPreference
import com.extenre.patches.music.utils.settings.settingsPatch
import com.extenre.patches.shared.tracking.baseSanitizeUrlQueryPatch
import com.extenre.patches.shared.tracking.hookQueryParameters
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val sanitizeUrlQueryPatch = bytecodePatch(
    name = SANITIZE_SHARING_LINKS.key,
    description = "${SANITIZE_SHARING_LINKS.title}: ${SANITIZE_SHARING_LINKS.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        baseSanitizeUrlQueryPatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {

        if (is_8_05_or_greater) {
            imageShareLinkFormatterFingerprint.mutableMethodOrThrow().apply {
                val stringIndex = indexOfFirstStringInstructionOrThrow("android.intent.extra.TEXT")
                val insertIndex = indexOfFirstInstructionOrThrow(stringIndex) {
                    val reference = getReference<MethodReference>()
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            reference?.name == "putExtra" &&
                            reference.definingClass == "Landroid/content/Intent;"
                }

                hookQueryParameters(insertIndex)
            }
        }

        addSwitchPreference(
            CategoryType.MISC,
            "extenre_sanitize_sharing_links",
            "true"
        )

        updatePatchStatus(SANITIZE_SHARING_LINKS)

    }
}
