package com.attentive.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.getParentOfType

class SdkInitializationDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(UCallExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                val methodName = node.methodName ?: return

                if (methodName != "initialize") return

                if (!isAttentiveSdkInitializeCall(node)) return

                if (!isInsideApplicationClass(node)) {
                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node),
                        "AttentiveSdk.initialize() should be called in the onCreate() method of " +
                            "your Application subclass to ensure proper SDK initialization.",
                    )
                }
            }
        }
    }

    private fun isAttentiveSdkInitializeCall(node: UCallExpression): Boolean {
        // Check via receiver (normal call style: AttentiveSdk.initialize())
        val receiverType = node.receiverType?.canonicalText
        val receiver = node.receiver?.asSourceString()

        if (receiverType == ATTENTIVE_SDK_CLASS ||
            receiver == "AttentiveSdk" ||
            receiver == ATTENTIVE_SDK_CLASS
        ) {
            return true
        }

        // Check via method resolution (handles static imports and imported functions)
        // e.g., `import static com.attentive.androidsdk.AttentiveSdk.initialize` in Java
        // or `import com.attentive.androidsdk.AttentiveSdk.initialize` in Kotlin
        val resolvedMethod = node.resolve() ?: return false
        val containingClass = resolvedMethod.containingClass ?: return false

        return containingClass.qualifiedName == ATTENTIVE_SDK_CLASS
    }

    private fun isInsideApplicationClass(node: UCallExpression): Boolean {
        val containingClass = node.getParentOfType<UClass>() ?: return false
        return isApplicationSubclass(containingClass)
    }

    private fun isApplicationSubclass(uClass: UClass): Boolean {
        val psiClass = uClass.javaPsi

        var superClass = psiClass.superClass
        while (superClass != null) {
            val qualifiedName = superClass.qualifiedName
            if (qualifiedName == "android.app.Application") {
                return true
            }
            superClass = superClass.superClass
        }

        return false
    }

    companion object {
        private const val ATTENTIVE_SDK_CLASS = "com.attentive.androidsdk.AttentiveSdk"

        @JvmField
        val ISSUE: Issue =
            Issue.create(
                id = "AttentiveSdkInitialization",
                briefDescription = "AttentiveSdk.initialize() called outside Application class",
                explanation = """
                The Attentive SDK must be initialized in the `onCreate()` method of your \
                `Application` subclass. Initializing the SDK elsewhere (such as in an Activity, \
                Fragment, or other component) can lead to initialization issues, crashes, or \
                unexpected behavior.

                Move the `AttentiveSdk.initialize()` call to your Application class's `onCreate()` \
                method to ensure the SDK is properly initialized before any other component \
                attempts to use it.

                Example:
                ```kotlin
                class MyApplication : Application() {
                    override fun onCreate() {
                        super.onCreate()
                        val config = AttentiveConfig.Builder()
                            .applicationContext(this)
                            .domain("your-domain")
                            .mode(AttentiveConfig.Mode.PRODUCTION)
                            .build()
                        AttentiveSdk.initialize(config)
                    }
                }
                ```
            """,
                category = Category.CORRECTNESS,
                priority = 8,
                severity = Severity.WARNING,
                implementation =
                    Implementation(
                        SdkInitializationDetector::class.java,
                        Scope.JAVA_FILE_SCOPE,
                    ),
            )
    }
}
