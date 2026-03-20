/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.utils.settings

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.extensions.InstructionExtensions.replaceInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.patch.resourcePatch
import com.extenre.patcher.patch.stringOption
import com.extenre.patcher.util.smali.ExternalLabel
import com.extenre.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.music.utils.extension.Constants.EXTENSION_PATH
import com.extenre.patches.music.utils.extension.Constants.UTILS_PATH
import com.extenre.patches.music.utils.extension.sharedExtensionPatch
import com.extenre.patches.music.utils.fix.timedlyrics.timedLyricsPatch
import com.extenre.patches.music.utils.mainactivity.mainActivityResolvePatch
import com.extenre.patches.music.utils.patch.PatchList.GMSCORE_SUPPORT
import com.extenre.patches.music.utils.patch.PatchList.SETTINGS_FOR_YOUTUBE_MUSIC
import com.extenre.patches.music.utils.playservice.is_6_39_or_greater
import com.extenre.patches.music.utils.playservice.is_6_42_or_greater
import com.extenre.patches.music.utils.playservice.versionCheckPatch
import com.extenre.patches.music.utils.settings.ResourceUtils.addGmsCorePreference
import com.extenre.patches.music.utils.settings.ResourceUtils.gmsCorePackageName
import com.extenre.patches.shared.extension.Constants.EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR
import com.extenre.patches.shared.extension.Constants.EXTENSION_UTILS_CLASS_DESCRIPTOR
import com.extenre.patches.shared.mainactivity.injectConstructorMethodCall
import com.extenre.patches.shared.mainactivity.injectOnCreateMethodCall
import com.extenre.patches.shared.settings.baseSettingsPatch
import com.extenre.patches.shared.sharedSettingFingerprint
import com.extenre.util.ResourceGroup
import com.extenre.util.Utils.printInfo
import com.extenre.util.copyResources
import com.extenre.util.copyXmlNode
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.removeStringsElements
import com.extenre.util.valueOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.util.MethodUtil
import org.w3c.dom.Element

private const val EXTENSION_ACTIVITY_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/settings/ActivityHook;"
private const val EXTENSION_FRAGMENT_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/settings/preference/ExtenRePreferenceFragment;"
private const val EXTENSION_INITIALIZATION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/InitializationPatch;"

private val settingsBytecodePatch = bytecodePatch(
    name = "settings-bytecode-patch",
    description = "settingsBytecodePatch"
) {
    dependsOn(
        sharedExtensionPatch,
        mainActivityResolvePatch,
        timedLyricsPatch,
        versionCheckPatch,
        baseSettingsPatch,
    )

    execute {

        // region patch for set SharedPrefCategory

        sharedSettingFingerprint.mutableMethodOrThrow().apply {
            val stringIndex = indexOfFirstInstructionOrThrow(Opcode.CONST_STRING)
            val stringRegister = getInstruction<OneRegisterInstruction>(stringIndex).registerA

            replaceInstruction(
                stringIndex,
                "const-string v$stringRegister, \"youtube\""
            )
        }

        // endregion

        // region patch for hook activity

        settingsHeadersFragmentFingerprint.matchOrThrow().let {
            val method = it.method
            val classDef = it.classDef
            val mutableMethod = mutableClassDefBy(classDef.type).methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.apply {
                val targetIndex = it.patternMatch!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $EXTENSION_ACTIVITY_CLASS_DESCRIPTOR->setActivity(Ljava/lang/Object;)V"
                )
            }
        }

        // endregion

        // region patch for hook preference change listener

        preferenceFingerprint.matchOrThrow().let {
            val method = it.method
            val classDef = it.classDef
            val mutableMethod = mutableClassDefBy(classDef.type).methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.apply {
                val targetIndex = it.patternMatch!!.endIndex
                val keyRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerD
                val valueRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerE

                addInstruction(
                    targetIndex,
                    "invoke-static {v$keyRegister, v$valueRegister}, $EXTENSION_FRAGMENT_CLASS_DESCRIPTOR->onPreferenceChanged(Ljava/lang/String;Z)V"
                )
            }
        }

        // endregion

        // region patch for hook dummy Activity for intent

        googleApiActivityFingerprint.mutableMethodOrThrow().apply {
            addInstructionsWithLabels(
                1,
                """
                    invoke-static {p0}, $EXTENSION_ACTIVITY_CLASS_DESCRIPTOR->initialize(Landroid/app/Activity;)Z
                    move-result v0
                    if-eqz v0, :show
                    return-void
                    """,
                ExternalLabel("show", getInstruction(1)),
            )
        }

        // endregion

        // apply the current theme of the settings page
        run {
            val themeUtilsClass = mutableClassDefBy(EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR)
            val method = themeUtilsClass.methods.find { it.name == "setThemeColor" }
                ?: throw PatchException("Method setThemeColor not found in $EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR")
            method.addInstruction(
                0,
                "invoke-static {}, $EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR->updateDarkModeStatus()V"
            )
        }

        injectOnCreateMethodCall(
            EXTENSION_INITIALIZATION_CLASS_DESCRIPTOR,
            "onCreate"
        )
        injectConstructorMethodCall(
            EXTENSION_UTILS_CLASS_DESCRIPTOR,
            "setActivity"
        )

        accountIdentityConstructorFingerprint
            .mutableMethodOrThrow()
            .addInstruction(
                1,
                "invoke-static/range { p7 .. p7 }, $EXTENSION_INITIALIZATION_CLASS_DESCRIPTOR->" +
                        "onLoggedIn(Ljava/lang/String;)V"
            )
    }
}

