/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.reddit.utils.settings

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.extensions.InstructionExtensions.replaceInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.patch.resourcePatch
import com.extenre.patcher.patch.stringOption
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod
import com.extenre.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.reddit.utils.extension.Constants.EXTENSION_PATH
import com.extenre.patches.reddit.utils.extension.sharedExtensionPatch
import com.extenre.patches.reddit.utils.fix.signature.spoofSignaturePatch
import com.extenre.patches.reddit.utils.patch.PatchList
import com.extenre.patches.reddit.utils.patch.PatchList.SETTINGS_FOR_REDDIT
import com.extenre.patches.shared.extension.Constants.EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR
import com.extenre.patches.shared.sharedSettingFingerprint
import com.extenre.util.findMethodOrThrow
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.extenre.util.indexOfFirstStringInstructionOrThrow
import com.extenre.util.valueOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil
import kotlin.io.path.exists

private const val EXTENSION_METHOD_DESCRIPTOR =
    "$EXTENSION_PATH/settings/ActivityHook;->initialize(Landroid/app/Activity;)V"

private lateinit var acknowledgementsLabelBuilderMethod: MutableMethod
private lateinit var settingsStatusLoadMethod: MutableMethod

var is_2024_26_or_greater = false
    private set
var is_2024_41_or_greater = false
    private set
var is_2025_01_or_greater = false
    private set
var is_2025_05_or_greater = false
    private set
var is_2025_06_or_greater = false
    private set

private val settingsBytecodePatch = bytecodePatch(
    name = "settings-Bytecode-Patch",
    description = "settingsBytecodePatch"
) {

    execute {

        /**
         * Set version info
         */
        redditInternalFeaturesFingerprint.mutableMethodOrThrow().apply {
            val versionIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.CONST_STRING
                        && (this as? BuilderInstruction21c)?.reference.toString().startsWith("202")
            }

            val versionNumber =
                getInstruction<BuilderInstruction21c>(versionIndex).reference.toString()
                    .replace(".", "").toInt()

            is_2024_26_or_greater = 2024260 <= versionNumber
            is_2024_41_or_greater = 2024410 <= versionNumber
            is_2025_01_or_greater = 2025010 <= versionNumber
            is_2025_05_or_greater = 2025050 <= versionNumber
            is_2025_06_or_greater = 2025060 <= versionNumber
        }

        /**
         * Set SharedPrefCategory
         */
        sharedSettingFingerprint.mutableMethodOrThrow().apply {
            val stringIndex = indexOfFirstInstructionOrThrow(Opcode.CONST_STRING)
            val stringRegister = getInstruction<OneRegisterInstruction>(stringIndex).registerA

            replaceInstruction(
                stringIndex,
                "const-string v$stringRegister, \"reddit_revanced\""
            )
        }

        /**
         * Replace settings label
         */
        acknowledgementsLabelBuilderMethod =
            acknowledgementsLabelBuilderFingerprint.mutableMethodOrThrow()

        /**
         * Initialize settings activity
         */
        ossLicensesMenuActivityOnCreateFingerprint.matchOrThrow().let {
            it.method.apply {
                val insertIndex = it.patternMatch!!.startIndex + 1

                addInstructions(
                    insertIndex, """
                        invoke-static {p0}, $EXTENSION_METHOD_DESCRIPTOR
                        return-void
                        """
                )
            }
        }

        settingsStatusLoadMethod = settingsStatusLoadFingerprint.mutableMethodOrThrow()

        // Reemplazar findmutableMethodOrThrow por búsqueda manual
        run {
            val method = findMethodOrThrow(EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR) {
                name == "setThemeColor"
            }
            val classDef = classes.find { it.type == EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR }
                ?: throw PatchException("Class not found: $EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR")
            val mutableMethod = proxy(classDef).mutableClass.methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.addInstruction(
                0,
                "invoke-static {}, $EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR->updateDarkModeStatus()V"
            )
        }
    }
}

internal fun updateSettingsLabel(label: String) =
    acknowledgementsLabelBuilderMethod.apply {
        val stringIndex = indexOfFirstStringInstructionOrThrow("onboardingAnalytics")
        val insertIndex = indexOfFirstInstructionReversedOrThrow(stringIndex) {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.name == "getString"
        } + 2
        val insertRegister =
            getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA

        addInstruction(
            insertIndex,
            "const-string v$insertRegister, \"$label\""
        )
    }

internal fun updatePatchStatus(description: String) =
    settingsStatusLoadMethod.addInstruction(
        0,
        "invoke-static {}, $EXTENSION_PATH/settings/SettingsStatus;->$description()V"
    )

internal fun updatePatchStatus(patch: PatchList) {
    patch.included = true
}

internal fun updatePatchStatus(
    description: String,
    patch: PatchList
) {
    updatePatchStatus(description)
    updatePatchStatus(patch)
}

private const val DEFAULT_LABEL = "ExtenRe"

val settingsPatch = resourcePatch(
    name = SETTINGS_FOR_REDDIT.key,
    description = "${SETTINGS_FOR_REDDIT.title}: ${SETTINGS_FOR_REDDIT.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        sharedExtensionPatch,
        settingsBytecodePatch,
        spoofSignaturePatch,
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
        title = "ExtenRe settings menu name",
        description = "The name of the ExtenRe settings menu.",
        required = true
    )

    execute {
        /**
         * Replace settings icon and label
         */
        val settingsLabel = extenreSettingsLabel
            .valueOrThrow()

        arrayOf(
            "preferences.xml",
            "preferences_logged_in.xml",
            "preferences_logged_in_old.xml",
        ).forEach { targetXML ->
            val resDirectory = get("res")
            val targetXml = resDirectory.resolve("xml").resolve(targetXML).toPath()

            if (targetXml.exists()) {
                val preference = get("res/xml/$targetXML")

                preference.writeText(
                    preference.readText()
                        .replace(
                            "\"@drawable/icon_text_post\" android:title=\"@string/label_acknowledgements\"",
                            "\"@drawable/icon_beta_planet\" android:title=\"$settingsLabel\""
                        )
                )
            }
        }

        updateSettingsLabel(settingsLabel)
        updatePatchStatus(SETTINGS_FOR_REDDIT)
    }
}
