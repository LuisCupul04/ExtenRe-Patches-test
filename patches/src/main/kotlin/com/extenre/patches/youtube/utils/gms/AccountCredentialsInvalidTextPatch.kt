/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.utils.gms

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.youtube.utils.extension.Constants.UTILS_PATH
import com.extenre.patches.youtube.utils.extension.sharedExtensionPatch
import com.extenre.patches.youtube.utils.resourceid.offlineNoContentBodyTextNotOfflineEligible
import com.extenre.patches.youtube.utils.resourceid.sharedResourceIdPatch
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/AccountCredentialsInvalidTextPatch;"

val accountCredentialsInvalidTextPatch = bytecodePatch(
    name = "account-CredentialsInvalid-Text-Patch",
    description = "accountCredentialsInvalidTextPatch"
) {
    dependsOn(
        sharedExtensionPatch,
        sharedResourceIdPatch,
    )

    execute {
        // If the user recently changed their account password,
        // the app can show "You're offline. Check your internet connection."
        // even when the internet is available.  For this situation
        // YouTube + MicroG shows an offline error message.
        //
        // Change the error text to inform the user to uninstall and reinstall MicroG.
        // The user can also fix this by deleting the MicroG account but
        // MicroG accounts look almost identical to Google device accounts
        // and it's more foolproof to instead uninstall/reinstall.
        arrayOf(
            specificNetworkErrorViewControllerFingerprint,
            loadingFrameLayoutControllerFingerprint
        ).forEach { fingerprint ->
            fingerprint.mutableMethodOrThrow().apply {
                val resourceIndex = indexOfFirstLiteralInstructionOrThrow(
                    offlineNoContentBodyTextNotOfflineEligible
                )
                val getStringIndex = indexOfFirstInstructionOrThrow(resourceIndex) {
                    val reference = getReference<MethodReference>()
                    reference?.name == "getString"
                }
                val register = getInstruction<OneRegisterInstruction>(getStringIndex + 1).registerA

                addInstructions(
                    getStringIndex + 2,
                    """
                        invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->getOfflineNetworkErrorString(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$register  
                    """
                )
            }
        }
    }
}
