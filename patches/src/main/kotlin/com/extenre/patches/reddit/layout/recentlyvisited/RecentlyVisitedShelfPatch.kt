/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.reddit.layout.recentlyvisited

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.reddit.utils.extension.Constants.PATCHES_PATH
import com.extenre.patches.reddit.utils.patch.PatchList.HIDE_RECENTLY_VISITED_SHELF
import com.extenre.patches.reddit.utils.settings.settingsPatch
import com.extenre.patches.reddit.utils.settings.updatePatchStatus
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val EXTENSION_METHOD_DESCRIPTOR =
    "$PATCHES_PATH/RecentlyVisitedShelfPatch;" +
            "->" +
            "hideRecentlyVisitedShelf(Ljava/util/List;)Ljava/util/List;"

@Suppress("unused")
val recentlyVisitedShelfPatch = bytecodePatch(
    name = HIDE_RECENTLY_VISITED_SHELF.key,
    description = "${HIDE_RECENTLY_VISITED_SHELF.title}: ${HIDE_RECENTLY_VISITED_SHELF.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        val recentlyVisitedReference =
            with(communityDrawerPresenterConstructorFingerprint.mutableMethodOrThrow()) {
                val recentlyVisitedFieldIndex = indexOfHeaderItemInstruction(this)
                val recentlyVisitedObjectIndex =
                    indexOfFirstInstructionOrThrow(recentlyVisitedFieldIndex, Opcode.IPUT_OBJECT)

                getInstruction<ReferenceInstruction>(recentlyVisitedObjectIndex).reference.toString()
            }

        communityDrawerPresenterFingerprint.mutableMethodOrThrow(
            communityDrawerPresenterConstructorFingerprint
        ).apply {
            val recentlyVisitedObjectIndex =
                indexOfFirstInstructionOrThrow {
                    (this as? ReferenceInstruction)?.reference?.toString() == recentlyVisitedReference
                }

            arrayOf(
                indexOfFirstInstructionOrThrow(
                    recentlyVisitedObjectIndex,
                    Opcode.INVOKE_STATIC
                ),
                indexOfFirstInstructionReversedOrThrow(
                    recentlyVisitedObjectIndex,
                    Opcode.INVOKE_STATIC
                )
            ).forEach { staticIndex ->
                val insertRegister =
                    getInstruction<OneRegisterInstruction>(staticIndex + 1).registerA

                addInstructions(
                    staticIndex + 2, """
                        invoke-static {v$insertRegister}, $EXTENSION_METHOD_DESCRIPTOR
                        move-result-object v$insertRegister
                        """
                )
            }
        }

        updatePatchStatus(
            "enableRecentlyVisitedShelf",
            HIDE_RECENTLY_VISITED_SHELF
        )
    }
}
