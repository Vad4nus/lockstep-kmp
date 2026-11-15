package io.github.vad4nus.lockstep.gradle

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VersionCheckTest {

    @Test
    fun onlyALockstepLibraryOfAnotherVersionIsAMismatch() {
        assertNull(versionMismatch("io.github.vad4nus", "lockstep-mvvm", "1.0.0", "1.0.0"))
        assertNull(versionMismatch("io.github.vad4nus", "actioncable-kmp", "0.3.0", "1.0.0"))
        assertNull(versionMismatch("io.ktor", "lockstep", "0.3.0", "1.0.0"))
        assertNull(versionMismatch("io.github.vad4nus", "lockstep", null, "1.0.0"))
        assertNotNull(versionMismatch("io.github.vad4nus", "lockstep-mvvm-iosarm64", "0.9.0", "1.0.0"))
    }

    @Test
    fun aLibraryOfAnotherVersionFailsTheResolution() {
        val project = ProjectBuilder.builder().build()
        checkLibraryVersions(project)
        val libraries = project.configurations.create("libraries")
        project.dependencies.add("libraries", "io.github.vad4nus:lockstep-mvvm:0.0.1")
        val failure = assertFails { libraries.resolve() }
        assertTrue(generateSequence(failure) { it.cause }.any { "lockstep-mvvm is 0.0.1" in it.message.orEmpty() })
    }
}
