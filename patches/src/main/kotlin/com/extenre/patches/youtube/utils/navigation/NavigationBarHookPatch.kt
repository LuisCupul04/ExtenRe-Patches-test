/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.utils.navigation

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.extensions.InstructionExtensions.instructions
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod
import com.extenre.patches.shared.mainactivity.injectOnBackPressedMethodCall
import com.extenre.patches.youtube.utils.extension.Constants.SHARED_PATH
import com.extenre.patches.youtube.utils.extension.sharedExtensionPatch
import com.extenre.patches.youtube.utils.mainactivity.mainActivityResolvePatch
import com.extenre.patches.youtube.utils.playertype.playerTypeHookPatch
import com.extenre.patches.youtube.utils.playservice.is_20_21_or_greater
import com.extenre.patches.youtube.utils.playservice.is_20_28_or_greater
import com.extenre.patches.youtube.utils.playservice.versionCheckPatch
import com.extenre.patches.youtube.utils.resourceid.sharedResourceIdPatch
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.fingerprint.mutableClassOrThrow
import com.extenre.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

internal const val EXTENSION_CLASS_DESCRIPTOR =
    "$SHARED_PATH/NavigationBar;"
internal const val EXTENSION_NAVIGATION_BUTTON_DESCRIPTOR =
    "$SHARED_PATH/NavigationBar\$NavigationButton;"

private lateinit var bottomBarContainerMethod: MutableMethod
private var bottomBarContainerOffset = 0

lateinit var navigationButtonsMethod: MutableMethod
    private set

lateinit var hookNavigationButtonCreated: (String) -> Unit

val navigationBarHookPatch = bytecodePatch(
    name = "navigation-Bar-Hook-Patch",
    description = "navigationBarHookPatch"
) {
    dependsOn(
        sharedExtensionPatch,
        mainActivityResolvePatch,
        playerTypeHookPatch,
        sharedResourceIdPatch,
        versionCheckPatch,
    )

    execute {
        fun MutableMethod.addHook(
            hook: NavigationHook,
            insertPredicate: Instruction.() -> Boolean
        ) {
            val filtered = instructions.filter(insertPredicate)
            if (filtered.isEmpty()) throw PatchException("Could not find insert indexes")
            filtered.forEach {
                val insertIndex = it.location.index + 2
                val register = getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static { v$register }, " +
                            "$EXTENSION_CLASS_DESCRIPTOR->${hook.methodName}(${hook.parameters})V",
                )
            }
        }

        initializeButtonsFingerprint.mutableMethodOrThrow(pivotBarConstructorFingerprint).apply {
            navigationButtonsMethod = this

            // Hook the current navigation bar enum value. Note, the 'You' tab does not have an enum value.
            val navigationEnumClassName = navigationEnumFingerprint.mutableClassOrThrow().type
            addHook(NavigationHook.SET_LAST_APP_NAVIGATION_ENUM) {
                opcode == Opcode.INVOKE_STATIC &&
                        getReference<MethodReference>()?.definingClass == navigationEnumClassName
            }

            // Hook the creation of navigation tab views.
            val drawableTabMethod =
                pivotBarButtonsCreateDrawableViewFingerprint.mutableMethodOrThrow()
            addHook(NavigationHook.NAVIGATION_TAB_LOADED) predicate@{
                MethodUtil.methodSignaturesMatch(
                    getReference<MethodReference>() ?: return@predicate false,
                    drawableTabMethod,
                )
            }

            if (is_20_21_or_greater && !is_20_28_or_greater) {
                val imageResourceIntTabMethod =
                    pivotBarButtonsCreateResourceIntViewFingerprint.mutableMethodOrThrow()
                addHook(NavigationHook.NAVIGATION_TAB_LOADED) predicate@{
                    MethodUtil.methodSignaturesMatch(
                        getReference<MethodReference>() ?: return@predicate false,
                        imageResourceIntTabMethod,
                    )
                }
            }

            val imageResourceTabMethod =
                pivotBarButtonsCreateResourceViewFingerprint.mutableMethodOrThrow()
            addHook(NavigationHook.NAVIGATION_IMAGE_RESOURCE_TAB_LOADED) predicate@{
                MethodUtil.methodSignaturesMatch(
                    getReference<MethodReference>() ?: return@predicate false,
                    imageResourceTabMethod,
                )
            }
        }

        pivotBarButtonsViewSetSelectedFingerprint.mutableMethodOrThrow().apply {
            val index = indexOfSetViewSelectedInstruction(this)
            val instruction = getInstruction<FiveRegisterInstruction>(index)
            val viewRegister = instruction.registerC
            val isSelectedRegister = instruction.registerD

            addInstruction(
                index + 1,
                "invoke-static { v$viewRegister, v$isSelectedRegister }, " +
                        "$EXTENSION_CLASS_DESCRIPTOR->navigationTabSelected(Landroid/view/View;Z)V",
            )
        }

        injectOnBackPressedMethodCall(
            EXTENSION_CLASS_DESCRIPTOR,
            "onBackPressed"
        )

        bottomBarContainerMethod = initializeBottomBarContainerFingerprint.mutableMethodOrThrow()

        hookNavigationButtonCreated = { extensionClassDescriptor ->
            navigationBarHookCallbackFingerprint.mutableMethodOrThrow().addInstruction(
                0,
                "invoke-static { p0, p1 }, $extensionClassDescriptor->navigationTabCreated" +
                        "(${EXTENSION_NAVIGATION_BUTTON_DESCRIPTOR}Landroid/view/View;)V",
            )
        }
    }
}

fun addBottomBarContainerHook(descriptor: String) {
    bottomBarContainerMethod.apply {
        val layoutChangeListenerIndex = indexOfLayoutChangeListenerInstruction(this)
        val bottomBarContainerRegister =
            getInstruction<FiveRegisterInstruction>(layoutChangeListenerIndex).registerC

        addInstruction(
            layoutChangeListenerIndex + bottomBarContainerOffset--,
            "invoke-static { v$bottomBarContainerRegister }, $descriptor"
        )
    }
}

internal enum class NavigationHook(val methodName: String, val parameters: String) {
    SET_LAST_APP_NAVIGATION_ENUM("setLastAppNavigationEnum", "Ljava/lang/Enum;"),
    NAVIGATION_TAB_LOADED("navigationTabLoaded", "Landroid/view/View;"),
    NAVIGATION_IMAGE_RESOURCE_TAB_LOADED(
        "navigationImageResourceTabLoaded",
        "Landroid/view/View;"
    ),
}
