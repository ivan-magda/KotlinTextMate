plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.plugins.detekt.get().run {
        "${pluginId}:${pluginId}.gradle.plugin:${version}"
    })
}
