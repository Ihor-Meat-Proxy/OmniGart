plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

/**
 * Fail the build if any Russian (ru / ru-*) locale catalogs or resource
 * directories appear. Wired into the root [check] lifecycle so CI catches it
 * inside "KMP + Android + Wear" rather than as a separate required job.
 */
tasks.register<Exec>("checkNoRuLocale") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description =
        "Fail if any ru/ru-* locale resource directories or string catalogs exist"
    workingDir = rootDir
    commandLine("bash", "./scripts/check_no_ru_locale.sh")
}

tasks.register<Exec>("checkAppReviewCompliance") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description =
        "Fail on banned medical/competitor copy, empty usage descriptions, " +
            "release debug menus, or incomplete PrivacyInfo"
    workingDir = rootDir
    commandLine("python3", "./scripts/check_app_review_compliance.py")
}

tasks.register<Exec>("packBundledContent") {
    group = "build"
    description =
        "Generate generated/content-bundle from content/, docs/evidence, design icons/body-maps"
    workingDir = rootDir
    commandLine("python3", "./scripts/pack_bundled_content.py")
}

tasks.register<Exec>("verifyBundledContent") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description =
        "Fail if generated/content-bundle is stale vs content sources"
    workingDir = rootDir
    commandLine("python3", "./scripts/pack_bundled_content.py", "--verify")
}

tasks.named("check") {
    dependsOn("checkNoRuLocale", "checkAppReviewCompliance", "verifyBundledContent")
}
