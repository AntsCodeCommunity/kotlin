// DO NOT EDIT MANUALLY!
// Generated by org/jetbrains/kotlin/generators/arguments/GenerateGradleOptions.kt
package org.jetbrains.kotlin.gradle.dsl

@Suppress("DEPRECATION")
internal abstract class KotlinJvmOptionsBase : org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions {

    private var allWarningsAsErrorsField: kotlin.Boolean? = null
    override var allWarningsAsErrors: kotlin.Boolean
        get() = allWarningsAsErrorsField ?: false
        set(value) {
            allWarningsAsErrorsField = value
        }

    private var suppressWarningsField: kotlin.Boolean? = null
    override var suppressWarnings: kotlin.Boolean
        get() = suppressWarningsField ?: false
        set(value) {
            suppressWarningsField = value
        }

    private var verboseField: kotlin.Boolean? = null
    override var verbose: kotlin.Boolean
        get() = verboseField ?: false
        set(value) {
            verboseField = value
        }

    override var apiVersion: kotlin.String? = null

    override var languageVersion: kotlin.String? = null

    private var useFirField: kotlin.Boolean? = null
    override var useFir: kotlin.Boolean
        get() = useFirField ?: false
        set(value) {
            useFirField = value
        }

    private var javaParametersField: kotlin.Boolean? = null
    override var javaParameters: kotlin.Boolean
        get() = javaParametersField ?: false
        set(value) {
            javaParametersField = value
        }

    override var jvmTarget: kotlin.String? = null

    override var moduleName: kotlin.String? = null

    private var noJdkField: kotlin.Boolean? = null
    override var noJdk: kotlin.Boolean
        get() = noJdkField ?: false
        set(value) {
            noJdkField = value
        }

    private var useOldBackendField: kotlin.Boolean? = null
    override var useOldBackend: kotlin.Boolean
        get() = useOldBackendField ?: false
        set(value) {
            useOldBackendField = value
        }

    internal open fun updateArguments(args: org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments) {
        allWarningsAsErrorsField?.let { args.allWarningsAsErrors = it }
        suppressWarningsField?.let { args.suppressWarnings = it }
        verboseField?.let { args.verbose = it }
        apiVersion?.let { args.apiVersion = it }
        languageVersion?.let { args.languageVersion = it }
        useFirField?.let { args.useFir = it }
        javaParametersField?.let { args.javaParameters = it }
        jvmTarget?.let { args.jvmTarget = it }
        moduleName?.let { args.moduleName = it }
        noJdkField?.let { args.noJdk = it }
        useOldBackendField?.let { args.useOldBackend = it }
    }
}

internal fun org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments.fillDefaultValues() {
    allWarningsAsErrors = false
    suppressWarnings = false
    verbose = false
    apiVersion = null
    languageVersion = null
    useFir = false
    javaParameters = false
    jvmTarget = null
    moduleName = null
    noJdk = false
    useOldBackend = false
    noStdlib = true
    noReflect = true
}
