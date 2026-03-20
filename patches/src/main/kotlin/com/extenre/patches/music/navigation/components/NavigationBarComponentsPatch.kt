/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.music.navigation.components

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.patch.resourcePatch
import com.extenre.patches.music.general.startpage.changeStartPagePatch
import com.extenre.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.music.utils.extension.Constants.NAVIGATION_CLASS_DESCRIPTOR
import com.extenre.patches.music.utils.patch.PatchList.NAVIGATION_BAR_COMPONENTS
import com.extenre.patches.music.utils.playservice.is_6_27_or_greater
import com.extenre.patches.music.utils.playservice.is_8_29_or_greater
import com.extenre.patches.music.utils.playservice.versionCheckPatch
import com.extenre.patches.music.utils.resourceid.colorGrey
import com.extenre.patches.music.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.music.utils.resourceid.text1
import com.extenre.patches.music.utils.resourceid.ytFillSamples
import com.extenre.patches.music.utils.resourceid.ytFillYouTubeMusic
import com.extenre.patches.music.utils.resourceid.ytOutlineSamples
import com.extenre.patches.music.utils.resourceid.ytOutlineYouTubeMusic
import com.extenre.patches.music.utils.settings.CategoryType
import com.extenre.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import com.extenre.patches.music.utils.settings.addPreferenceWithIntent
import com.extenre.patches.music.utils.settings.addSwitchPreference
import com.extenre.patches.music.utils.settings.settingsPatch
import com.extenre.util.REGISTER_TEMPLATE_REPLACEMENT
import com.extenre.util.Utils.printWarn
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.extenre.util.indexOfFirstLiteralInstructionOrThrow
import com.extenre.util.replaceLiteralInstructionCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val FLAG = "android:layout_weight"
private const val RESOURCE_FILE_PATH = "res/layout/image_with_text_tab.xml"

private val navigationBarComponentsResourcePatch = resourcePatch(
    name = "navigation-bar-components-resource-patch",
    description = "navigationBarComponentsResourcePatch"
) {
    execute {
        document(RESOURCE_FILE_PATH).use { document ->
            with(document.getElementsByTagName("ImageView").item(0)) {
                if (attributes.getNamedItem(FLAG) != null)
                    return@with

                document.createAttribute(FLAG)
                    .apply { value = "0.5" }
                    .let(attributes::setNamedItem)
            }
        }
    }
}

