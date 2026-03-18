/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.utils.request

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod
import com.extenre.patches.shared.buildRequestFingerprint
import com.extenre.patches.shared.buildRequestParentFingerprint
import com.extenre.patches.shared.indexOfEntrySetInstruction
import com.extenre.patches.shared.indexOfNewUrlRequestBuilderInstruction
import com.extenre.patches.youtube.utils.extension.sharedExtensionPatch
import com.extenre.util.findFreeRegister
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private lateinit var buildInitPlaybackRequestMethod: MutableMethod
private var initPlaybackUrlRegister = 0
private var initPlaybackMapRegister = 0

private lateinit var buildRequestMethod: MutableMethod
private var urlRegister = 0
private var mapRegister = 0
private var offSet = 0

val buildRequestPatch = bytecodePatch(
    name = "build-Request-Patch",
    description = "buildRequestPatch"
) {
    dependsOn(sharedExtensionPatch)

    execute {
        buildInitPlaybackRequestFingerprint.mutableMethodOrThrow().apply {
            buildInitPlaybackRequestMethod = this
            val mapIndex = indexOfMapInstruction(this)
            val mapField = getInstruction<ReferenceInstruction>(mapIndex).reference

            val uriIndex = indexOfUriToStringInstruction(this) + 1
            initPlaybackUrlRegister =
                getInstruction<OneRegisterInstruction>(uriIndex).registerA

            initPlaybackMapRegister =
                findFreeRegister(uriIndex, false, initPlaybackUrlRegister)

            addInstruction(
                0,
                "iget-object v$initPlaybackMapRegister, p1, $mapField"
            )
        }

        buildRequestFingerprint.mutableMethodOrThrow(buildRequestParentFingerprint).apply {
            buildRequestMethod = this

            val newRequestBuilderIndex = indexOfNewUrlRequestBuilderInstruction(this)
            urlRegister =
                getInstruction<FiveRegisterInstruction>(newRequestBuilderIndex).registerD

            val entrySetIndex = indexOfEntrySetInstruction(this)
            val isLegacyTarget = entrySetIndex < 0
            mapRegister = if (isLegacyTarget)
                urlRegister + 1
            else
                getInstruction<FiveRegisterInstruction>(entrySetIndex).registerC

            if (isLegacyTarget) {
                addInstructions(
                    newRequestBuilderIndex + 2,
                    "move-object/from16 v$mapRegister, p1"
                )
                offSet++
            }
        }
    }
}

internal fun hookBuildRequest(descriptor: String) {
    buildRequestMethod.apply {
        val insertIndex = indexOfNewUrlRequestBuilderInstruction(this) + 2 + offSet

        addInstructions(
            insertIndex,
            "invoke-static { v$urlRegister, v$mapRegister }, $descriptor"
        )
    }
}

internal fun hookBuildRequestUrl(descriptor: String) {
    buildRequestMethod.apply {
        val insertIndex = indexOfNewUrlRequestBuilderInstruction(this)

        addInstructions(
            insertIndex, """
                invoke-static { v$urlRegister }, $descriptor
                move-result-object v$urlRegister
                """
        )
    }
}

internal fun hookInitPlaybackBuildRequest(descriptor: String) {
    buildInitPlaybackRequestMethod.apply {
        val insertIndex = indexOfUriToStringInstruction(this) + 2

        addInstructions(
            insertIndex,
            "invoke-static { v$initPlaybackUrlRegister, v$initPlaybackMapRegister }, $descriptor"
        )
    }
}
