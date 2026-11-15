package io.github.vad4nus.lockstep.skie.render

import kotlin.test.Test
import kotlin.test.assertEquals

class TypeSpellingTest {

    private val enums = setOf("Category")

    @Test
    fun enumInsideObjCGenericBecomesReferenceClass() {
        assertEquals(
            "[SampleShared.KotlinPair<SampleShared.KotlinInt, SampleShared.__Category>]",
            referenceEnums("[SampleShared.KotlinPair<SampleShared.KotlinInt, SampleShared.Category>]", enums),
        )
        assertEquals(
            "SampleShared.KotlinPair<SampleShared.KotlinInt, SampleShared.__Category>?",
            referenceEnums("SampleShared.KotlinPair<SampleShared.KotlinInt, SampleShared.Category>?", enums),
        )
    }

    @Test
    fun enumsInBridgedPositionsAreUntouched() {
        assertEquals("SampleShared.Category", referenceEnums("SampleShared.Category", enums))
        assertEquals("SampleShared.Category?", referenceEnums("SampleShared.Category?", enums))
        assertEquals("[SampleShared.Category]", referenceEnums("[SampleShared.Category]", enums))
        assertEquals("[String : SampleShared.Category]", referenceEnums("[String : SampleShared.Category]", enums))
        assertEquals("Swift.Array<SampleShared.Category>", referenceEnums("Swift.Array<SampleShared.Category>", enums))
    }

    @Test
    fun unrelatedTypesAreUntouched() {
        assertEquals("Pair<Int, Other>", referenceEnums("Pair<Int, Other>", enums))
        assertEquals("Pair<Int, Other>", referenceEnums("Pair<Int, Other>", emptySet()))
    }

    @Test
    fun importsComeFromQualifiedExternalModules() {
        val types = listOf("UIKit.UIImage?", "Swift.String", "[Foundation.Date]", "DIHelper.ImageDesc?", "(() -> CoreGraphics.CGFloat)?")
        val modules = setOf("UIKit", "Swift", "Foundation", "CoreGraphics")

        assertEquals(listOf("CoreGraphics", "UIKit"), importsFor(types, modules))
    }
}
