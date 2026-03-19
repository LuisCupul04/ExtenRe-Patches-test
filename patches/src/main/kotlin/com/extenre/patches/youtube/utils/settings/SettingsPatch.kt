/*
 * Copyright (C) 2026 LuisCupul04
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2022 ReVanced LLC
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.utils.settings

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.BytecodePatchContext
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.patch.resourcePatch
import com.extenre.patcher.patch.stringOption
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod
import com.extenre.patches.shared.extension.Constants.EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR
import com.extenre.patches.shared.extension.Constants.EXTENSION_UTILS_CLASS_DESCRIPTOR
import com.extenre.patches.shared.mainactivity.injectConstructorMethodCall
import com.extenre.patches.shared.mainactivity.injectOnCreateMethodCall
import com.extenre.patches.shared.settings.baseSettingsPatch
import com.extenre.patches.youtube.utils.cairoFragmentConfigFingerprint
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.extension.Constants.UTILS_PATH
import com.extenre.patches.youtube.utils.extension.sharedExtensionPatch
import com.extenre.patches.youtube.utils.fix.attributes.themeAttributesPatch
import com.extenre.patches.youtube.utils.fix.playbackspeed.playbackSpeedWhilePlayingPatch
import com.extenre.patches.youtube.utils.fix.splash.darkModeSplashScreenPatch
import com.extenre.patches.youtube.utils.mainactivity.mainActivityResolvePatch
import com.extenre.patches.youtube.utils.patch.PatchList.SETTINGS_FOR_YOUTUBE
import com.extenre.patches.youtube.utils.playservice.is_19_16_or_greater
import com.extenre.patches.youtube.utils.playservice.is_19_34_or_greater
import com.extenre.patches.youtube.utils.playservice.versionCheckPatch
import com.extenre.patches.youtube.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.youtube.utils.settings.ResourceUtils.YOUTUBE_SETTINGS_PATH
import com.extenre.patches.youtube.utils.settingsFragmentSyntheticFingerprint
import com.extenre.util.FilesCompat
import com.extenre.util.ResourceGroup
import com.extenre.util.Utils.printWarn
import com.extenre.util.addInstructionsAtControlFlowLabel
import com.extenre.util.className
import com.extenre.util.copyResources
import com.extenre.util.copyXmlNode
import com.extenre.util.findFreeRegister
import com.extenre.util.findInstructionIndicesReversedOrThrow
import com.extenre.util.findMethodOrThrow
import com.extenre.util.fingerprint.definingClassOrThrow
import com.extenre.util.fingerprint.methodCall
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.fingerprint.mutableClassOrThrow
import com.extenre.util.getReference
import com.extenre.util.hookClassHierarchy
import com.extenre.util.indexOfFirstInstruction
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.insertNode
import com.extenre.util.removeStringsElements
import com.extenre.util.returnEarly
import com.extenre.util.valueOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil
import org.w3c.dom.Element
import java.nio.file.Files

private const val EXTENSION_INITIALIZATION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/InitializationPatch;"

private lateinit var bytecodeContext: BytecodePatchContext

internal fun getBytecodeContext() = bytecodeContext

internal var cairoFragmentDisabled = false
private var targetActivityClassName = ""

private val settingsBytecodePatch = bytecodePatch(
    name = "settings-BytecodeP-atch",
    description = "settingsBytecodePatch"
) {
    dependsOn(
        sharedExtensionPatch,
        sharedResourceIdPatch,
        mainActivityResolvePatch,
        versionCheckPatch,
        baseSettingsPatch,
    )

    execute {
        bytecodeContext = this

        // region fix cairo fragment.

        /**
         * Disable Cairo fragment settings.
         * 1. Fix - When spoofing the app version to 19.20 or earlier, the app crashes or the Notifications tab is inaccessible.
         * 2. Fix - Preference 'Playback' is hidden.
         * 3. Some settings that were in Preference 'General' are moved to Preference 'Playback'.
         */
        // Cairo fragment have been widely rolled out in YouTube 19.34+.
        if (is_19_34_or_greater) {
            // Instead of disabling all Cairo fragment configs,
            // Just disable 'Load Cairo fragment xml' and 'Set style to Cairo preference'.
            fun MutableMethod.disableCairoFragmentConfig() {
                val cairoFragmentConfigMethodCall = cairoFragmentConfigFingerprint
                    .methodCall()
                val insertIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.toString() == cairoFragmentConfigMethodCall
                } + 2
                val insertRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA

                addInstruction(insertIndex, "const/4 v$insertRegister, 0x0")
            }

            try {
                arrayOf(
                    // Load cairo fragment xml.
                    settingsFragmentSyntheticFingerprint
                        .mutableMethodOrThrow(),
                    // Set style to cairo preference.
                    settingsFragmentStylePrimaryFingerprint
                        .mutableMethodOrThrow(),
                    settingsFragmentStyleSecondaryFingerprint
                        .mutableMethodOrThrow(settingsFragmentStylePrimaryFingerprint),
                ).forEach { method ->
                    method.disableCairoFragmentConfig()
                }
                cairoFragmentDisabled = true
            } catch (_: Exception) {
                cairoFragmentConfigFingerprint
                    .mutableMethodOrThrow()
                    .returnEarly()

                printWarn("Failed to restore 'Playback' settings. 'Autoplay next video' setting may not appear in the YouTube settings.")
            }
        }

        // endregion.

        val hostAbstractActivityClass = baseHostActivityOnCreateFingerprint.mutableClassOrThrow()
        val hostActivityClass = youtubeHostActivityOnCreateFingerprint.mutableClassOrThrow()
        val targetActivityClass = licenseMenuActivityOnCreateFingerprint.mutableClassOrThrow()

        hookClassHierarchy(
            hostActivityClass,
            targetActivityClass,
            hostAbstractActivityClass,
        )

        targetActivityClass.methods.forEach { method ->
            method.apply {
                if (!MethodUtil.isConstructor(method) && returnType == "V") {
                    val insertIndex =
                        indexOfFirstInstruction(Opcode.INVOKE_SUPER) + 1
                    if (insertIndex > 0) {
                        val freeRegister = findFreeRegister(insertIndex)

                        addInstructionsWithLabels(
                            insertIndex, """
                                invoke-virtual {p0}, ${hostAbstractActivityClass.type}->isInitialized()Z
                                move-result v$freeRegister
                                if-eqz v$freeRegister, :ignore
                                return-void
                                :ignore
                                nop
                                """
                        )
                    }
                }
            }
        }

        targetActivityClassName = targetActivityClass.type.className
        // Reemplazar findmutableMethodOrThrow por búsqueda manual
        run {
            val method = findMethodOrThrow(PATCH_STATUS_CLASS_DESCRIPTOR) {
                name == "TargetActivityClass"
            }
            val classDef = classes.find { it.type == PATCH_STATUS_CLASS_DESCRIPTOR }
                ?: throw PatchException("Class not found: $PATCH_STATUS_CLASS_DESCRIPTOR")
            val mutableMethod = proxy(classDef).mutableClass.methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.returnEarly(targetActivityClassName)
        }

        // apply the current theme of the settings page
        themeSetterSystemFingerprint.mutableMethodOrThrow().apply {
            findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructionsAtControlFlowLabel(
                    index,
                    "invoke-static { v$register }, $EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR->updateLightDarkModeStatus(Ljava/lang/Enum;)V"
                )
            }
        }

        if (is_19_16_or_greater) {
            val userInterfaceThemeEnum = userInterfaceThemeEnumFingerprint
                .definingClassOrThrow()

            clientContextBodyBuilderFingerprint.mutableMethodOrThrow().apply {
                findInstructionIndicesReversedOrThrow {
                    val fieldReference = getReference<FieldReference>()
                    opcode == Opcode.IGET &&
                            fieldReference?.definingClass == userInterfaceThemeEnum &&
                            fieldReference.type == "I"
                }.forEach { index ->
                    val register = getInstruction<TwoRegisterInstruction>(index).registerA

                    addInstruction(
                        index + 1,
                        "invoke-static { v$register }, $EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR->updateLightDarkModeStatus(I)V",
                    )
                }
            }
        }

        injectOnCreateMethodCall(
            EXTENSION_INITIALIZATION_CLASS_DESCRIPTOR,
            "onCreate"
        )
        injectConstructorMethodCall(
            EXTENSION_UTILS_CLASS_DESCRIPTOR,
            "setActivity"
        )
    }
}

