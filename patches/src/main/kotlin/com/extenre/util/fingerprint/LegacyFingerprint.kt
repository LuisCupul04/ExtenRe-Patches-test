/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package com.extenre.util.fingerprint

import com.extenre.patcher.Fingerprint
import com.extenre.patcher.Match
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.fingerprint
import com.extenre.patcher.patch.BytecodePatchContext
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.util.proxy.mutableTypes.MutableClass
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod
import com.extenre.util.containsLiteralInstruction
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstLiteralInstruction
import com.extenre.util.injectLiteralInstructionViewCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.util.MethodUtil

private val String.exception
    get() = PatchException("Failed to resolve $this")

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.resolvable(): Boolean =
    second.methodOrNull != null

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.definingClassOrThrow(): String =
    second.classDefOrNull?.type ?: throw first.exception

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.matchOrThrow(): Match {
    val classDef = second.classDefOrNull ?: throw first.exception
    return second.match(classDef) ?: throw first.exception
}

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.matchOrThrow(parentFingerprint: Pair<String, Fingerprint>): Match {
    val parentClassDef = parentFingerprint.second.classDefOrNull
        ?: throw parentFingerprint.first.exception
    return second.matchOrNull(parentClassDef) ?: throw first.exception
}

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.matchOrNull(): Match? =
    second.classDefOrNull?.let { second.matchOrNull(it) }

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.matchOrNull(parentFingerprint: Pair<String, Fingerprint>): Match? =
    parentFingerprint.second.classDefOrNull?.let { parentClassDef ->
        second.matchOrNull(parentClassDef)
    }

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.methodOrNull(): Method? =
    second.methodOrNull

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.methodOrThrow(): Method =
    second.method ?: throw first.exception

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.methodOrThrow(parentFingerprint: Pair<String, Fingerprint>): Method =
    matchOrThrow(parentFingerprint).method

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.originalMethodOrThrow(): Method =
    second.originalMethod ?: throw first.exception

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.originalMethodOrThrow(parentFingerprint: Pair<String, Fingerprint>): Method =
    matchOrThrow(parentFingerprint).originalMethod

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.mutableClassOrThrow(): MutableClass {
    val classDef = second.classDefOrNull ?: throw first.exception
    // Usar proxy en lugar de mutableClassDefBy para evitar problemas de importación
    return proxy(classDef).mutableClass
}

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.mutableMethodOrThrow(): MutableMethod {
    val method = second.method ?: throw first.exception
    val mutableClass = mutableClassOrThrow()
    return mutableClass.methods.first { MethodUtil.methodSignaturesMatch(it, method) }
}

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.mutableMethodOrThrow(parentFingerprint: Pair<String, Fingerprint>): MutableMethod {
    val method = methodOrThrow(parentFingerprint)
    val mutableClass = mutableClassOrThrow()
    return mutableClass.methods.first { MethodUtil.methodSignaturesMatch(it, method) }
}

context(BytecodePatchContext)
internal fun Pair<String, Fingerprint>.methodCall(): String =
    mutableMethodOrThrow().methodCall()

context(BytecodePatchContext)
internal fun MutableMethod.methodCall(): String {
    var methodCall = "$definingClass->$name("
    for (i in 0 until parameters.size) {
        methodCall += parameterTypes[i]
    }
    methodCall += ")$returnType"
    return methodCall
}

context(BytecodePatchContext)
fun Pair<String, Fingerprint>.injectLiteralInstructionBooleanCall(
    literal: Long,
    descriptor: String
) {
    val method = mutableMethodOrThrow()
    method.apply {
        val literalIndex = indexOfFirstLiteralInstruction(literal)
        val index = indexOfFirstInstructionOrThrow(literalIndex, Opcode.MOVE_RESULT)
        val register = getInstruction<OneRegisterInstruction>(index).registerA

        val smaliInstruction =
            if (descriptor.startsWith("0x")) """
                const/16 v$register, $descriptor
                """
            else if (descriptor.endsWith("(Z)Z")) """
                invoke-static/range { v$register .. v$register }, $descriptor
                move-result v$register
                """
            else """
                invoke-static {}, $descriptor
                move-result v$register
                """

        addInstructions(
            index + 1,
            smaliInstruction
        )
    }
}

context(BytecodePatchContext)
fun Pair<String, Fingerprint>.injectLiteralInstructionViewCall(
    literal: Long,
    smaliInstruction: String
) {
    val method = mutableMethodOrThrow()
    method.injectLiteralInstructionViewCall(literal, smaliInstruction)
}

internal fun legacyFingerprint(
    name: String,
    fuzzyPatternScanThreshold: Int = 0,
    accessFlags: Int? = null,
    returnType: String? = null,
    parameters: List<String>? = null,
    opcodes: List<Opcode?>? = null,
    strings: List<String>? = null,
    literals: List<Long>? = null,
    customFingerprint: ((methodDef: Method, classDef: ClassDef) -> Boolean)? = null,
) = Pair(
    name,
    fingerprint(fuzzyPatternScanThreshold = fuzzyPatternScanThreshold) {
        if (accessFlags != null) {
            accessFlags(accessFlags)
        }
        if (returnType != null) {
            returns(returnType)
        }
        if (parameters != null) {
            parameters(*parameters.toTypedArray())
        }
        if (opcodes != null) {
            opcodes(*opcodes.toTypedArray())
        }
        if (strings != null) {
            strings(*strings.toTypedArray())
        }
        custom { method, classDef ->
            if (literals != null) {
                for (literal in literals)
                    if (!method.containsLiteralInstruction(literal))
                        return@custom false
            }
            if (customFingerprint != null && !customFingerprint(method, classDef)) {
                return@custom false
            }

            return@custom true
        }
    }
)