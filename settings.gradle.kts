pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BarcodeScanner"
include(":BarcodeScannerLibrary")
// https://developer.android.com/build/publish-library/configure-test-fixtures#kts
//  Gradle expects that the test fixtures artifact declares a capability with coordinates
//  groupId:artifactId-test-fixtures:version. This is not currently done automatically by either
//  the test fixture support or the Maven Publish Plugin, and therefore must be done manually.
project(":BarcodeScannerLibrary").name = "barcode-scanner"
include(":BarcodeScannerApp")
