/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.layout.theme

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.extensions.InstructionExtensions.replaceInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.booleanOption
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.patch.resourcePatch
import com.extenre.patcher.patch.stringOption
import com.extenre.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.music.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import com.extenre.patches.music.utils.extension.Constants.UTILS_PATH
import com.extenre.patches.music.utils.patch.PatchList.DARK_THEME
import com.extenre.patches.music.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import com.extenre.patches.music.utils.settings.settingsPatch
import com.extenre.patches.shared.drawable.addDrawableColorHook
import com.extenre.patches.shared.drawable.drawableColorHookPatch
import com.extenre.patches.shared.materialyou.baseMaterialYou
import com.extenre.util.ResourceGroup
import com.extenre.util.copyResources
import com.extenre.util.findMethodOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.extenre.util.valueOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.util.MethodUtil
import org.w3c.dom.Element

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/DrawableColorPatch;"

private val darkThemeBytecodePatch = bytecodePatch(
    name = "dark-theme-bytecode-patch",
    description = "darkThemeBytecodePatch"
) {
    dependsOn(
        settingsPatch,
        sharedResourceIdPatch,
        drawableColorHookPatch,
    )

    execute {
        addDrawableColorHook("$EXTENSION_CLASS_DESCRIPTOR->getLithoColor(I)I")

        // The images in the playlist and album headers have a black gradient (probably applied server-side).
        // Applies a new gradient to the images in the playlist and album headers.
        elementsContainerFingerprint.mutableMethodOrThrow().apply {
            val index = indexOfFirstInstructionReversedOrThrow(Opcode.CHECK_CAST)
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            addInstruction(
                index + 1,
                "invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->setHeaderGradient(Landroid/view/ViewGroup;)V"
            )
        }

        // Reemplazar mutableMethodOrThrow por búsqueda manual
        run {
            val method = findMethodOrThrow(PATCH_STATUS_CLASS_DESCRIPTOR) {
                name == "DarkTheme"
            }
            val classDef = classes.find { it.type == PATCH_STATUS_CLASS_DESCRIPTOR }
                ?: throw PatchException("Class not found: $PATCH_STATUS_CLASS_DESCRIPTOR")
            val mutableMethod = proxy(classDef).mutableClass.methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.replaceInstruction(
                0,
                "const/4 v0, 0x1"
            )
        }
    }
}

val DARK_COLOR = arrayOf(
    "yt_black0", "yt_black1", "yt_black1_opacity95", "yt_black1_opacity98",
    "yt_black2", "yt_black3", "yt_black4", "yt_black_pure",
    "yt_black_pure_opacity80", "yt_status_bar_background_dark",
    "ytm_color_grey_12", "material_grey_800", "material_grey_850",
)

@Suppress("unused")
val darkThemePatch = resourcePatch(
    name = DARK_THEME.key,
    description = "${DARK_THEME.title}: ${DARK_THEME.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(darkThemeBytecodePatch)

    val amoledBlackColor = "@android:color/black"

    val darkThemeBackgroundColor = stringOption(
        key = "darkThemeBackgroundColor",
        default = amoledBlackColor,
        values = mapOf(
            "Amoled Black" to amoledBlackColor,
            "Catppuccin (Mocha)" to "#FF181825",
            "Dark Pink" to "#FF290025",
            "Dark Blue" to "#FF001029",
            "Dark Green" to "#FF002905",
            "Dark Yellow" to "#FF282900",
            "Dark Orange" to "#FF291800",
            "Dark Red" to "#FF290000",
        ),
        title = "Dark theme background color",
        description = "Can be a hex color (#AARRGGBB) or a color resource reference.",
    )

    val materialYou by booleanOption(
        key = "materialYou",
        default = false,
        title = "MaterialYou",
        description = "Applies the MaterialYou theme for Android 12+ devices.",
        required = true
    )

    execute {
        // Check patch options first.
        val darkThemeColor = darkThemeBackgroundColor
            .valueOrThrow()

        document("res/values/colors.xml").use { document ->
            val resourcesNode = document.documentElement
            val childNodes = resourcesNode.childNodes

            for (i in 0 until childNodes.length) {
                val node = childNodes.item(i) as? Element ?: continue
                val colorName = node.getAttribute("name")

                if (DARK_COLOR.contains(colorName)) {
                    node.textContent = darkThemeColor
                }
            }
        }

        arrayOf(
            ResourceGroup(
                "drawable",
                "extenre_header_gradient.xml",
            )
        ).forEach { resourceGroup ->
            copyResources("music/theme", resourceGroup)
        }

        if (materialYou == true) {
            baseMaterialYou()

            document("res/values-v31/colors.xml").use { document ->
                DARK_COLOR.forEach { name ->
                    val colorElement = document.createElement("color")
                    colorElement.setAttribute("name", name)
                    colorElement.textContent = "@android:color/system_neutral1_900"

                    document.getElementsByTagName("resources").item(0).appendChild(colorElement)
                }
            }
        }

        updatePatchStatus(DARK_THEME)

    }
}
