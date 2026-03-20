/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.utils.fix.fileprovider

import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.smali.ExternalLabel
import com.extenre.util.fingerprint.mutableMethodOrThrow

fun fileProviderPatch(
    youtubePackageName: String,
    musicPackageName: String
) = bytecodePatch(
    name = "file-provider-patch",
    description = "fileProviderPatch"
) {
    execute {

        /**
         * For some reason, if the app gets "android.support.FILE_PROVIDER_PATHS",
         * the package name of YouTube is used, not the package name of the YT Music.
         *
         * There is no issue in the stock YT Music, but this is an issue in the GmsCore Build.
         * https://github.com/inotia00/ReVanced_Extended/issues/1830
         *
         * To solve this issue, replace the package name of YouTube with YT Music's package name.
         */
        fileProviderResolverFingerprint.mutableMethodOrThrow().apply {
            addInstructionsWithLabels(
                0, """
                    const-string v0, "com.google.android.youtube.fileprovider"
                    invoke-static {p1, v0}, Ljava/util/Objects;->equals(Ljava/lang/Object;Ljava/lang/Object;)Z
                    move-result v0
                    if-nez v0, :fix
                    const-string v0, "$youtubePackageName.fileprovider"
                    invoke-static {p1, v0}, Ljava/util/Objects;->equals(Ljava/lang/Object;Ljava/lang/Object;)Z
                    move-result v0
                    if-nez v0, :fix
                    goto :ignore
                    :fix
                    const-string p1, "$musicPackageName.fileprovider"
                    """, ExternalLabel("ignore", getInstruction(0))
            )
        }

    }
}
