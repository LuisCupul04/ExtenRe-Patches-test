/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.reddit.layout.trendingtoday

import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.smali.ExternalLabel
import com.extenre.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.reddit.utils.extension.Constants.PATCHES_PATH
import com.extenre.patches.reddit.utils.patch.PatchList.HIDE_TRENDING_TODAY_SHELF
import com.extenre.patches.reddit.utils.settings.settingsPatch
import com.extenre.patches.reddit.utils.settings.updatePatchStatus
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/TrendingTodayShelfPatch;"

@Suppress("unused")
val trendingTodayShelfPatch = bytecodePatch(
    name = HIDE_TRENDING_TODAY_SHELF.key,
    description = "${HIDE_TRENDING_TODAY_SHELF.title}: ${HIDE_TRENDING_TODAY_SHELF.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        // region patch for hide trending today title.

        val trendingTodayTitleMatch = trendingTodayTitleFingerprint.matchOrThrow()
        val titleMethod = trendingTodayTitleMatch.method
        val titleClassDef = trendingTodayTitleMatch.classDef
        val titleMutableMethod = proxy(titleClassDef).mutableClass.methods.first {
            MethodUtil.methodSignaturesMatch(it, titleMethod)
        }
        titleMutableMethod.apply {
            val stringIndex = trendingTodayTitleMatch.stringMatches!!.first().index
            val relativeIndex =
                indexOfFirstInstructionReversedOrThrow(stringIndex, Opcode.AND_INT_LIT8)
            val insertIndex = indexOfFirstInstructionReversedOrThrow(
                relativeIndex + 1,
                Opcode.MOVE_OBJECT_FROM16
            )
            val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA
            val jumpOpcode = if (returnType == "V") Opcode.RETURN_VOID else Opcode.SGET_OBJECT
            val jumpIndex = indexOfFirstInstructionReversedOrThrow(jumpOpcode)

            addInstructionsWithLabels(
                insertIndex, """
                        invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->hideTrendingTodayShelf()Z
                        move-result v$insertRegister
                        if-nez v$insertRegister, :hidden
                        """, ExternalLabel("hidden", getInstruction(jumpIndex))
            )
        }

        // endregion

        // region patch for hide trending today contents.

        trendingTodayItemFingerprint.mutableMethodOrThrow().addInstructionsWithLabels(
            0, """
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->hideTrendingTodayShelf()Z
                move-result v0
                if-eqz v0, :ignore
                return-void
                :ignore
                nop
                """
        )

        // endregion

        updatePatchStatus(
            "enableTrendingTodayShelf",
            HIDE_TRENDING_TODAY_SHELF
        )
    }
}
