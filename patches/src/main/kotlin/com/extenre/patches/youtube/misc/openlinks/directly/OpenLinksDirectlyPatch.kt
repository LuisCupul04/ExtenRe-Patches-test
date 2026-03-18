/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.misc.openlinks.directly

import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.extensions.InstructionExtensions.replaceInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.MISC_PATH
import com.extenre.patches.youtube.utils.patch.PatchList.BYPASS_URL_REDIRECTS
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val openLinksDirectlyPatch = bytecodePatch(
    name = BYPASS_URL_REDIRECTS.key,
    description = "${BYPASS_URL_REDIRECTS.title}: ${BYPASS_URL_REDIRECTS.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        arrayOf(
            openLinksDirectlyFingerprintPrimary,
            openLinksDirectlyFingerprintSecondary
        ).forEach { fingerprint ->
            fingerprint.mutableMethodOrThrow().apply {
                val insertIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_STATIC &&
                            getReference<MethodReference>()?.name == "parse"
                }
                val insertRegister =
                    getInstruction<FiveRegisterInstruction>(insertIndex).registerC

                replaceInstruction(
                    insertIndex,
                    "invoke-static {v$insertRegister}, $MISC_PATH/OpenLinksDirectlyPatch;->parseRedirectUri(Ljava/lang/String;)Landroid/net/Uri;"
                )
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "SETTINGS: BYPASS_URL_REDIRECTS"
            ),
            BYPASS_URL_REDIRECTS
        )

        // endregion

    }
}
