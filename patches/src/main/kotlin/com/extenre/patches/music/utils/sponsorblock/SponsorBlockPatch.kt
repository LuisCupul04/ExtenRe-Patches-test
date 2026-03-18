/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.utils.sponsorblock

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.patch.resourcePatch
import com.extenre.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.music.utils.extension.Constants.EXTENSION_PATH
import com.extenre.patches.music.utils.patch.PatchList.SPONSORBLOCK
import com.extenre.patches.music.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.music.utils.settings.CategoryType
import com.extenre.patches.music.utils.settings.ResourceUtils.ACTIVITY_HOOK_TARGET_CLASS
import com.extenre.patches.music.utils.settings.ResourceUtils.PREFERENCE_CATEGORY_TAG_NAME
import com.extenre.patches.music.utils.settings.ResourceUtils.PREFERENCE_SCREEN_TAG_NAME
import com.extenre.patches.music.utils.settings.ResourceUtils.SETTINGS_HEADER_PATH
import com.extenre.patches.music.utils.settings.ResourceUtils.SWITCH_PREFERENCE_TAG_NAME
import com.extenre.patches.music.utils.settings.ResourceUtils.addPreferenceCategory
import com.extenre.patches.music.utils.settings.ResourceUtils.musicPackageName
import com.extenre.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import com.extenre.patches.music.utils.settings.settingsPatch
import com.extenre.patches.music.video.information.videoIdHook
import com.extenre.patches.music.video.information.videoInformationPatch
import com.extenre.patches.music.video.information.videoTimeHook
import com.extenre.util.adoptChild
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/sponsorblock/SegmentPlaybackController;"

private val sponsorBlockBytecodePatch = bytecodePatch(
    name = "sponsor-Block-Bytecode-Patch",
    description = "sponsorBlockBytecodePatch"
) {
    dependsOn(
        sharedResourceIdPatch,
        videoInformationPatch
    )

    execute {

        /**
         * Hook the video time methods & Initialize the player controller
         */
        videoTimeHook(EXTENSION_CLASS_DESCRIPTOR, "setVideoTime")

        /**
         * Responsible for seekbar in fullscreen
         */
        var rectangleFieldName =
            with(rectangleFieldInvalidatorFingerprint.mutableMethodOrThrow(seekBarConstructorFingerprint)) {
                val invalidateIndex = indexOfInvalidateInstruction(this)
                val rectangleIndex =
                    indexOfFirstInstructionReversedOrThrow(invalidateIndex + 1) {
                        getReference<FieldReference>()?.type == "Landroid/graphics/Rect;"
                    }
                val rectangleReference =
                    getInstruction<ReferenceInstruction>(rectangleIndex).reference

                (rectangleReference as FieldReference).name
            }

        seekbarOnDrawFingerprint.mutableMethodOrThrow(seekBarConstructorFingerprint).apply {
            // Initialize seekbar method
            addInstructions(
                0, """
                    move-object/from16 v0, p0
                    const-string v1, "$rectangleFieldName"
                    invoke-static {v0, v1}, $EXTENSION_CLASS_DESCRIPTOR->setSponsorBarRect(Ljava/lang/Object;Ljava/lang/String;)V
                    """
            )

            // Set seekbar thickness
            val roundIndex = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.name == "round"
            } + 1
            val roundRegister = getInstruction<OneRegisterInstruction>(roundIndex).registerA
            addInstruction(
                roundIndex + 1,
                "invoke-static {v$roundRegister}, " +
                        "$EXTENSION_CLASS_DESCRIPTOR->setSponsorBarThickness(I)V"
            )

            // Draw segment
            val drawCircleIndex = indexOfFirstInstructionReversedOrThrow {
                getReference<MethodReference>()?.name == "drawCircle"
            }
            val drawCircleInstruction = getInstruction<FiveRegisterInstruction>(drawCircleIndex)
            addInstruction(
                drawCircleIndex,
                "invoke-static {v${drawCircleInstruction.registerC}, v${drawCircleInstruction.registerE}}, " +
                        "$EXTENSION_CLASS_DESCRIPTOR->drawSponsorTimeBars(Landroid/graphics/Canvas;F)V"
            )
        }


        /**
         * Responsible for seekbar in player
         */
        rectangleFieldName =
            musicPlaybackControlsTimeBarOnMeasureFingerprint.matchOrThrow().let {
                with(it.method) {
                    val rectangleIndex =
                        indexOfFirstInstructionReversedOrThrow(it.patternMatch!!.endIndex) {
                            opcode == Opcode.IGET_OBJECT &&
                                    getReference<FieldReference>()?.type == "Landroid/graphics/Rect;"
                        }
                    val rectangleReference =
                        getInstruction<ReferenceInstruction>(rectangleIndex).reference
                    (rectangleReference as FieldReference).name
                }
            }

        musicPlaybackControlsTimeBarDrawFingerprint.mutableMethodOrThrow().apply {
            // Initialize seekbar method
            addInstructions(
                1, """
                    move-object/from16 v0, p0
                    const-string v1, "$rectangleFieldName"
                    invoke-static {v0, v1}, $EXTENSION_CLASS_DESCRIPTOR->setSponsorBarRect(Ljava/lang/Object;Ljava/lang/String;)V
                    """
            )

            // Draw segment
            val drawCircleIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.name == "drawCircle"
            }
            val drawCircleInstruction = getInstruction<FiveRegisterInstruction>(drawCircleIndex)
            addInstruction(
                drawCircleIndex,
                "invoke-static {v${drawCircleInstruction.registerC}, v${drawCircleInstruction.registerE}}, " +
                        "$EXTENSION_CLASS_DESCRIPTOR->drawSponsorTimeBars(Landroid/graphics/Canvas;F)V"
            )
        }

        /**
         * Set current video id
         */
        videoIdHook("$EXTENSION_CLASS_DESCRIPTOR->setVideoId(Ljava/lang/String;)V")
    }
}

