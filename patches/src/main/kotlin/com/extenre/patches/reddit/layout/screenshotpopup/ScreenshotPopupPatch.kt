/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.reddit.layout.screenshotpopup

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.reddit.utils.extension.Constants.PATCHES_PATH
import com.extenre.patches.reddit.utils.patch.PatchList.DISABLE_SCREENSHOT_POPUP
import com.extenre.patches.reddit.utils.settings.settingsPatch
import com.extenre.patches.reddit.utils.settings.updatePatchStatus
import com.extenre.util.findMutableMethodOf
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val screenshotPopupPatch = bytecodePatch(
    name = DISABLE_SCREENSHOT_POPUP.key,
    description = "${DISABLE_SCREENSHOT_POPUP.title}: ${DISABLE_SCREENSHOT_POPUP.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        fun indexOfShowBannerInstruction(method: Method) =
            method.indexOfFirstInstruction {
                val reference = getReference<FieldReference>()
                opcode == Opcode.IGET_OBJECT &&
                        reference?.name?.contains("shouldShowBanner") == true &&
                        reference.definingClass.startsWith("Lcom/reddit/sharing/screenshot/") == true
            }

        fun indexOfSetValueInstruction(method: Method) =
            method.indexOfFirstInstruction {
                getReference<MethodReference>()?.name == "setValue"
            }

        fun indexOfBooleanInstruction(method: Method, startIndex: Int = 0) =
            method.indexOfFirstInstruction(startIndex) {
                val reference = getReference<FieldReference>()
                opcode == Opcode.SGET_OBJECT &&
                        reference?.definingClass == "Ljava/lang/Boolean;" &&
                        reference.type == "Ljava/lang/Boolean;"
            }

        val isScreenShotMethod: Method.() -> Boolean = {
            definingClass.startsWith("Lcom/reddit/sharing/screenshot/") &&
                    name == "invokeSuspend" &&
                    indexOfShowBannerInstruction(this) >= 0 &&
                    indexOfBooleanInstruction(this) >= 0 &&
                    indexOfSetValueInstruction(this) >= 0
        }

        var hookCount = 0

        classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                if (method.isScreenShotMethod()) {
                    proxy(classDef)
                        .mutableClass
                        .findMutableMethodOf(method)
                        .apply {
                            val showBannerIndex = indexOfShowBannerInstruction(this)
                            val booleanIndex = indexOfBooleanInstruction(this, showBannerIndex)
                            val booleanRegister =
                                getInstruction<OneRegisterInstruction>(booleanIndex).registerA

                            addInstructions(
                                booleanIndex + 1, """
                                    invoke-static {v$booleanRegister}, $PATCHES_PATH/ScreenshotPopupPatch;->disableScreenshotPopup(Ljava/lang/Boolean;)Ljava/lang/Boolean;
                                    move-result-object v$booleanRegister
                                    """
                            )
                            hookCount++
                        }
                }
            }
        }

        if (hookCount == 0) {
            throw PatchException("Failed to find hook method")
        }

        updatePatchStatus(
            "enableScreenshotPopup",
            DISABLE_SCREENSHOT_POPUP
        )
    }
}
