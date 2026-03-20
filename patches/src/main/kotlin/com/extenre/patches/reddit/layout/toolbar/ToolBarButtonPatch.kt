/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.reddit.layout.toolbar

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.reddit.utils.extension.Constants.PATCHES_PATH
import com.extenre.patches.reddit.utils.patch.PatchList.HIDE_TOOLBAR_BUTTON
import com.extenre.patches.reddit.utils.settings.settingsPatch
import com.extenre.patches.reddit.utils.settings.updatePatchStatus
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_METHOD_DESCRIPTOR =
    "$PATCHES_PATH/ToolBarButtonPatch;->hideToolBarButton(Landroid/view/View;)V"

@Suppress("unused")
@Deprecated("This patch is deprecated until Reddit adds a button like r/place or Reddit recap button to the toolbar.")
val toolBarButtonPatch = bytecodePatch(
    name = HIDE_TOOLBAR_BUTTON.key,
    description = "${HIDE_TOOLBAR_BUTTON.title}: ${HIDE_TOOLBAR_BUTTON.summary}",
) {
    // compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        val homePagerScreenMatch = homePagerScreenFingerprint.matchOrThrow()
        val method = homePagerScreenMatch.method
        val classDef = homePagerScreenMatch.classDef
        val mutableMethod = proxy(classDef).mutableClass.methods.first {
            MethodUtil.methodSignaturesMatch(it, method)
        }
        mutableMethod.apply {
            val stringIndex = homePagerScreenMatch.stringMatches!!.first().index
            val insertIndex = indexOfFirstInstructionOrThrow(stringIndex, Opcode.CHECK_CAST)
            val insertRegister =
                getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstruction(
                insertIndex,
                "invoke-static {v$insertRegister}, $EXTENSION_METHOD_DESCRIPTOR"
            )
        }

        updatePatchStatus(
            "enableToolBarButton",
            HIDE_TOOLBAR_BUTTON
        )
    }
}
