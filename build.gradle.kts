// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    extra.apply {
        set("compose_bom_version", "2023.08.00")
        set("activity_compose_version", "1.8.2")
        set("kotlin_compiler_ext_version", "1.5.1")
    }
}
plugins {
    `maven-publish`
    id("com.android.library") version "8.2.1" apply false
    id("com.android.application") version "8.2.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
}