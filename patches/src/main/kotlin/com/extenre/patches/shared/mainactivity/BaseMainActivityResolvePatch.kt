/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.shared.mainactivity

import com.extenre.patcher.Fingerprint
import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.proxy.mutableTypes.MutableClass
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.fingerprint.mutableClassOrThrow
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import kotlin.properties.Delegates

lateinit var mainActivityMutableClass: MutableClass
    private set
lateinit var onConfigurationChangedMethod: MutableMethod
    private set
lateinit var onCreateMethod: MutableMethod
    private set
lateinit var onStartMethod: MutableMethod
    private set
lateinit var onStopMethod: MutableMethod
    private set

private lateinit var constructorMethod: MutableMethod
private lateinit var onBackPressedMethod: MutableMethod

private var constructorMethodIndex by Delegates.notNull<Int>()
private var onBackPressedMethodIndex by Delegates.notNull<Int>()

fun baseMainActivityResolvePatch(
    mainActivityOnCreateFingerprint: Pair<String, Fingerprint>,
) = bytecodePatch(
    name = "base-main-activity-resolve-patch",
    description = "Resolves the main activity for other patches.",
) {
    execute {
        onCreateMethod = mainActivityOnCreateFingerprint.mutableMethodOrThrow()
        mainActivityMutableClass = mainActivityOnCreateFingerprint.mutableClassOrThrow()

        // set constructor method
        constructorMethod = getMainActivityMethod("<init>")
        constructorMethodIndex = constructorMethod.implementation!!.instructions.lastIndex

        // set onBackPressed method
        onBackPressedMethod = getMainActivityMethod("onBackPressed")
        onBackPressedMethodIndex =
            onBackPressedMethod.indexOfFirstInstructionOrThrow(Opcode.RETURN_VOID)

        // set onConfigurationChanged method
        onConfigurationChangedMethod = getMainActivityMethod("onConfigurationChanged")

        onStartMethod = getMainActivityMethod("onStart")
        onStopMethod = getMainActivityMethod("onStop")
    }
}

internal fun injectConstructorMethodCall(classDescriptor: String, methodDescriptor: String) =
    constructorMethod.injectMethodCall(
        classDescriptor,
        methodDescriptor,
        constructorMethodIndex
    )

internal fun injectOnBackPressedMethodCall(classDescriptor: String, methodDescriptor: String) =
    onBackPressedMethod.injectMethodCall(
        classDescriptor,
        methodDescriptor,
        onBackPressedMethodIndex
    )

internal fun injectOnCreateMethodCall(classDescriptor: String, methodDescriptor: String) =
    onCreateMethod.injectMethodCall(classDescriptor, methodDescriptor)

internal fun getMainActivityMethod(methodDescriptor: String) =
    mainActivityMutableClass.methods.find { method -> method.name == methodDescriptor }
        ?: throw PatchException("Could not find $methodDescriptor")

private fun MutableMethod.injectMethodCall(
    classDescriptor: String,
    methodDescriptor: String
) = injectMethodCall(classDescriptor, methodDescriptor, 0)

private fun MutableMethod.injectMethodCall(
    classDescriptor: String,
    methodDescriptor: String,
    insertIndex: Int
) = addInstruction(
    insertIndex,
    "invoke-static/range {p0 .. p0}, $classDescriptor->$methodDescriptor(Landroid/app/Activity;)V"
)