private const val DEFAULT_ELEMENT = "pref_key_parent_tools"
private const val DEFAULT_LABEL = "ExtenRe"
private const val FALLBACK_ELEMENT = "settings_header_general"
private lateinit var settingsLabel: String

private val SETTINGS_ELEMENTS_MAP = mapOf(
    "Parent settings" to DEFAULT_ELEMENT,
    "General" to FALLBACK_ELEMENT,
    "Playback" to "settings_header_playback",
    "Data saving" to "settings_header_data_saving",
    "Downloads & storage" to "settings_header_downloads_and_storage",
    "Notifications" to "settings_header_notifications",
    "Privacy & data" to "settings_header_privacy_and_location",
    "Recommendations" to "settings_header_recommendations",
    "Paid memberships" to "settings_header_paid_memberships",
    "About YouTube Music" to "settings_header_about_youtube_music",
)

val settingsPatch = resourcePatch(
    name = SETTINGS_FOR_YOUTUBE_MUSIC.key,
    description = "${SETTINGS_FOR_YOUTUBE_MUSIC.title}: ${SETTINGS_FOR_YOUTUBE_MUSIC.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsBytecodePatch,
        versionCheckPatch,
    )

    val insertPosition = stringOption(
        key = "insertPosition",
        default = DEFAULT_ELEMENT,
        values = SETTINGS_ELEMENTS_MAP,
        title = "Insert position",
        description = "The settings menu name that the ExtenRe settings menu should be above.",
        required = true,
    )

    val extenreSettingsLabel = stringOption(
        key = "extenreSettingsLabel",
        default = DEFAULT_LABEL,
        values = mapOf(
            "ExtenRe" to DEFAULT_LABEL,
            "ExtenRe +" to "ExtenRe +",
            "EXTEN+" to "EXTEN+",
            "EXRE" to "EXRE",
            "EXTEN" to "EXTEN",
            "EXTENRE" to "EXTENRE",
            "KITSUNE" to "KITSUNE",
            "NEKO" to "NEKO",
            "RE+" to "RE+",
            "RVX" to "RVX",
            "ReVanced Extended" to "ReVanced Extended",
        ),
        title = "ExtenRe settings label",
        description = "The name of the ExtenRe settings menu.",
        required = true,
    )

    execute {
        /**
         * check patch options
         */
        settingsLabel = extenreSettingsLabel
            .valueOrThrow()

        var insertKey = insertPosition
            .valueOrThrow()

        if (!is_6_39_or_greater && insertKey == DEFAULT_ELEMENT) {
            // 'Parent settings' does not exists in YT Music 6.38.
            // Fallback to 'General'
            insertKey = FALLBACK_ELEMENT
            printInfo("Since this version does not have \"Parent settings\", patch option \"Insert position\" is replaced with \"General\".")
        }

        /**
         * copy arrays, colors and strings
         */
        arrayOf(
            "arrays.xml",
            "colors.xml",
            "strings.xml"
        ).forEach { xmlFile ->
            copyXmlNode("music/settings/host", "values/$xmlFile", "resources")
        }

        arrayOf(
            ResourceGroup(
                "drawable",
                "extenre_settings_toolbar_arrow_left.xml",
            ),
        ).forEach { resourceGroup ->
            copyResources("music/settings", resourceGroup)
        }

        /**
         * hide divider
         */
        val styleFile = get("res/values/styles.xml")

        styleFile.writeText(
            styleFile.readText()
                .replace(
                    "allowDividerAbove\">true",
                    "allowDividerAbove\">false"
                ).replace(
                    "allowDividerBelow\">true",
                    "allowDividerBelow\">false"
                )
        )

        /**
         * Change colors
         */
        document("res/values/colors.xml").use { document ->
            val resourcesNode = document.getElementsByTagName("resources").item(0) as Element
            val children = resourcesNode.childNodes
            for (i in 0 until children.length) {
                val node = children.item(i) as? Element ?: continue

                node.textContent =
                    when (node.getAttribute("name")) {
                        "material_deep_teal_500",
                            -> "@android:color/white"

                        else -> continue
                    }
            }
        }

        ResourceUtils.setContext(this)
        ResourceUtils.addEXTENRESettingsPreference(insertKey)

        ResourceUtils.updatePatchStatus(SETTINGS_FOR_YOUTUBE_MUSIC)

        /**
         * add import export settings
         */
        addPreferenceWithIntent(
            CategoryType.MISC,
            "extenre_settings_import_export"
        )
    }

    finalize {
        /**
         * change EXTENRE settings menu name
         * since it must be invoked after the Translations patch, it must be the last in the order.
         */
        if (settingsLabel != DEFAULT_LABEL) {
            removeStringsElements(
                arrayOf("extenre_settings_title")
            )
            document("res/values/strings.xml").use { document ->
                mapOf(
                    "extenre_settings_title" to settingsLabel
                ).forEach { (k, v) ->
                    val stringElement = document.createElement("string")

                    stringElement.setAttribute("name", k)
                    stringElement.textContent = v

                    document.getElementsByTagName("resources").item(0)
                        .appendChild(stringElement)
                }
            }
        }

        /**
         * add open default app settings
         */
        addPreferenceWithIntent(
            CategoryType.MISC,
            "extenre_default_app_settings"
        )

        if (GMSCORE_SUPPORT.included == true) {
            addGmsCorePreference(
                CategoryType.MISC.value,
                "gms_core_settings",
                gmsCorePackageName,
                "org.microg.gms.ui.SettingsActivity"
            )
        }

        /**
         * add app info setting
         * Html.fromHtml() requires Android 7.0+
         */
        if (is_6_42_or_greater) {
            addPreferenceWithIntent(
                CategoryType.MISC,
                "extenre_app_info"
            )
        }

        /**
         * sort preference
         */
        CategoryType.entries.sorted().forEach {
            ResourceUtils.sortPreferenceCategory(it.value)
        }
    }
}

internal fun addSwitchPreference(
    category: CategoryType,
    key: String,
    defaultValue: String
) = addSwitchPreference(category, key, defaultValue, "")

internal fun addSwitchPreference(
    category: CategoryType,
    key: String,
    defaultValue: String,
    setSummary: Boolean
) = addSwitchPreference(category, key, defaultValue, "", setSummary)

internal fun addSwitchPreference(
    category: CategoryType,
    key: String,
    defaultValue: String,
    dependencyKey: String
) = addSwitchPreference(category, key, defaultValue, dependencyKey, true)

internal fun addSwitchPreference(
    category: CategoryType,
    key: String,
    defaultValue: String,
    dependencyKey: String,
    setSummary: Boolean
) {
    val categoryValue = category.value
    ResourceUtils.addPreferenceCategory(categoryValue)
    ResourceUtils.addSwitchPreference(categoryValue, key, defaultValue, dependencyKey, setSummary)
}

internal fun addPreferenceWithIntent(
    category: CategoryType,
    key: String,
    dependencyKey: String = ""
) {
    val categoryValue = category.value
    ResourceUtils.addPreferenceCategory(categoryValue)
    ResourceUtils.addPreferenceWithIntent(categoryValue, key, dependencyKey)
}