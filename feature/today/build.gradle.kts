plugins {
    alias(libs.plugins.todoapp.android.feature)
}
android { namespace = "com.eulerity.todo.feature.today" }
dependencies {
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.datetime)

    // Test source set needs TaskRepository to wire real use-cases to FakeTaskRepository.
    // testImplementation only — production code never touches :core:data directly;
    // features access the data layer exclusively through :core:domain use-cases.
    testImplementation(project(":core:data"))
}
