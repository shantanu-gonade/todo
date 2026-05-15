plugins {
    alias(libs.plugins.todoapp.android.library)
    alias(libs.plugins.todoapp.android.hilt)
}
android { namespace = "com.eulerity.todo.core.data" }
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
