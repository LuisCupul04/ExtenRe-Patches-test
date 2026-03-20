/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.video.playerresponse

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod
import com.extenre.patches.music.utils.extension.sharedExtensionPatch
import com.extenre.patches.music.utils.playservice.is_7_03_or_greater
import com.extenre.patches.music.utils.playservice.versionCheckPatch
import com.extenre.util.fingerprint.matchOrThrow
import com.android.tools.smali.dexlib2.util.MethodUtil

private val hooks = mutableSetOf<Hook>()

fun addPlayerResponseMethodHook(hook: Hook) {
    hooks += hook
}

private const val REGISTER_VIDEO_ID = "p1"
private const val REGISTER_PLAYER_PARAMETER = "p3"
private const val REGISTER_PLAYLIST_ID = "p4"
private const val REGISTER_PLAYLIST_INDEX = "p5"

private lateinit var playerResponseMethod: MutableMethod
private var numberOfInstructionsAdded = 0

val playerResponseMethodHookPatch = bytecodePatch(
    name = "player-response-method-hook-patch",
    description = "playerResponseMethodHookPatch"
) {
    dependsOn(
        sharedExtensionPatch,
        versionCheckPatch,
    )

    execute {
        val match = if (is_7_03_or_greater) {
            playerParameterBuilderFingerprint.matchOrThrow()
        } else {
            playerParameterBuilderLegacyFingerprint.matchOrThrow()
        }
        val method = match.method
        val classDef = match.classDef
        playerResponseMethod = mutableClassDefBy(classDef.type).methods.first {
            MethodUtil.methodSignaturesMatch(it, method)
        }
    }

    finalize {
        fun hookVideoId(hook: Hook) {
            playerResponseMethod.addInstruction(
                0,
                "invoke-static {$REGISTER_VIDEO_ID}, $hook",
            )
            numberOfInstructionsAdded++
        }

        fun hookVideoIdAndPlaylistId(hook: Hook) {
            playerResponseMethod.addInstruction(
                0,
                "invoke-static {$REGISTER_VIDEO_ID, $REGISTER_PLAYLIST_ID, $REGISTER_PLAYLIST_INDEX}, $hook",
            )
            numberOfInstructionsAdded++
        }

        fun hookPlayerParameter(hook: Hook) {
            playerResponseMethod.addInstructions(
                0,
                """
                    invoke-static {$REGISTER_VIDEO_ID, v0}, $hook
                    move-result-object v0
                    """,
            )
            numberOfInstructionsAdded += 2
        }

        // Reverse the order in order to preserve insertion order of the hooks.
        val beforeVideoIdHooks =
            hooks.filterIsInstance<Hook.PlayerParameterBeforeVideoId>().asReversed()
        val videoIdHooks = hooks.filterIsInstance<Hook.VideoId>().asReversed()
        val videoIdAndPlaylistIdHooks =
            hooks.filterIsInstance<Hook.VideoIdAndPlaylistId>().asReversed()
        val afterVideoIdHooks = hooks.filterIsInstance<Hook.PlayerParameter>().asReversed()

        // Add the hooks in this specific order as they insert instructions at the beginning of the method.
        afterVideoIdHooks.forEach(::hookPlayerParameter)
        videoIdAndPlaylistIdHooks.forEach(::hookVideoIdAndPlaylistId)
        videoIdHooks.forEach(::hookVideoId)
        beforeVideoIdHooks.forEach(::hookPlayerParameter)

        playerResponseMethod.apply {
            addInstruction(
                0,
                "move-object/from16 v0, $REGISTER_PLAYER_PARAMETER"
            )
            numberOfInstructionsAdded++

            // Move the modified register back.
            addInstruction(
                numberOfInstructionsAdded,
                "move-object/from16 $REGISTER_PLAYER_PARAMETER, v0"
            )
        }
    }
}

sealed class Hook(private val methodDescriptor: String) {
    class VideoId(methodDescriptor: String) : Hook(methodDescriptor)
    class VideoIdAndPlaylistId(methodDescriptor: String) : Hook(methodDescriptor)

    class PlayerParameter(methodDescriptor: String) : Hook(methodDescriptor)
    class PlayerParameterBeforeVideoId(methodDescriptor: String) : Hook(methodDescriptor)

    override fun toString() = methodDescriptor
}