private const val SEGMENTS_CATEGORY_KEY = "sb_diff_segments"
private const val ABOUT_CATEGORY_KEY = "sb_about"

private val SPONSOR_BLOCK_CATEGORY = CategoryType.SPONSOR_BLOCK.value

@Suppress("unused")
val sponsorBlockPatch = resourcePatch(
    name = SPONSORBLOCK.key,
    description = "${SPONSORBLOCK.title}: ${SPONSORBLOCK.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        sponsorBlockBytecodePatch,
        settingsPatch,
    )

    execute {
        fun addSwitchPreference(
            category: String,
            key: String,
            defaultValue: String,
            dependencyKey: String
        ) {
            document(SETTINGS_HEADER_PATH).use { document ->
                val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
                List(tags.length) { tags.item(it) as Element }
                    .filter {
                        it.getAttribute("android:key")
                            .contains("extenre_preference_screen_$category")
                    }
                    .forEach {
                        it.adoptChild(SWITCH_PREFERENCE_TAG_NAME) {
                            setAttribute("android:title", "@string/extenre_$key")
                            setAttribute("android:summary", "@string/extenre_$key" + "_sum")
                            setAttribute("android:key", key)
                            setAttribute("android:defaultValue", defaultValue)
                            if (dependencyKey != "") {
                                setAttribute("android:dependency", dependencyKey)
                            }
                        }
                    }
            }
        }

        fun addSwitchPreference(
            category: String,
            key: String,
            defaultValue: String
        ) = addSwitchPreference(category, key, defaultValue, "")

        fun addPreferenceWithIntent(
            category: String,
            key: String,
            dependencyKey: String
        ) {
            document(SETTINGS_HEADER_PATH).use { document ->
                val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
                List(tags.length) { tags.item(it) as Element }
                    .filter {
                        it.getAttribute("android:key")
                            .contains("extenre_preference_screen_$category")
                    }
                    .forEach {
                        it.adoptChild("Preference") {
                            setAttribute("android:title", "@string/extenre_$key")
                            setAttribute("android:summary", "@string/extenre_$key" + "_sum")
                            setAttribute("android:key", key)
                            setAttribute("android:dependency", dependencyKey)
                            this.adoptChild("intent") {
                                setAttribute("android:targetPackage", musicPackageName)
                                setAttribute("android:data", key)
                                setAttribute(
                                    "android:targetClass",
                                    ACTIVITY_HOOK_TARGET_CLASS
                                )
                            }
                        }
                    }
            }
        }

        fun addPreferenceCategoryUnderPreferenceScreen(
            preferenceScreenKey: String,
            category: String
        ) {
            document(SETTINGS_HEADER_PATH).use { document ->
                val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
                List(tags.length) { tags.item(it) as Element }
                    .filter { it.getAttribute("android:key").contains(preferenceScreenKey) }
                    .forEach {
                        it.adoptChild(PREFERENCE_CATEGORY_TAG_NAME) {
                            setAttribute("android:title", "@string/extenre_$category")
                            setAttribute("android:key", category)
                        }
                    }
            }
        }

        fun addSegmentsPreference(
            key: String,
            dependencyKey: String
        ) {
            document(SETTINGS_HEADER_PATH).use { document ->
                val tags = document.getElementsByTagName(PREFERENCE_CATEGORY_TAG_NAME)
                List(tags.length) { tags.item(it) as Element }
                    .filter { it.getAttribute("android:key") == SEGMENTS_CATEGORY_KEY }
                    .forEach {
                        it.adoptChild("Preference") {
                            setAttribute("android:title", "@string/extenre_$key")
                            setAttribute("android:summary", "@string/extenre_$key" + "_sum")
                            setAttribute("android:key", key)
                            setAttribute("android:dependency", dependencyKey)
                            this.adoptChild("intent") {
                                setAttribute("android:targetPackage", musicPackageName)
                                setAttribute("android:data", key)
                                setAttribute(
                                    "android:targetClass",
                                    ACTIVITY_HOOK_TARGET_CLASS
                                )
                            }
                        }
                    }
            }
        }

        fun addAboutPreference(
            key: String,
            data: String
        ) {
            document(SETTINGS_HEADER_PATH).use { document ->
                val tags = document.getElementsByTagName(PREFERENCE_CATEGORY_TAG_NAME)
                List(tags.length) { tags.item(it) as Element }
                    .filter { it.getAttribute("android:key") == ABOUT_CATEGORY_KEY }
                    .forEach {
                        it.adoptChild("Preference") {
                            setAttribute("android:title", "@string/extenre_$key")
                            setAttribute("android:summary", "@string/extenre_$key" + "_sum")
                            setAttribute("android:key", key)
                            this.adoptChild("intent") {
                                setAttribute("android:action", "android.intent.action.VIEW")
                                setAttribute("android:data", data)
                            }
                        }
                    }
            }
        }

        addPreferenceCategory(SPONSOR_BLOCK_CATEGORY)

        addSwitchPreference(
            SPONSOR_BLOCK_CATEGORY,
            "sb_enabled",
            "true"
        )
        addSwitchPreference(
            SPONSOR_BLOCK_CATEGORY,
            "sb_toast_on_skip",
            "true",
            "sb_enabled"
        )
        addSwitchPreference(
            SPONSOR_BLOCK_CATEGORY,
            "sb_toast_on_connection_error",
            "true",
            "sb_enabled"
        )
        addPreferenceWithIntent(
            SPONSOR_BLOCK_CATEGORY,
            "sb_api_url",
            "sb_enabled"
        )

        addPreferenceCategoryUnderPreferenceScreen(
            SPONSOR_BLOCK_CATEGORY,
            SEGMENTS_CATEGORY_KEY
        )

        addSegmentsPreference(
            "sb_segments_sponsor",
            "sb_enabled"
        )
        addSegmentsPreference(
            "sb_segments_selfpromo",
            "sb_enabled"
        )
        addSegmentsPreference(
            "sb_segments_interaction",
            "sb_enabled"
        )
        addSegmentsPreference(
            "sb_segments_intro",
            "sb_enabled"
        )
        addSegmentsPreference(
            "sb_segments_outro",
            "sb_enabled"
        )
        addSegmentsPreference(
            "sb_segments_preview",
            "sb_enabled"
        )
        addSegmentsPreference(
            "sb_segments_hook",
            "sb_enabled"
        )
        addSegmentsPreference(
            "sb_segments_filler",
            "sb_enabled"
        )
        addSegmentsPreference(
            "sb_segments_nomusic",
            "sb_enabled"
        )

        addPreferenceCategoryUnderPreferenceScreen(
            CategoryType.SPONSOR_BLOCK.value,
            ABOUT_CATEGORY_KEY
        )

        addAboutPreference(
            "sb_about_api",
            "https://sponsor.ajay.app"
        )

        get(SETTINGS_HEADER_PATH).apply {
            writeText(
                readText()
                    .replace(
                        "\"sb_segments_nomusic",
                        "\"sb_segments_music_offtopic"
                    )
            )
        }

        updatePatchStatus(SPONSORBLOCK)

    }
}

