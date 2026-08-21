// Root build file. Deliberately declares NO plugins here (not even "apply
// false"): each module applies exactly the plugins it needs, from the
// version catalog in gradle/libs.versions.toml. This matters in this sandbox
// specifically — it has no Android SDK, and Google's Maven repo (which AGP's
// plugin marker resolves from) is not reachable, so anything that would force
// AGP's plugin coordinates to resolve as part of configuring the ROOT project
// would break `./gradlew :core-domain:test` too, even though core-domain
// itself has zero Android dependency. Keeping the root build script plugin-free
// plus `org.gradle.configureondemand=true` (see gradle.properties) means
// requesting :core-domain:test only ever configures the core-domain project.
tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
