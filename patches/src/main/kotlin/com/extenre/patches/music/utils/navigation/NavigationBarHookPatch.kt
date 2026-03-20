/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.utils.navigation

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.music.utils.extension.Constants.SHARED_PATH
import com.extenre.patches.music.utils.extension.sharedExtensionPatch
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val EXTENSION_CLASS_DESCRIPTOR =
    "$SHARED_PATH/NavigationBar;"

val navigationBarHookPatch = bytecodePatch(
    name = "navigation-bar-hook-patch",
    description = "navigationBarHookPatch"
) {
    dependsOn(sharedExtensionPatch)

    execute {
        tabLayoutViewSetSelectedFingerprint.mutableMethodOrThrow().apply {
            val childAtIndex = indexOfChildAtInstruction(this)
            val tabIndexRegister =
                getInstruction<FiveRegisterInstruction>(childAtIndex).registerD

            implementation!!.instructions
                .withIndex()
                .filter { (_, instruction) ->
                    val reference = (instruction as? ReferenceInstruction)?.reference
                    reference is MethodReference &&
                            reference.name == "setActivated"
                }
                .map { (index, _) -> index }
                .reversed()
                .forEach { index ->
                    val isSelectedRegister =
                        getInstruction<FiveRegisterInstruction>(childAtIndex).registerD

                    addInstruction(
                        index,
                        "invoke-static {v$tabIndexRegister, v$isSelectedRegister}, " +
                                "$EXTENSION_CLASS_DESCRIPTOR->navigationTabSelected(IZ)V"
                    )
                }
        }
    }
}
