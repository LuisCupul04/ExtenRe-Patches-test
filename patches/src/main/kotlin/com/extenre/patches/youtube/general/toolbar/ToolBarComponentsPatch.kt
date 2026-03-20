/*
 * Copyright (C) 2022 ReVanced LLC
 * Copyright (C) 2022 inotia00
 * Copyright (C) 2026 LuisCupul04
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.extenre.patches.youtube.general.toolbar

import com.extenre.patcher.extensions.InstructionExtensions.addInstruction
import com.extenre.patcher.extensions.InstructionExtensions.addInstructions
import com.extenre.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.extenre.patcher.extensions.InstructionExtensions.getInstruction
import com.extenre.patcher.extensions.InstructionExtensions.removeInstruction
import com.extenre.patcher.extensions.InstructionExtensions.replaceInstruction
import com.extenre.patcher.patch.PatchException
import com.extenre.patcher.patch.bytecodePatch
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod
import com.extenre.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.extenre.patcher.util.smali.ExternalLabel
import com.extenre.patches.youtube.utils.castbutton.castButtonPatch
import com.extenre.patches.youtube.utils.castbutton.hookToolBarCastButton
import com.extenre.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import com.extenre.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import com.extenre.patches.youtube.utils.patch.PatchList.TOOLBAR_COMPONENTS
import com.extenre.patches.youtube.utils.playservice.is_19_16_or_greater
import com.extenre.patches.youtube.utils.playservice.is_19_46_or_greater
import com.extenre.patches.youtube.utils.playservice.is_20_15_or_greater
import com.extenre.patches.youtube.utils.playservice.versionCheckPatch
import com.extenre.patches.youtube.utils.resourceid.actionBarRingoBackground
import com.extenre.patches.youtube.utils.resourceid.sharedResourceIdPatch
import com.extenre.patches.youtube.utils.resourceid.ytOutlineVideoCamera
import com.extenre.patches.youtube.utils.resourceid.ytPremiumWordMarkHeader
import com.extenre.patches.youtube.utils.resourceid.ytWordMarkHeader
import com.extenre.patches.youtube.utils.settings.ResourceUtils.addPreference
import com.extenre.patches.youtube.utils.settings.ResourceUtils.getContext
import com.extenre.patches.youtube.utils.settings.settingsPatch
import com.extenre.patches.youtube.utils.toolbar.hookToolBar
import com.extenre.patches.youtube.utils.toolbar.toolBarHookPatch
import com.extenre.util.REGISTER_TEMPLATE_REPLACEMENT
import com.extenre.util.Utils.printWarn
import com.extenre.util.doRecursively
import com.extenre.util.findInstructionIndicesReversedOrThrow
import com.extenre.util.findMethodOrThrow
import com.extenre.util.fingerprint.injectLiteralInstructionBooleanCall
import com.extenre.util.fingerprint.matchOrThrow
import com.extenre.util.fingerprint.methodCall
import com.extenre.util.fingerprint.mutableMethodOrThrow
import com.extenre.util.fingerprint.mutableClassOrThrow
import com.extenre.util.getReference
import com.extenre.util.getWalkerMethod
import com.extenre.util.indexOfFirstInstruction
import com.extenre.util.indexOfFirstInstructionOrThrow
import com.extenre.util.indexOfFirstInstructionReversedOrThrow
import com.extenre.util.indexOfFirstLiteralInstructionOrThrow
import com.extenre.util.replaceLiteralInstructionCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.util.MethodUtil
import org.w3c.dom.Element

@Suppress("unused")
val toolBarComponentsPatch = bytecodePatch(
    name = TOOLBAR_COMPONENTS.key,
    description = "${TOOLBAR_COMPONENTS.title}: ${TOOLBAR_COMPONENTS.summary}",
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        castButtonPatch,
        sharedResourceIdPatch,
        settingsPatch,
        toolBarHookPatch,
        versionCheckPatch,
    )

    execute {
        fun MutableMethod.injectSearchBarHook(
            insertIndex: Int,
            insertRegister: Int,
            descriptor: String
        ) =
            addInstructions(
                insertIndex, """
                invoke-static {v$insertRegister}, $GENERAL_CLASS_DESCRIPTOR->$descriptor(Z)Z
                move-result v$insertRegister
                """
            )

        fun MutableMethod.injectSearchBarHook(
            insertIndex: Int,
            descriptor: String
        ) =
            injectSearchBarHook(
                insertIndex,
                getInstruction<OneRegisterInstruction>(insertIndex).registerA,
                descriptor
            )

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: GENERAL",
            "SETTINGS: TOOLBAR_COMPONENTS"
        )

        // region patch for change YouTube header

        // Invoke YouTube's header attribute into extension.
        val smaliInstruction = """
            invoke-static {}, $GENERAL_CLASS_DESCRIPTOR->getHeaderAttributeId()I
            move-result v$REGISTER_TEMPLATE_REPLACEMENT
            """

        arrayOf(
            ytPremiumWordMarkHeader,
            ytWordMarkHeader
        ).forEach { literal ->
            replaceLiteralInstructionCall(literal, smaliInstruction)
        }

        // YouTube's headers have the form of AttributeSet, which is decoded from YouTube's built-in classes.
        val attributeResolverMethod = attributeResolverFingerprint.mutableMethodOrThrow()
        val attributeResolverMethodCall =
            attributeResolverMethod.definingClass + "->" + attributeResolverMethod.name + "(Landroid/content/Context;I)Landroid/graphics/drawable/Drawable;"

        // Reemplazar findmutableMethodOrThrow por búsqueda manual
        run {
            val method = findMethodOrThrow(GENERAL_CLASS_DESCRIPTOR) {
                name == "getHeaderDrawable"
            }
            val classDef = classes.find { it.type == GENERAL_CLASS_DESCRIPTOR }
                ?: throw PatchException("Class not found: $GENERAL_CLASS_DESCRIPTOR")
            val mutableMethod = proxy(classDef).mutableClass.methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.addInstructions(
                0, """
                    invoke-static {p0, p1}, $attributeResolverMethodCall
                    move-result-object p0
                    return-object p0
                    """
            )
        }

        // The sidebar's header is lithoView. Add a listener to change it.
        drawerContentViewFingerprint.mutableMethodOrThrow(drawerContentViewConstructorFingerprint).apply {
            val insertIndex = indexOfAddViewInstruction(this)
            val insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD

            addInstruction(
                insertIndex,
                "invoke-static {v$insertRegister}, $GENERAL_CLASS_DESCRIPTOR->setDrawerNavigationHeader(Landroid/view/View;)V"
            )
        }

        // Override the header in the search bar.
        setActionBarRingoFingerprint.mutableClassOrThrow().methods.first { method ->
            MethodUtil.isConstructor(method)
        }.apply {
            val insertIndex = indexOfFirstInstructionOrThrow(Opcode.IPUT_BOOLEAN)
            val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

            addInstruction(
                insertIndex + 1,
                "const/4 v$insertRegister, 0x0"
            )
            addInstructions(
                insertIndex, """
                    invoke-static {}, $GENERAL_CLASS_DESCRIPTOR->overridePremiumHeader()Z
                    move-result v$insertRegister
                    """
            )
        }

        // endregion

        // region patch for enable wide search bar

        // Limitation: Premium header will not be applied for YouTube Premium users if the user uses the 'Wide search bar with header' option.
        // This is because it forces the deprecated search bar to be loaded.
        // As a solution to this limitation, 'Change YouTube header' patch is required.
        actionBarRingoBackgroundFingerprint.mutableMethodOrThrow().apply {
            val viewIndex =
                indexOfFirstLiteralInstructionOrThrow(actionBarRingoBackground) + 2
            val viewRegister = getInstruction<OneRegisterInstruction>(viewIndex).registerA

            addInstructions(
                viewIndex + 1,
                "invoke-static {v$viewRegister}, $GENERAL_CLASS_DESCRIPTOR->setWideSearchBarLayout(Landroid/view/View;)V"
            )

            val targetIndex = indexOfActionBarRingoBackgroundTabletInstruction(this) + 1
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            injectSearchBarHook(
                targetIndex + 1,
                targetRegister,
                "enableWideSearchBarWithHeaderInverse"
            )
        }

        actionBarRingoTextFingerprint.mutableMethodOrThrow(actionBarRingoBackgroundFingerprint).apply {
            val targetIndex = indexOfActionBarRingoTextTabletInstructions(this) + 1
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            injectSearchBarHook(
                targetIndex + 1,
                targetRegister,
                "enableWideSearchBarWithHeader"
            )
        }

        actionBarRingoConstructorFingerprint.mutableMethodOrThrow().apply {
            val staticCalls = implementation!!.instructions
                .withIndex()
                .filter { (_, instruction) ->
                    val methodReference = (instruction as? ReferenceInstruction)?.reference
                    instruction.opcode == Opcode.INVOKE_STATIC &&
                            methodReference is MethodReference &&
                            methodReference.parameterTypes.size == 1 &&
                            methodReference.returnType == "Z"
                }

            if (staticCalls.size != 2)
                throw PatchException("Size of staticCalls does not match: ${staticCalls.size}")

            mapOf(
                staticCalls.elementAt(0).index to "enableWideSearchBar",
                staticCalls.elementAt(1).index to "enableWideSearchBarWithHeader"
            ).forEach { (index, descriptor) ->
                val walkerMethod = getWalkerMethod(index)

                walkerMethod.apply {
                    injectSearchBarHook(
                        implementation!!.instructions.lastIndex,
                        descriptor
                    )
                }
            }
        }

        youActionBarFingerprint.matchOrThrow(setActionBarRingoFingerprint).let {
            val method = it.method
            val classDef = it.classDef
            val mutableMethod = proxy(classDef).mutableClass.methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.apply {
                injectSearchBarHook(
                    it.patternMatch!!.endIndex,
                    "enableWideSearchBarInYouTab"
                )
            }
        }

        // This attribution cannot be changed in extension, so change it in the xml file.

        getContext().document("res/layout/action_bar_ringo_background.xml").use { document ->
            document.doRecursively { node ->
                arrayOf("layout_marginStart").forEach replacement@{ replacement ->
                    if (node !is Element) return@replacement

                    node.getAttributeNode("android:$replacement")?.let { attribute ->
                        attribute.textContent = "0.0dip"
                    }
                }
            }
        }

        // endregion

        // region patch for hide cast button

        hookToolBarCastButton()

        // endregion

        // region patch for hide create button

        hookToolBar("$GENERAL_CLASS_DESCRIPTOR->hideCreateButton")

        // endregion

        // region patch for hide notification button

        hookToolBar("$GENERAL_CLASS_DESCRIPTOR->hideNotificationButton")

        // endregion

        // region patch for hide search button

        hookToolBar("$GENERAL_CLASS_DESCRIPTOR->hideSearchButton")

        toolbarSearchButtonFingerprint
            .mutableMethodOrThrow(toolbarSearchButtonLabelFingerprint)
            .apply {
                val index = indexOfShowAsActionInstruction(this)
                val instruction = getInstruction<FiveRegisterInstruction>(index)

                replaceInstruction(
                    index,
                    "invoke-static {v${instruction.registerC}, v${instruction.registerD}}, " +
                            "$GENERAL_CLASS_DESCRIPTOR->hideSearchButton(Landroid/view/MenuItem;I)V"
                )
            }

        // endregion

        // region patch for hide search term thumbnail

        createSearchSuggestionsFingerprint.mutableMethodOrThrow().apply {
            val iteratorIndex = indexOfIteratorInstruction(this)
            val replaceIndex = indexOfFirstInstruction(iteratorIndex) {
                opcode == Opcode.IGET_OBJECT &&
                        getReference<FieldReference>()?.type == "Landroid/widget/ImageView;"
            }
            if (replaceIndex > -1) {
                val uriIndex = indexOfFirstInstructionOrThrow(replaceIndex) {
                    opcode == Opcode.INVOKE_STATIC &&
                            getReference<MethodReference>()?.toString() == "Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;"
                }
                val jumpIndex = indexOfFirstInstructionOrThrow(uriIndex, Opcode.CONST_4)
                val replaceIndexInstruction = getInstruction<TwoRegisterInstruction>(replaceIndex)
                val freeRegister = replaceIndexInstruction.registerA
                val classRegister = replaceIndexInstruction.registerB
                val replaceIndexReference =
                    getInstruction<ReferenceInstruction>(replaceIndex).reference

                addInstructionsWithLabels(
                    replaceIndex + 1, """
                        invoke-static { }, $GENERAL_CLASS_DESCRIPTOR->hideSearchTermThumbnail()Z
                        move-result v$freeRegister
                        if-nez v$freeRegister, :hidden
                        iget-object v$freeRegister, v$classRegister, $replaceIndexReference
                        """, ExternalLabel("hidden", getInstruction(jumpIndex))
                )
                removeInstruction(replaceIndex)
            } else { // only for YT 20.03
                val insertIndex = indexOfFirstInstructionOrThrow(iteratorIndex) {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.toString() == "Landroid/widget/ImageView;->setVisibility(I)V"
                } - 1
                if (getInstruction(insertIndex).opcode != Opcode.CONST_4) {
                    throw PatchException("Failed to find insert index")
                }
                val freeRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA
                val uriIndex = indexOfFirstInstructionOrThrow(insertIndex) {
                    opcode == Opcode.INVOKE_STATIC &&
                            getReference<MethodReference>()?.toString() == "Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;"
                }
                val jumpIndex = indexOfFirstInstructionOrThrow(uriIndex, Opcode.CONST_4)

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static { }, $GENERAL_CLASS_DESCRIPTOR->hideSearchTermThumbnail()Z
                        move-result v$freeRegister
                        if-nez v$freeRegister, :hidden
                        """, ExternalLabel("hidden", getInstruction(jumpIndex))
                )
            }
        }

        if (is_19_16_or_greater) {
            searchFragmentFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                SEARCH_FRAGMENT_FEATURE_FLAG,
                "$GENERAL_CLASS_DESCRIPTOR->hideSearchTermThumbnail(Z)Z"
            )
        }

        // endregion

        // region patch for hide voice search button

        val searchBarMatch = searchBarFingerprint.matchOrThrow(searchBarParentFingerprint)
        val searchBarMethod = searchBarMatch.method
        val searchBarClassDef = searchBarMatch.classDef
        val searchBarMutableMethod = proxy(searchBarClassDef).mutableClass.methods.first {
            MethodUtil.methodSignaturesMatch(it, searchBarMethod)
        }
        searchBarMutableMethod.apply {
            val startIndex = searchBarMatch.patternMatch!!.startIndex
            val setVisibilityIndex = indexOfFirstInstructionOrThrow(startIndex) {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.name == "setVisibility"
            }
            val setVisibilityInstruction =
                getInstruction<FiveRegisterInstruction>(setVisibilityIndex)

            replaceInstruction(
                setVisibilityIndex,
                "invoke-static {v${setVisibilityInstruction.registerC}, v${setVisibilityInstruction.registerD}}, " +
                        "$GENERAL_CLASS_DESCRIPTOR->hideVoiceSearchButton(Landroid/view/View;I)V"
            )
        }

        val searchResultMatch = searchResultFingerprint.matchOrThrow()
        val searchResultMethod = searchResultMatch.method
        val searchResultClassDef = searchResultMatch.classDef
        val searchResultMutableMethod = proxy(searchResultClassDef).mutableClass.methods.first {
            MethodUtil.methodSignaturesMatch(it, searchResultMethod)
        }
        searchResultMutableMethod.apply {
            val voiceInputControllerActivityMethodCall =
                voiceInputControllerFingerprint
                    .mutableMethodOrThrow(voiceInputControllerParentFingerprint)
                    .methodCall()

            val voiceInputControllerActivityIndex =
                indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.toString() == voiceInputControllerActivityMethodCall
                }
            val setOnClickListenerIndex =
                indexOfFirstInstructionOrThrow(voiceInputControllerActivityIndex) {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "setOnClickListener"
                }
            val viewRegister =
                getInstruction<FiveRegisterInstruction>(setOnClickListenerIndex).registerC

            addInstruction(
                setOnClickListenerIndex + 1,
                "invoke-static {v$viewRegister}, $GENERAL_CLASS_DESCRIPTOR->hideVoiceSearchButton(Landroid/view/View;)V"
            )
        }

        // endregion

        // region patch for hide You may like section

        if (is_19_46_or_greater && !is_20_15_or_greater) {
            val (searchSuggestionEndpointClass, searchSuggestionEndpointField) = with(
                searchSuggestionEndpointFingerprint.mutableMethodOrThrow(
                    searchSuggestionEndpointParentFingerprint
                )
            ) {
                val isEmptyIndex = indexOfIsEmptyInstruction(this)
                val index = indexOfFirstInstructionReversedOrThrow(isEmptyIndex) {
                    opcode == Opcode.IGET_OBJECT &&
                            getReference<FieldReference>()?.type == "Ljava/lang/String;"
                }
                val searchSuggestionEndpointField =
                    getInstruction<ReferenceInstruction>(index).reference as FieldReference

                Pair(
                    searchSuggestionEndpointField.definingClass,
                    searchSuggestionEndpointField
                )
            }

            val searchSuggestionCollectionMatch = searchSuggestionCollectionFingerprint.matchOrThrow(
                createSearchSuggestionsFingerprint
            )
            val searchSuggestionCollectionMethod = searchSuggestionCollectionMatch.method
            val searchSuggestionCollectionClassDef = searchSuggestionCollectionMatch.classDef
            val searchSuggestionCollectionMutableMethod = proxy(searchSuggestionCollectionClassDef).mutableClass.methods.first {
                MethodUtil.methodSignaturesMatch(it, searchSuggestionCollectionMethod)
            }
            searchSuggestionCollectionMutableMethod.apply {
                val helperMethodName = "patch_setCollection"

                searchSuggestionCollectionMatch.classDef.methods.add(
                    ImmutableMethod(
                        searchSuggestionCollectionMatch.classDef.type,
                        helperMethodName,
                        listOf(
                            ImmutableMethodParameter(
                                "Ljava/util/Collection;",
                                annotations,
                                "collection"
                            ),
                            ImmutableMethodParameter(
                                "Ljava/lang/String;",
                                annotations,
                                "searchQuery"
                            )
                        ),
                        "Ljava/util/Collection;",
                        AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                        annotations,
                        null,
                        MutableMethodImplementation(8),
                    ).toMutable().apply {
                        addInstructionsWithLabels(
                            0,
                            """
                                    # Collection.
                                    move-object/from16 v0, p1
                                    # Search query.
                                    move-object/from16 v1, p2
                                    
                                    # Check that the setting is enabled and that the search query is empty.
                                    invoke-static {v1}, $GENERAL_CLASS_DESCRIPTOR->hideYouMayLikeSection(Ljava/lang/String;)Z
                                    move-result v2

                                    if-eqz v2, :exit
                                    
                                    invoke-interface {v0}, Ljava/util/Collection;->iterator()Ljava/util/Iterator;
                                    move-result-object v2
                                    
                                    :loop
                                    invoke-interface {v2}, Ljava/util/Iterator;->hasNext()Z
                                    move-result v3
                                    
                                    if-eqz v3, :exit
                                    
                                    invoke-interface {v2}, Ljava/util/Iterator;->next()Ljava/lang/Object;
                                    move-result-object v3
                                    instance-of v4, v3, $searchSuggestionEndpointClass

                                    if-eqz v4, :loop
                                    check-cast v3, $searchSuggestionEndpointClass
                                    iget-object v4, v3, $searchSuggestionEndpointField
                                    invoke-static {v3, v4}, $GENERAL_CLASS_DESCRIPTOR->isSearchHistory(Ljava/lang/Object;Ljava/lang/String;)Z
                                    move-result v3

                                    if-nez v3, :loop
                                    
                                    # If it's not a search history, it's a search term suggestion.
                                    # Remove it from the collection.
                                    invoke-interface {v2}, Ljava/util/Iterator;->remove()V
                                    goto :loop

                                    :exit
                                    return-object v0
                                    """,
                        )
                    }
                )

                addInstructions(
                    0, """
                            invoke-direct/range {p0 .. p2}, $definingClass->$helperMethodName(Ljava/util/Collection;Ljava/lang/String;)Ljava/util/Collection;
                            move-result-object v0
                            move-object/from16 p1, v0
                            """
                )
            }

            roundEdgeSearchBarFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                ROUND_EDGE_SEARCH_BAR_FEATURE_FLAG,
                "$GENERAL_CLASS_DESCRIPTOR->disableRoundSearchBar(Z)Z"
            )

            settingArray += "SETTINGS: HIDE_YOU_MAY_LIKE_SECTION"
        } else if (is_20_15_or_greater) {
            printWarn("\"Hide You may like section\" is not yet supported in this version. Use YouTube 20.14.43 or earlier.")
        }

        // endregion

        // region patch for hide YouTube Doodles

        yoodlesImageViewFingerprint.mutableMethodOrThrow().apply {
            findInstructionIndicesReversedOrThrow {
                opcode == Opcode.INVOKE_VIRTUAL
                        && getReference<MethodReference>()?.name == "setImageDrawable"
            }.forEach { insertIndex ->
                val (viewRegister, drawableRegister) = getInstruction<FiveRegisterInstruction>(
                    insertIndex
                ).let {
                    Pair(it.registerC, it.registerD)
                }
                replaceInstruction(
                    insertIndex,
                    "invoke-static {v$viewRegister, v$drawableRegister}, " +
                            "$GENERAL_CLASS_DESCRIPTOR->hideYouTubeDoodles(Landroid/widget/ImageView;Landroid/graphics/drawable/Drawable;)V"
                )
            }
        }

        // endregion

        // region patch for replace create button

        createButtonDrawableFingerprint.mutableMethodOrThrow().apply {
            val index = indexOfFirstLiteralInstructionOrThrow(ytOutlineVideoCamera)
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            addInstructions(
                index + 1, """
                    invoke-static {v$register}, $GENERAL_CLASS_DESCRIPTOR->getCreateButtonDrawableId(I)I
                    move-result v$register
                    """
            )
        }

        hookToolBar("$GENERAL_CLASS_DESCRIPTOR->replaceCreateButton")

        // Reemplazar findmutableMethodOrThrow por búsqueda manual
        run {
            val method = findMethodOrThrow(
                "Lcom/google/android/apps/youtube/app/application/Shell_SettingsActivity;"
            ) {
                name == "onCreate"
            }
            val classDef = classes.find { it.type == "Lcom/google/android/apps/youtube/app/application/Shell_SettingsActivity;" }
                ?: throw PatchException("Class not found: Lcom/google/android/apps/youtube/app/application/Shell_SettingsActivity;")
            val mutableMethod = proxy(classDef).mutableClass.methods.first {
                MethodUtil.methodSignaturesMatch(it, method)
            }
            mutableMethod.addInstruction(
                0,
                "invoke-static {p0}, $GENERAL_CLASS_DESCRIPTOR->setShellActivityTheme(Landroid/app/Activity;)V"
            )
        }

        // endregion

        // region add settings

        addPreference(
            settingArray,
            TOOLBAR_COMPONENTS
        )

        // endregion

    }
}