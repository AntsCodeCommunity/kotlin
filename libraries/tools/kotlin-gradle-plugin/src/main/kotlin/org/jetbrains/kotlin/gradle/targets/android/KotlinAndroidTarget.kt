/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import com.android.build.gradle.api.BaseVariant
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage.JAVA_RUNTIME_JARS
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.gradle.utils.listProperty
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.setProperty

open class KotlinAndroidTarget(
    override val targetName: String,
    project: Project
) : AbstractKotlinTarget(project) {

    override var disambiguationClassifier: String? = null
        internal set

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.androidJvm

    internal val compilationFactory = KotlinJvmAndroidCompilationFactory(project, this)

    override val compilations: NamedDomainObjectContainer<out KotlinJvmAndroidCompilation> =
        project.container(compilationFactory.itemClass, compilationFactory)

    /** Names of the Android library variants that should be published from the target's project within the default publications which are
     * set up if the `maven-publish` Gradle plugin is applied.
     *
     * Item examples:
     * * 'release' (in case no product flavors were defined)
     * * 'fooRelease' (for the release build type of a flavor 'foo')
     * * 'fooBarRelease' (for the release build type multi-dimensional flavors 'foo' and 'bar').
     *
     * If set to null, which can also be done with [publishAllLibraryVariants],
     * all library variants will be published, but not test or application variants. */
    var publishLibraryVariants: List<String>? = listOf()
        // Workaround for Groovy GString items in a list:
        set(value) {
            field = value?.map(Any::toString)
        }

    /** Add Android library variant names to [publishLibraryVariants]. */
    fun publishLibraryVariants(vararg names: String) {
        publishLibraryVariants = publishLibraryVariants.orEmpty() + names
    }

    /** Set up all of the Android library variants to be published from this target's project within the default publications, which are
     * set up if the `maven-publish` Gradle plugin is applied. This overrides the variants chosen with [publishLibraryVariants] */
    fun publishAllLibraryVariants() {
        publishLibraryVariants = null
    }

    /** If true, a publication will be created per merged product flavor, with the build types used as classifiers for the artifacts
     * published within each publication. If set to false, each Android variant will have a separate publication. */
    var publishLibraryVariantsGroupedByFlavor = false

    private fun checkPublishLibraryVariantsExist() {
        fun AbstractAndroidProjectHandler.getLibraryVariantNames() =
            mutableSetOf<String>().apply {
                project.forEachVariant {
                    if (getLibraryOutputTask(it) != null)
                        add(getVariantName(it))
                }
            }

        val variantNames = KotlinAndroidPlugin.androidTargetHandler().getLibraryVariantNames()

        val missingVariants =
            publishLibraryVariants?.minus(variantNames).orEmpty()

        if (missingVariants.isNotEmpty())
            throw InvalidUserDataException(
                "Kotlin target '$targetName' tried to set up publishing for Android build variants that are not library variants " +
                        "or do not exist:\n" + missingVariants.joinToString("\n") { "* $it" } +
                        "\nCheck the 'publishLibraryVariants' property, it should point to existing Android library variants. Publishing " +
                        "of application and test variants is not supported."
            )
    }

    override val kotlinComponents by lazy {
        checkPublishLibraryVariantsExist()

        KotlinAndroidPlugin.androidTargetHandler().doCreateComponents()
    }

    private fun isVariantPublished(variant: BaseVariant): Boolean {
        return publishLibraryVariants?.contains(getVariantName(variant)) ?: true
    }

    private fun AbstractAndroidProjectHandler.doCreateComponents(): Set<KotlinTargetComponent> {

        val publishableVariants = mutableListOf<BaseVariant>()
            .apply { project.forEachVariant { add(it) } }
            .toList() // Defensive copy against unlikely modification by the lambda that captures the list above in forEachVariant { }
            .filter { getLibraryOutputTask(it) != null }

        val publishableVariantGroups = publishableVariants.groupBy { variant ->
            val flavorNames = getFlavorNames(variant)
            if (publishLibraryVariantsGroupedByFlavor) {
                // For each flavor, we group its variants (which differ only in the build type) in a single component in order to publish
                // all of the build types of the flavor as a single module with the build type as the classifier of the artifacts
                flavorNames
            } else {
                flavorNames + getBuildTypeName(variant)
            }
        }

        return publishableVariantGroups.map { (flavorGroupNameParts, androidVariants) ->
            val nestedVariants = androidVariants.mapTo(mutableSetOf()) { androidVariant ->
                val androidVariantName = getVariantName(androidVariant)
                val compilation = compilations.getByName(androidVariantName)

                val flavorNames = getFlavorNames(androidVariant)
                val buildTypeName = getBuildTypeName(androidVariant)

                val artifactClassifier = buildTypeName.takeIf { it != "release" && publishLibraryVariantsGroupedByFlavor }

                createKotlinVariant(
                    lowerCamelCaseName(compilation.target.name, *flavorGroupNameParts.toTypedArray()),
                    compilation,
                    createAndroidUsageContexts(
                        androidVariant,
                        compilation,
                        artifactClassifier,
                        publishableVariants.filter(::isVariantPublished).map(::getBuildTypeName).distinct().size == 1
                    )
                ).apply {
                    publishable = isVariantPublished(androidVariant)
                    sourcesArtifacts = setOf(
                        sourcesJarArtifact(
                            compilation, compilation.disambiguateName(""),
                            dashSeparatedName(
                                compilation.target.name.toLowerCase(),
                                *flavorNames.map { it.toLowerCase() }.toTypedArray(),
                                buildTypeName.takeIf { it != "release" }?.toLowerCase()
                            ),
                            classifierPrefix = artifactClassifier
                        )
                    )

                    if (!publishLibraryVariantsGroupedByFlavor) {
                        defaultArtifactIdSuffix =
                            dashSeparatedName(
                                (getFlavorNames(androidVariant) + getBuildTypeName(androidVariant).takeIf { it != "release" })
                                    .map { it?.toLowerCase() }
                            ).takeIf { it.isNotEmpty() }
                    }
                }
            }

            if (publishLibraryVariantsGroupedByFlavor) {
                JointAndroidKotlinTargetComponent(
                    target = this@KotlinAndroidTarget,
                    nestedVariants = nestedVariants,
                    flavorNames = flavorGroupNameParts,
                    sourcesArtifacts = nestedVariants.filter { it.publishable }.flatMap { it.sourcesArtifacts }.toSet()
                )
            } else {
                nestedVariants.single()
            } as KotlinTargetComponent // Type inference corner case or bug? this cast in each branch is redundant but required here
        }.toSet()
    }

    private fun AbstractAndroidProjectHandler.createAndroidUsageContexts(
        variant: BaseVariant,
        compilation: KotlinCompilation<*>,
        artifactClassifier: String?,
        isSingleBuildType: Boolean
    ): Set<DefaultKotlinUsageContext> {
        val variantName = getVariantName(variant)
        val outputTaskOrProvider = getLibraryOutputTask(variant) ?: return emptySet()
        val artifact = run {
            val archivesConfigurationName = lowerCamelCaseName(targetName, variantName, "archives")
            project.configurations.maybeCreate(archivesConfigurationName).apply {
                isCanBeConsumed = false
                isCanBeResolved = false
            }
            project.artifacts.add(archivesConfigurationName, outputTaskOrProvider) { artifact ->
                artifact.classifier = artifactClassifier
            }
        }

        val apiElementsConfigurationName = lowerCamelCaseName(variantName, "apiElements")
        val runtimeElementsConfigurationName = lowerCamelCaseName(variantName, "runtimeElements")

        // Here, the Java Usage values are used intentionally as Gradle needs this for
        // ordering of the usage contexts (prioritizing the dependencies) when merging them into the POM;
        // These Java usages should not be replaced with the custom Kotlin usages.
        return listOf(
            apiElementsConfigurationName to javaApiUsageForMavenScoping(),
            runtimeElementsConfigurationName to JAVA_RUNTIME_JARS
        ).mapTo(mutableSetOf()) { (dependencyConfigurationName, usageName) ->
            val configuration = project.configurations.getByName(dependencyConfigurationName)
            DefaultKotlinUsageContext(
                compilation,
                project.usageByName(usageName),
                dependencyConfigurationName,
                overrideConfigurationArtifacts = project.setProperty { listOf(artifact) },
                overrideConfigurationAttributes = HierarchyAttributeContainer(configuration.attributes) {
                    val valueString = run {
                        val value = configuration.attributes.getAttribute(it)
                        (value as? Named)?.name ?: value.toString()
                    }
                    filterOutAndroidVariantAttribute(it) &&
                            filterOutAndroidBuildTypeAttribute(it, valueString, isSingleBuildType) &&
                            filterOutAndroidAgpVersionAttribute(it)
                }
            )
        }
    }

    /** We filter this variant out as it is never requested on the consumer side, while keeping it leads to ambiguity between Android and
     * JVM variants due to non-nesting sets of unmatched attributes. */
    private fun filterOutAndroidVariantAttribute(
        attribute: Attribute<*>
    ): Boolean =
        attribute.name != "com.android.build.gradle.internal.attributes.VariantAttr" &&
                attribute.name != "com.android.build.api.attributes.VariantAttr"

    private fun filterOutAndroidBuildTypeAttribute(
        it: Attribute<*>,
        valueString: String,
        isSinglePublishedVariant: Boolean
    ) = when {
        PropertiesProvider(project).keepAndroidBuildTypeAttribute -> true
        it.name != "com.android.build.api.attributes.BuildTypeAttr" -> true

        // then the name is "com.android.build.api.attributes.BuildTypeAttr", so we omit it if there's just the single variant and always for the release one:
        valueString == "release" -> false
        isSinglePublishedVariant -> false
        else -> true
    }

    private fun filterOutAndroidAgpVersionAttribute(
        attribute: Attribute<*>
    ): Boolean = attribute.name != "com.android.build.api.attributes.AgpVersionAttr"
}