private const val DEFAULT_ELEMENT = "@string/about_key"
private const val DEFAULT_LABEL = "ExtenRe"

private val SETTINGS_ELEMENTS_MAP = mapOf(
    "About" to DEFAULT_ELEMENT,
    "Parent settings" to "@string/parent_tools_key",
    "General" to "@string/general_key",
    "Account" to "@string/account_switcher_key",
    "Data saving" to "@string/data_saving_settings_key",
    "Autoplay" to "@string/auto_play_key",
    "Video quality preferences" to "@string/video_quality_settings_key",
    "Background" to "@string/offline_key",
    "Watch on TV" to "@string/pair_with_tv_key",
    "Manage all history" to "@string/history_key",
    "Your data in YouTube" to "@string/your_data_key",
    "Privacy" to "@string/privacy_key",
    "History & privacy" to "@string/privacy_key",
    "Try experimental new features" to "@string/premium_early_access_browse_page_key",
    "Purchases and memberships" to "@string/subscription_product_setting_key",
    "Billing & payments" to "@string/billing_and_payment_key",
    "Billing and payments" to "@string/billing_and_payment_key",
    "Notifications" to "@string/notification_key",
    "Connected apps" to "@string/connected_accounts_browse_page_key",
    "Live chat" to "@string/live_chat_key",
    "Captions" to "@string/captions_key",
    "Accessibility" to "@string/accessibility_settings_key",
    "About" to "@string/about_key"
)

private lateinit var settingsLabel: String

