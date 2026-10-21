package com.attentive.screens.processor

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Visibility

class ScreenCatalogProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        ScreenCatalogProcessor(environment.codeGenerator, environment.logger)
}

/**
 * Generates an `AttentiveScreenCatalog` named `<Type>Screens` for each enum or sealed type
 * annotated with `@AttentiveScreens`.
 *
 * Enum routes are keyed by `Entry.name`. Sealed routes are keyed by their serial name, which is
 * what type-safe navigation uses as the route: the `@SerialName` value, or the qualified class
 * name. Both are compile-time strings, so the keys survive R8.
 */
internal class ScreenCatalogProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation(ATTENTIVE_SCREENS)
            .filterIsInstance<KSClassDeclaration>()
            .forEach(::generate)
        return emptyList()
    }

    private fun generate(declaration: KSClassDeclaration) {
        val routes =
            when {
                declaration.classKind == ClassKind.ENUM_CLASS -> enumRoutes(declaration)
                Modifier.SEALED in declaration.modifiers -> sealedRoutes(declaration)
                else -> {
                    logger.error("@AttentiveScreens must be on an enum class or a sealed type", declaration)
                    return
                }
            }

        val visibility = declaration.getVisibility()
        if (visibility == Visibility.PRIVATE || visibility == Visibility.LOCAL) {
            logger.error("@AttentiveScreens cannot be on a private type", declaration)
            return
        }

        val packageName = declaration.packageName.asString()
        val relativeName = relativeName(declaration)
        val result =
            ScreenCatalogBuilder.build(
                packageName = packageName,
                objectName = relativeName.replace(".", "") + "Screens",
                sourceName = relativeName,
                isInternal = visibility == Visibility.INTERNAL,
                routes = routes,
            )

        when (result) {
            is CatalogResult.Failure -> result.errors.forEach { logger.error(it, declaration) }
            is CatalogResult.Success -> {
                val files = listOfNotNull(declaration.containingFile).toTypedArray()
                codeGenerator
                    .createNewFile(Dependencies(false, *files), packageName, result.catalog.objectName)
                    .bufferedWriter()
                    .use { it.write(ScreenCatalogRenderer.render(result.catalog)) }
            }
        }
    }

    private fun enumRoutes(declaration: KSClassDeclaration): List<RouteDeclaration> {
        val qualifiedName = declaration.qualifiedName?.asString() ?: return emptyList()
        return declaration.declarations
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.classKind == ClassKind.ENUM_ENTRY }
            .map { entry ->
                val entryName = entry.simpleName.asString()
                RouteDeclaration(
                    sourceName = "${relativeName(declaration)}.$entryName",
                    keyExpression = "$qualifiedName.$entryName.name",
                    routeName = entryName,
                    nameOverride = entry.screenNameOverride(),
                    ignored = entry.findAnnotation(IGNORE_SCREEN) != null,
                )
            }
            .toList()
    }

    private fun sealedRoutes(declaration: KSClassDeclaration): List<RouteDeclaration> =
        declaration.getSealedSubclasses()
            .flatMap { subclass ->
                if (Modifier.SEALED in subclass.modifiers) {
                    sealedRoutes(subclass).asSequence()
                } else {
                    sequenceOf(sealedRoute(subclass))
                }
            }
            .toList()

    private fun sealedRoute(subclass: KSClassDeclaration): RouteDeclaration {
        if (subclass.findAnnotation(SERIALIZABLE) == null) {
            logger.warn("${subclass.simpleName.asString()} is not @Serializable, so navigation cannot use it as a route", subclass)
        }
        val serialName =
            subclass.findAnnotation(SERIAL_NAME)?.stringArgument("value")
                ?: subclass.qualifiedName?.asString()
                ?: subclass.simpleName.asString()
        return RouteDeclaration(
            sourceName = relativeName(subclass),
            keyExpression = ScreenCatalogRenderer.stringLiteral(serialName),
            routeName = subclass.simpleName.asString(),
            nameOverride = subclass.screenNameOverride(),
            ignored = subclass.findAnnotation(IGNORE_SCREEN) != null,
        )
    }

    private fun relativeName(declaration: KSClassDeclaration): String {
        val qualifiedName = declaration.qualifiedName?.asString() ?: return declaration.simpleName.asString()
        val packageName = declaration.packageName.asString()
        return if (packageName.isEmpty()) qualifiedName else qualifiedName.removePrefix("$packageName.")
    }

    private fun KSAnnotated.screenNameOverride(): String? = findAnnotation(SCREEN_NAME)?.stringArgument("value")

    private fun KSAnnotated.findAnnotation(qualifiedName: String): KSAnnotation? =
        annotations.firstOrNull {
            it.shortName.asString() == qualifiedName.substringAfterLast('.') &&
                it.annotationType.resolve().declaration.qualifiedName?.asString() == qualifiedName
        }

    private fun KSAnnotation.stringArgument(name: String): String? = arguments.firstOrNull { it.name?.asString() == name }?.value as? String

    private companion object {
        const val ATTENTIVE_SCREENS = "com.attentive.androidsdk.screens.AttentiveScreens"
        const val SCREEN_NAME = "com.attentive.androidsdk.screens.ScreenName"
        const val IGNORE_SCREEN = "com.attentive.androidsdk.screens.IgnoreScreen"
        const val SERIALIZABLE = "kotlinx.serialization.Serializable"
        const val SERIAL_NAME = "kotlinx.serialization.SerialName"
    }
}
