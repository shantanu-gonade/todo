plugins {
    alias(libs.plugins.todoapp.android.library)
    alias(libs.plugins.todoapp.android.hilt)
}
android { namespace = "com.eulerity.todo.core.domain" }
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:common"))
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
