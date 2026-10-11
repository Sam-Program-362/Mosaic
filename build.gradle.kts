// Top-level build file. Plugins are declared here without applying them so that
// each module can opt in through the version catalog.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
}