val settingsPatch = resourcePatch(
    name = SETTINGS_FOR_YOUTUBE.key,
    description = "${SETTINGS_FOR_YOUTUBE.title}: ${SETTINGS_FOR_YOUTUBE.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsBytecodePatch,
        darkModeSplashScreenPatch,
        playbackSpeedWhilePlayingPatch,
        themeAttributesPatch,
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
            "ReVanced Extended" to "ReVanced Extended"
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

        val insertKey = insertPosition
            .valueOrThrow()

        ResourceUtils.setContext(this)

        /**
         * remove strings duplicated with RVX resources
         *
         * YouTube does not provide translations for these strings.
         * That's why it's been added to RVX resources.
         * This string also exists in RVX resources, so it must be removed to avoid being duplicated.
         */
        removeStringsElements(
            arrayOf("values"),
            arrayOf(
                "accessibility_settings_edu_opt_in_text",
                "accessibility_settings_edu_opt_out_text"
            )
        )

        /**
         * copy arrays, strings and preference
         */
        arrayOf(
            "arrays.xml",
            "dimens.xml",
            "strings.xml",
            "styles.xml"
        ).forEach { xmlFile ->
            copyXmlNode("youtube/settings/host", "values/$xmlFile", "resources")
        }

        val valuesV21Directory = get("res").resolve("values-v21")
        if (!valuesV21Directory.isDirectory)
            Files.createDirectories(valuesV21Directory.toPath())

        copyResources(
            "youtube/settings",
            ResourceGroup(
                "values-v21",
                "strings.xml"
            )
        )

        arrayOf(
            ResourceGroup(
                "drawable",
                "extenre_settings_arrow_time.xml",
                "extenre_settings_cursor.xml",
                "extenre_settings_custom_checkmark.xml",
                "extenre_settings_icon.xml",
                "extenre_settings_rounded_corners_background.xml",
                "extenre_settings_search_icon.xml",
                "extenre_settings_search_remove.xml",
                "extenre_settings_toolbar_arrow_left.xml",
            ),
            ResourceGroup(
                "layout",
                "extenre_color_dot_widget.xml",
                "extenre_color_picker.xml",
                "extenre_custom_list_item_checked.xml",
                "extenre_preference_search_history_item.xml",
                "extenre_preference_search_history_screen.xml",
                "extenre_preference_search_no_result.xml",
                "extenre_preference_search_result_color.xml",
                "extenre_preference_search_result_group_header.xml",
                "extenre_preference_search_result_list.xml",
                "extenre_preference_search_result_regular.xml",
                "extenre_preference_search_result_switch.xml",
                "extenre_settings_preferences_category.xml",
                "extenre_settings_with_toolbar.xml",
            ),
            ResourceGroup(
                "menu",
                "extenre_search_menu.xml",
            ),
            ResourceGroup(
                "xml",
                "extenre_prefs.xml",
            )
        ).forEach { resourceGroup ->
            copyResources("youtube/settings", resourceGroup)
        }

        /**
         * initialize ExtenRe Settings
         */
        ResourceUtils.addPreferenceFragment(
            "extenre_settings",
            insertKey,
            targetActivityClassName,
        )

        /**
         * remove ReVanced Extended Settings divider
         */
        document("res/values/styles.xml").use { document ->
            val themeNames = arrayOf("Theme.YouTube.Settings", "Theme.YouTube.Settings.Dark")
            with(document) {
                val resourcesNode = documentElement
                val childNodes = resourcesNode.childNodes

                for (i in 0 until childNodes.length) {
                    val node = childNodes.item(i) as? Element ?: continue

                    if (node.getAttribute("name") in themeNames) {
                        val newElement = createElement("item").apply {
                            setAttribute("name", "android:listDivider")
                            appendChild(createTextNode("@null"))
                        }
                        node.appendChild(newElement)
                    }
                }
            }
        }
    }

    finalize {
        /**
         * change ExtenRe settings menu name
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
         * Disable Cairo fragment settings.
         */
        if (cairoFragmentDisabled) {
            /**
             * If the app version is spoofed to 19.30 or earlier due to the Spoof app version patch,
             * the 'Playback' setting will be broken.
             * If the app version is spoofed, the previous fragment must be used.
             */
            val xmlDirectory = get("res").resolve("xml")
            FilesCompat.copy(
                xmlDirectory.resolve("settings_fragment.xml"),
                xmlDirectory.resolve("settings_fragment_legacy.xml")
            )

            /**
             * The Preference key for 'Playback' is '@string/playback_key'.
             * Copy the node to add the Preference 'Playback' to the legacy settings fragment.
             */
            document(YOUTUBE_SETTINGS_PATH).use { document ->
                val tags = document.getElementsByTagName("Preference")
                List(tags.length) { tags.item(it) as Element }
                    .find { it.getAttribute("android:key") == "@string/auto_play_key" }
                    ?.let { node ->
                        node.insertNode("Preference", node) {
                            for (index in 0 until node.attributes.length) {
                                with(node.attributes.item(index)) {
                                    setAttribute(nodeName, nodeValue)
                                }
                            }
                            setAttribute("android:key", "@string/playback_key")
                        }
                    }
            }
        }
    }
}
