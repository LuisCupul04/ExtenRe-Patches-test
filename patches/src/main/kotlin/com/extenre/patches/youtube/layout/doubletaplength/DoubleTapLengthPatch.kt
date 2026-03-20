/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.layout.doubletaplength

import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.resourcePatch
import com.extenre.patcher.patch.stringOption
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.patch.PatchList.CUSTOM_DOUBLE_TAP_LENGTH
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.ResourceGroup
import com.extenre.util.addEntryValues
import com.extenre.util.copyResources
import com.extenre.util.valueOrThrow
import java.nio.file.Files

@Suppress("unused")
val doubleTapLengthPatch = resourcePatch(
    name = CUSTOM_DOUBLE_TAP_LENGTH.key,
    description = "${CUSTOM_DOUBLE_TAP_LENGTH.title}: ${CUSTOM_DOUBLE_TAP_LENGTH.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    val doubleTapLengthArraysOption = stringOption(
        key = "doubleTapLengthArrays",
        default = "3, 5, 10, 15, 20, 30, 60, 120, 180",
        title = "Double-tap to seek values",
        description = "A list of custom Double-tap to seek lengths to be added, separated by commas.",
        required = true,
    )

    execute {
        // Check patch options first.
        val doubleTapLengthArrays = doubleTapLengthArraysOption
            .valueOrThrow()

        // Check patch options first.
        val splits = doubleTapLengthArrays
            .replace(" ", "")
            .split(",")
        if (splits.isEmpty()) throw PatchException("Invalid double-tap length elements")
        val lengthElements = splits.map { it }

        val arrayPath = "res/values-v21/arrays.xml"
        val entriesName = "double_tap_length_entries"
        val entryValueName = "double_tap_length_values"

        val valuesV21Directory = get("res").resolve("values-v21")
        if (!valuesV21Directory.isDirectory)
            Files.createDirectories(valuesV21Directory.toPath())

        /**
         * Copy arrays
         */
        copyResources(
            "youtube/doubletap",
            ResourceGroup(
                "values-v21",
                "arrays.xml"
            )
        )

        for (index in 0 until splits.count()) {
            addEntryValues(
                entryValueName,
                lengthElements[index],
                path = arrayPath
            )
            addEntryValues(
                entriesName,
                lengthElements[index],
                path = arrayPath
            )
        }

        addPreference(CUSTOM_DOUBLE_TAP_LENGTH)

    }
}
