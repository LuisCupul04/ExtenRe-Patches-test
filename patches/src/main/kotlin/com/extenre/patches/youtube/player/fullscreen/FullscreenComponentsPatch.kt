/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.player.fullscreen

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.smali.ExternalLabel
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.patch.PatchList.FULLSCREEN_COMPONENTS
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.getWalkerMethod
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

@Suppress("unused")
val fullscreenComponentsPatch = bytecodePatch(
    name = FULLSCREEN_COMPONENTS.key,
    description = "${FULLSCREEN_COMPONENTS.title}: ${FULLSCREEN_COMPONENTS.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        val fullscreenButtonMatch = fullscreenButtonFingerprint.matchOrThrow()
        val fullscreenButtonMethod = fullscreenButtonMatch.method
        val fullscreenButtonClassDef = fullscreenButtonMatch.classDef
        val fullscreenButtonMutableMethod = mutableClassDefBy(fullscreenButtonClassDef.type).methods.first {
            MethodUtil.methodSignaturesMatch(it, fullscreenButtonMethod)
        }
        fullscreenButtonMutableMethod.apply {
            val insertIndex = fullscreenButtonMatch.patternMatch!!.endIndex
            val register = getInstruction<OneRegisterInstruction>(insertIndex).registerA
            addInstruction(
                insertIndex,
                "invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->hideFullscreenButton(Landroid/view/View;)V"
            )
        }

        val fullscreenDialogMatch = fullscreenDialogFingerprint.matchOrThrow()
        val fullscreenDialogMethod = fullscreenDialogMatch.method
        val fullscreenDialogClassDef = fullscreenDialogMatch.classDef
        val fullscreenDialogMutableMethod = mutableClassDefBy(fullscreenDialogClassDef.type).methods.first {
            MethodUtil.methodSignaturesMatch(it, fullscreenDialogMethod)
        }
        fullscreenDialogMutableMethod.apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideFullscreenDialog()Z
                    move-result v0
                    if-eqz v0, :show
                    return-void
                    """, ExternalLabel("show", getInstruction(0))
            )
        }

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: FULLSCREEN_COMPONENTS"
            ),
            FULLSCREEN_COMPONENTS
        )
    }
}