plugins {
    alias(libs.plugins.todoapp.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}
dependencies {
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
