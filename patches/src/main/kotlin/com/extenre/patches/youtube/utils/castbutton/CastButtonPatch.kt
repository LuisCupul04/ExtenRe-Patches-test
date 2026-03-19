/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package com.extenre.patches.youtube.utils.castbutton

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.extensions.InstructionExtensions.removeInstruction
import com.extenre.patcher.patch.BytecodePatchContext
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod
import com.extenre.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.extension.Constants.UTILS_PATH
import com.extenre.patches.youtube.utils.resourceid.sharedResourceIdPatch
import com.extenre.util.findMethodOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.updatePatchStatus
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/CastButtonPatch;"

private lateinit var playerButtonMethod: MutableMethod
private lateinit var toolbarMenuItemInitializeMethod: MutableMethod
private lateinit var toolbarMenuItemVisibilityMethod: MutableMethod

val castButtonPatch = bytecodePatch(
    name = "cast-Button-Patch",
    description = "castButtonPatch"
) {
    dependsOn(sharedResourceIdPatch)

    execute {
        toolbarMenuItemInitializeMethod = menuItemInitializeFingerprint.mutableMethodOrThrow()
        toolbarMenuItemVisibilityMethod =
            menuItemVisibilityFingerprint.mutableMethodOrThrow(menuItemInitializeFingerprint)

        playerButtonMethod = playerButtonFingerprint.mutableMethodOrThrow()

        // Reemplazar findmutableMethodOrThrow por búsqueda manual
        run {
            val method = findMethodOrThrow("Landroidx/mediarouter/app/MediaRouteButton;") {
                name == "setVisibility"
            }
            val classDef = classes.find { it.type == "Landroidx/mediarouter/app/MediaRouteButton;" }
                ?: throw PatchException("Class not found: Landroidx/mediarouter/app/MediaRouteButton;")
            val mutableMethod = proxy(classDef).mutableClass.methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.addInstructions(
                0, """
                    invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->hideCastButton(I)I
                    move-result p1
                    """
            )
        }
    }
}

context(BytecodePatchContext)
internal fun hookPlayerCastButton() {
    playerButtonMethod.apply {
        val index = indexOfFirstInstructionOrThrow {
            getReference<MethodReference>()?.name == "setVisibility"
        }
        val instruction = getInstruction<FiveRegisterInstruction>(index)
        val viewRegister = instruction.registerC
        val visibilityRegister = instruction.registerD
        val reference = getInstruction<ReferenceInstruction>(index).reference

        addInstructions(
            index + 1, """
                    invoke-static {v$visibilityRegister}, $PLAYER_CLASS_DESCRIPTOR->hideCastButton(I)I
                    move-result v$visibilityRegister
                    invoke-virtual {v$viewRegister, v$visibilityRegister}, $reference
                    """
        )
        removeInstruction(index)
    }
    updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "PlayerButtons")
}

context(BytecodePatchContext)
internal fun hookToolBarCastButton() {
    toolbarMenuItemInitializeMethod.apply {
        val index = indexOfFirstInstructionOrThrow {
            getReference<MethodReference>()?.name == "setShowAsAction"
        } + 1
        addInstruction(
            index,
            "invoke-static {p1}, $GENERAL_CLASS_DESCRIPTOR->hideCastButton(Landroid/view/MenuItem;)V"
        )
    }
    toolbarMenuItemVisibilityMethod.addInstructions(
        0, """
                invoke-static {p1}, $GENERAL_CLASS_DESCRIPTOR->hideCastButton(Z)Z
                move-result p1
                """
    )
    updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "ToolBarComponents")
}
