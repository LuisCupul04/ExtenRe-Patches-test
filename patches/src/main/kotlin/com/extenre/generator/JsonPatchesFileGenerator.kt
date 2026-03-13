/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.generator

import com.extenre.patcher.patch.Patch
import com.google.gson.GsonBuilder
import java.io.File

typealias PackageName = String
typealias VersionName = String

internal class JsonPatchesFileGenerator : PatchesFileGenerator {
    override fun generate(patches: Set<Patch<*>>) {
        val patchesJson = File("../patches-exre.json")
        patches.sortedBy { it.name }.map { patch ->
            JsonPatch(
                name = patch.name!!,                     // Cambiado de key a name
                description = patch.description,         // Cambiado de title a description
                use = patch.use,
                dependencies = patch.dependencies.map { dependency -> dependency.name ?: dependency.toString() },
                compatiblePackages = patch.compatiblePackages?.associate { (packageName, versions) -> packageName to versions },
                options = patch.options.values.map { option ->
                    JsonPatch.Option(
                        key = option.key,                // Correcto: Option tiene key
                        title = option.title,            // Correcto: Option tiene title
                        description = option.description,
                        required = option.required,
                        type = option.type.toString(),
                        default = option.default,
                        values = option.values,
                    )
                },
            )
        }.let { jsonPatches ->
            patchesJson.writeText(
                GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(jsonPatches)
            )
        }
    }

    @Suppress("unused")
    private class JsonPatch(
        val name: String? = null,
        val description: String? = null,
        val use: Boolean = true,
        val dependencies: List<String>,
        val compatiblePackages: Map<PackageName, Set<VersionName>?>? = null,
        val options: List<Option>,
    ) {
        class Option(
            val key: String,
            val title: String?,
            val description: String?,
            val required: Boolean,
            val type: String,
            val default: Any?,
            val values: Map<String, Any?>?,
        )
    }
}