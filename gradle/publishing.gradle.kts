apply(plugin = "maven-publish")

group = "io.github.vad4nus"

configure<PublishingExtension> {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set(project.name)
            description.set(provider { project.description })
            url.set("https://github.com/Vad4nus/lockstep-kmp")
            licenses {
                license {
                    name.set("Apache-2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("Vad4nus")
                    name.set("Vad4nus")
                    url.set("https://github.com/Vad4nus")
                }
            }
            scm {
                url.set("https://github.com/Vad4nus/lockstep-kmp")
                connection.set("scm:git:https://github.com/Vad4nus/lockstep-kmp.git")
                developerConnection.set("scm:git:ssh://git@github.com/Vad4nus/lockstep-kmp.git")
            }
        }
    }
}
