/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.generator

import com.extenre.patcher.patch.Patch
import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

internal class ReadMeFileGenerator : PatchesFileGenerator {
    private val exception = mapOf(
        "com.google.android.apps.youtube.music" to "6.29.59"
    )

    private val tableHeader =
        "| \uD83D\uDCA8 Patch | \uD83D\uDCDC Description | \uD83C\uDFF9 Target Version |\n" +
                "|:--------:|:--------------:|:-----------------:|"

    override fun generate(patches: Set<Patch<*>>) {
        val rootPath = Paths.get("").toAbsolutePath().parent!!
        val readMeFilePath = "$rootPath/README.md"
        val readMeFile = File(readMeFilePath)
        val readMeTemplateFile = File("$rootPath/README-template.md")

        if (readMeFile.exists()) {
            PrintWriter(readMeFile).use { it.print("") }
        } else {
            Files.createFile(Paths.get(readMeFilePath))
        }

        readMeFile.writeText(readMeTemplateFile.readText())

        // Reemplazar marcadores de versiones compatibles
        val markers = mapOf(
            com.extenre.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE to "\"COMPATIBLE_PACKAGE_MUSIC\"",
            com.extenre.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE to "\"COMPATIBLE_PACKAGE_REDDIT\"",
            com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE to "\"COMPATIBLE_PACKAGE_YOUTUBE\""
        )

        var currentContent = readMeFile.readText()
        markers.forEach { (compatiblePackage, marker) ->
            val (packageName, versions) = compatiblePackage
            val supportedVersion = when {
                versions == null && exception.containsKey(packageName) -> exception[packageName] + "+"
                versions != null -> versions.joinToString(
                    prefix = "[\n        \"",
                    separator = "\",\n        \"",
                    postfix = "\"\n      ]"
                )
                else -> "\"ALL\""
            }
            currentContent = currentContent.replace(Regex(marker), supportedVersion)
        }
        readMeFile.writeText(currentContent)

        // Agrupar parches por paquete compatible
        val patchesByPackage = mutableMapOf<String, MutableSet<Patch<*>>>()
        for (patch in patches) {
            patch.compatiblePackages?.forEach { (packageName, _) ->
                patchesByPackage.getOrPut(packageName) { mutableSetOf() }.add(patch)
            }
        }

        // Generar tabla de parches
        val output = StringBuilder()
        patchesByPackage.entries
            .sortedByDescending { it.value.size }
            .forEach { (pkg, patchesForPkg) ->
                output.appendLine("### [\uD83D\uDCE6 `$pkg`](https://play.google.com/store/apps/details?id=$pkg)")
                output.appendLine("<details>\n")
                output.appendLine(tableHeader)

                patchesForPkg.sortedBy { it.name }.forEach { patch ->
                    // Buscar el conjunto de versiones para este paquete sin usar get ni entries
                    var versionSet: Set<String>? = null
                    patch.compatiblePackages?.forEach { (key, value) ->
                        if (key == pkg) {
                            versionSet = value
                        }
                    }
                    val supportedVersion = if (versionSet != null && !versionSet.isEmpty()) {
                        val list = ArrayList(versionSet)
                        Collections.sort(list)
                        val min = list[0]
                        val max = list[list.size - 1]
                        if (min == max) min else "$min ~ $max"
                    } else if (exception.containsKey(pkg)) {
                        exception[pkg] + "+"
                    } else {
                        "ALL"
                    }

                    output.appendLine(
                        "| `${patch.name}` " +
                                "| ${patch.description} " +
                                "| $supportedVersion |"
                    )
                }
                output.appendLine("</details>\n")
            }

        // Insertar tabla en el README
        val finalContent = readMeFile.readText().replace(Regex("\\{\\{\\s?table\\s?}}"), output.toString())
        readMeFile.writeText(finalContent)
    }
}