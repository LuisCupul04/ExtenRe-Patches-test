/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.general.formfactor

import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patches.shared.CLIENT_INFO_CLASS_DESCRIPTOR
import com.extenre.patches.shared.createPlayerRequestBodyWithModelFingerprint
import com.extenre.patches.shared.spoof.guide.addClientInfoHook
import com.extenre.patches.shared.spoof.guide.spoofClientGuideEndpointPatch
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.GENERAL_PATH
import com.extenre.patches.youtube.utils.navigation.navigationBarHookPatch
import com.extenre.patches.youtube.utils.patch.PatchList.CHANGE_FORM_FACTOR
import com.extenre.patches.youtube.utils.playertype.playerTypeHookPatch
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.util.fingerprint.definingClassOrThrow
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.getReference
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/ChangeFormFactorPatch;"

@Suppress("unused")
val changeFormFactorPatch = bytecodePatch(
    name = CHANGE_FORM_FACTOR.key,
    description = "${CHANGE_FORM_FACTOR.title}: ${CHANGE_FORM_FACTOR.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        playerTypeHookPatch,
        navigationBarHookPatch,
        spoofClientGuideEndpointPatch,
    )

    execute {

        val formFactorEnumClass = formFactorEnumConstructorFingerprint
            .definingClassOrThrow()

        createPlayerRequestBodyWithModelFingerprint.mutableMethodOrThrow().apply {
            val ordinalIndex = indexOfFirstInstructionOrThrow {
                val reference = getReference<FieldReference>()
                opcode == Opcode.IGET &&
                        reference?.definingClass == formFactorEnumClass &&
                        reference.type == "I"
            }
            val ordinalRegister = getInstruction<TwoRegisterInstruction>(ordinalIndex).registerA

            // This patch changes the 'clientFormFactor' value to a different value
            addInstructions(
                ordinalIndex + 1, """
                    invoke-static {v$ordinalRegister}, $EXTENSION_CLASS_DESCRIPTOR->getFormFactor(I)I
                    move-result v$ordinalRegister
                    """
            )

            val clientFormFactorOrdinalIndex =
                indexOfFirstInstructionOrThrow(ordinalIndex - 1) {
                    val reference = getReference<FieldReference>()
                    opcode == Opcode.IPUT &&
                            reference?.type == "I" &&
                            reference.definingClass == CLIENT_INFO_CLASS_DESCRIPTOR
                }
            val clientFormFactorOrdinalReference =
                getInstruction<ReferenceInstruction>(clientFormFactorOrdinalIndex).reference

            // Changing 'clientFormFactor' in all requests will also affect the navigation bar
            // If 'clientFormFactor' is 'AUTOMOTIVE_FORM_FACTOR', the 'Shorts' button in the navigation bar will change to 'Explore'
            // To fix this side effect, requests to the '/guide' endpoint, which are related to navigation buttons, use the original 'clientFormFactor'
            addClientInfoHook(
                "patch_setClientFormFactor",
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->getFormFactor()I
                    move-result v2
                    iput v2, v1, $clientFormFactorOrdinalReference
                    """
            )
        }

        widthDpUIFingerprint.matchOrThrow().let {
            it.method.apply {
                val index = it.patternMatch!!.startIndex
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index, """
                        invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->getWidthDp(I)I
                        move-result v$register
                        """
                )
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS",
                "SETTINGS: CHANGE_FORM_FACTOR"
            ),
            CHANGE_FORM_FACTOR
        )

        // endregion

    }
}
