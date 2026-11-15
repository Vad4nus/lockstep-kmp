plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

description = "SKIE sub-plugin that generates Swift structs, enums and flow accessors for Lockstep"

version = libs.versions.lockstep.get()
apply(from = "../../gradle/publishing.gradle.kts")

publishing.publications.create<MavenPublication>("maven") { from(components["java"]) }

configurations.compileClasspath {
    attributes {
        attribute(Attribute.of("co.touchlab.skie.kotlin.compiler.version", String::class.java), libs.versions.skieCompilerVariant.get())
    }
}

dependencies {
    compileOnly(libs.skie.kotlin.compiler.core)
    compileOnly(libs.skie.kotlin.compiler.linker.plugin)
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlin.get()}")
    testImplementation(kotlin("test"))
}
