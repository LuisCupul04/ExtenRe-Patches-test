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
import kotlin.collections.*

internal class ReadMeFileGenerator : PatchesFileGenerator {
    // Excepción para ciertos paquetes donde la versión soportada es fija
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

        // Preparar el archivo README (borrar contenido existente si lo hay)
        if (readMeFile.exists()) {
            PrintWriter(readMeFile).use { it.print("") }
        } else {
            Files.createFile(Paths.get(readMeFilePath))
        }

        // Copiar el contenido de la plantilla
        readMeFile.writeText(readMeTemplateFile.readText())

        // Reemplazar los marcadores de versiones compatibles
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

        // Generar la tabla de parches
        val output = StringBuilder()
        patchesByPackage.entries
            .sortedByDescending { it.value.size }
            .forEach { (pkg, patchesForPkg) ->
                output.appendLine("### [\uD83D\uDCE6 `$pkg`](https://play.google.com/store/apps/details?id=$pkg)")
                output.appendLine("<details>\n")
                output.appendLine(tableHeader)

                patchesForPkg.sortedBy { it.name }.forEach { patch ->
                    // Obtener el conjunto de versiones para este paquete
                    val versionSet: Set<String>? = patch.compatiblePackages?.get(pkg)
                    val supportedVersion = if (!versionSet.isNullOrEmpty()) {
                        val list = versionSet.toList()
                        val min = list.minOrNull()!!
                        val max = list.maxOrNull()!!
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

        // Reemplazar el marcador de tabla en el README
        val finalContent = readMeFile.readText().replace(Regex("\\{\\{\\s?table\\s?}}"), output.toString())
        readMeFile.writeText(finalContent)
    }
}