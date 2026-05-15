plugins {
    alias(libs.plugins.todoapp.android.library)
    alias(libs.plugins.todoapp.android.library.compose)
}
android { namespace = "com.eulerity.todo.core.ui" }
dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(libs.kotlinx.datetime)
}
