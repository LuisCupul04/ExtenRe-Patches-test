/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.player.miniplayer.startup

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.PLAYER_PATH
import com.extenre.patches.youtube.utils.patch.PatchList.DISABLE_RESUMING_MINIPLAYER_ON_STARTUP
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.fingerprint.matchOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PLAYER_PATH/MiniplayerPatch;"

// YT uses "Miniplayer" without a space between 'mini' and 'player: https://support.google.com/youtube/answer/9162927.
@Suppress("unused", "SpellCheckingInspection")
val resumingMiniplayerOnStartupPatch = bytecodePatch(
    name = DISABLE_RESUMING_MINIPLAYER_ON_STARTUP.key,
    description = "${DISABLE_RESUMING_MINIPLAYER_ON_STARTUP.title}: ${DISABLE_RESUMING_MINIPLAYER_ON_STARTUP.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        showMiniplayerCommandFingerprint.matchOrThrow().let {
            it.method.apply {
                val insertIndex = it.patternMatch!!.endIndex
                val insertRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $EXTENSION_CLASS_DESCRIPTOR->disableResumingStartupMiniPlayer(Z)Z
                        move-result v$insertRegister
                        """
                )
            }
        }


        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: MINIPLAYER_COMPONENTS",
                "SETTINGS: DISABLE_RESUMING_MINIPLAYER"
            ),
            DISABLE_RESUMING_MINIPLAYER_ON_STARTUP
        )

    }
}
