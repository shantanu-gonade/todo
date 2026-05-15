plugins {
    alias(libs.plugins.todoapp.android.library)
    alias(libs.plugins.todoapp.android.library.compose)
}
android { namespace = "com.eulerity.todo.core.designsystem" }
dependencies {
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
}