@Suppress("unused")
val navigationBarComponentsPatch = bytecodePatch(
    name = NAVIGATION_BAR_COMPONENTS.key,
    description = "${NAVIGATION_BAR_COMPONENTS.title}: ${NAVIGATION_BAR_COMPONENTS.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        changeStartPagePatch,
        navigationBarComponentsResourcePatch,
        sharedResourceIdPatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {
        /**
         * Enable custom navigation bar color
         */
        tabLayoutFingerprint.mutableMethodOrThrow().apply {
            val constIndex = indexOfFirstLiteralInstructionOrThrow(colorGrey)
            val insertIndex = indexOfFirstInstructionOrThrow(constIndex) {
                opcode == Opcode.INVOKE_VIRTUAL
                        && getReference<MethodReference>()?.name == "setBackgroundColor"
            }
            val insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD

            addInstructions(
                insertIndex, """
                    invoke-static {}, $NAVIGATION_CLASS_DESCRIPTOR->enableCustomNavigationBarColor()I
                    move-result v$insertRegister
                    """
            )
        }

        /**
         * Hide navigation labels
         */
        tabLayoutTextFingerprint.mutableMethodOrThrow().apply {
            val constIndex =
                indexOfFirstLiteralInstructionOrThrow(text1)
            val targetIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.CHECK_CAST)
            val targetParameter = getInstruction<ReferenceInstruction>(targetIndex).reference
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            if (!targetParameter.toString().endsWith("Landroid/widget/TextView;"))
                throw PatchException("Method signature parameter did not match: $targetParameter")

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, $NAVIGATION_CLASS_DESCRIPTOR->hideNavigationLabel(Landroid/widget/TextView;)V"
            )
        }

        /**
         * Hide navigation bar & buttons
         */
        val tabLayoutMatch = tabLayoutTextFingerprint.matchOrThrow()
        val tabLayoutMethod = tabLayoutMatch.method
        val tabLayoutClassDef = tabLayoutMatch.classDef
        val tabLayoutMutableMethod = proxy(tabLayoutClassDef).mutableClass.methods.first {
            MethodUtil.methodSignaturesMatch(it, tabLayoutMethod)
        }
        tabLayoutMutableMethod.apply {
            val mapIndex = indexOfMapInstruction(this)
            val browseIdRegister =
                getInstruction<FiveRegisterInstruction>(mapIndex).registerD
            val browseIdIndex = indexOfFirstInstructionReversedOrThrow(mapIndex + 1) {
                opcode == Opcode.IGET_OBJECT &&
                        getReference<FieldReference>()?.type == "Ljava/lang/String;" &&
                        (this as TwoRegisterInstruction).registerA == browseIdRegister
            }
            val browseIdClassRegister =
                getInstruction<TwoRegisterInstruction>(browseIdIndex).registerB
            val browseIdFieldName =
                (getInstruction<ReferenceInstruction>(browseIdIndex).reference as FieldReference).name

            val enumIndex = tabLayoutMatch.patternMatch!!.startIndex + 3
            val enumRegister = getInstruction<OneRegisterInstruction>(enumIndex).registerA
            val insertEnumIndex = indexOfFirstInstructionOrThrow(Opcode.AND_INT_LIT8) - 2

            val pivotTabIndex = indexOfGetVisibilityInstruction(this)
            val pivotTabRegister =
                getInstruction<FiveRegisterInstruction>(pivotTabIndex).registerC

            val spannedIndex = indexOfSetTextInstruction(this)
            val spannedRegister =
                getInstruction<FiveRegisterInstruction>(spannedIndex).registerD

            addInstruction(
                pivotTabIndex,
                "invoke-static {v$pivotTabRegister}, $NAVIGATION_CLASS_DESCRIPTOR->hideNavigationButton(Landroid/view/View;)V"
            )

            addInstructions(
                mapIndex, """
                        const-string v$enumRegister, "$browseIdFieldName"
                        invoke-static {v$browseIdClassRegister, v$browseIdRegister, v$enumRegister}, $NAVIGATION_CLASS_DESCRIPTOR->replaceBrowseId(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$browseIdRegister
                        """
            )

            addInstructions(
                spannedIndex, """
                        invoke-static {v$spannedRegister}, $NAVIGATION_CLASS_DESCRIPTOR->replaceNavigationLabel(Landroid/text/Spanned;)Landroid/text/Spanned;
                        move-result-object v$spannedRegister
                        """
            )

            addInstruction(
                insertEnumIndex,
                "invoke-static {v$enumRegister}, $NAVIGATION_CLASS_DESCRIPTOR->setLastAppNavigationEnum(Ljava/lang/Enum;)V"
            )
        }

        val smaliInstruction = """
            invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $NAVIGATION_CLASS_DESCRIPTOR->replaceNavigationIcon(I)I
            move-result v$REGISTER_TEMPLATE_REPLACEMENT
            """

        arrayOf(
            ytFillSamples,
            ytFillYouTubeMusic,
            ytOutlineSamples,
            ytOutlineYouTubeMusic,
        ).forEach { literal ->
            replaceLiteralInstructionCall(literal, smaliInstruction)
        }

        addSwitchPreference(
            CategoryType.NAVIGATION,
            "extenre_enable_custom_navigation_bar_color",
            "false"
        )
        addPreferenceWithIntent(
            CategoryType.NAVIGATION,
            "extenre_custom_navigation_bar_color_value",
            "extenre_enable_custom_navigation_bar_color"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "extenre_hide_navigation_home_button",
            "false"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "extenre_hide_navigation_samples_button",
            "false"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "extenre_hide_navigation_explore_button",
            "false"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "extenre_hide_navigation_library_button",
            "false"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "extenre_hide_navigation_upgrade_button",
            "true"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "extenre_hide_navigation_bar",
            "false"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "extenre_hide_navigation_label",
            "false"
        )
        if (is_6_27_or_greater && !is_8_29_or_greater) {
            addSwitchPreference(
                CategoryType.NAVIGATION,
                "extenre_replace_navigation_samples_button",
                "false"
            )
        } else {
            printWarn("\"Replace Samples button\" is not supported in this version. Use YouTube Music 6.29.59 - 8.28.54.")
        }
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "extenre_replace_navigation_upgrade_button",
            "false"
        )
        addPreferenceWithIntent(
            CategoryType.NAVIGATION,
            "extenre_replace_navigation_button_about"
        )

        updatePatchStatus(NAVIGATION_BAR_COMPONENTS)

    }
}
