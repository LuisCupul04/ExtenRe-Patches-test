/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.general.updates

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.patch.PatchList.DISABLE_LAYOUT_UPDATES
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.fingerprint.matchOrThrow

@Suppress("unused")
val layoutUpdatesPatch = bytecodePatch(
    name = DISABLE_LAYOUT_UPDATES.key,
    description = "${DISABLE_LAYOUT_UPDATES.title}: ${DISABLE_LAYOUT_UPDATES.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        cronetHeaderFingerprint.matchOrThrow().let {
            it.method.apply {
                val index = it.stringMatches!!.first().index

                addInstructions(
                    index, """
                        invoke-static {p1, p2}, $GENERAL_CLASS_DESCRIPTOR->disableLayoutUpdates(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                        move-result-object p2
                        """
                )
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS",
                "SETTINGS: DISABLE_LAYOUT_UPDATES"
            ),
            DISABLE_LAYOUT_UPDATES
        )

        // endregion

    }
}
