package com.attentive.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

class SdkInitializationDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = SdkInitializationDetector()

    override fun getIssues(): List<Issue> = listOf(SdkInitializationDetector.ISSUE)

    private val attentiveSdkStub: TestFile =
        kotlin(
            """
        package com.attentive.androidsdk

        object AttentiveSdk {
            @JvmStatic
            fun initialize(config: Any) {}
        }
        """,
        ).indented()

    private val attentiveSdkJavaStub: TestFile =
        java(
            """
        package com.attentive.androidsdk;

        public class AttentiveSdk {
            public static void initialize(Object config) {}
        }
        """,
        ).indented()

    private val attentiveConfigStub: TestFile =
        kotlin(
            """
        package com.attentive.androidsdk

        class AttentiveConfig {
            class Builder {
                fun applicationContext(context: Any): Builder = this
                fun domain(domain: String): Builder = this
                fun mode(mode: Mode): Builder = this
                fun build(): AttentiveConfig = AttentiveConfig()
            }
            enum class Mode { DEBUG, PRODUCTION }
        }
        """,
        ).indented()

    @Test
    fun `test clean when initialize called in Application onCreate`() {
        lint()
            .files(
                attentiveSdkStub,
                attentiveConfigStub,
                kotlin(
                    """
                    package com.example.app

                    import android.app.Application
                    import com.attentive.androidsdk.AttentiveConfig
                    import com.attentive.androidsdk.AttentiveSdk

                    class MyApplication : Application() {
                        override fun onCreate() {
                            super.onCreate()
                            val config = AttentiveConfig.Builder()
                                .applicationContext(this)
                                .domain("test-domain")
                                .mode(AttentiveConfig.Mode.PRODUCTION)
                                .build()
                            AttentiveSdk.initialize(config)
                        }
                    }
                    """,
                ).indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun `test clean when initialize called in helper method of Application class`() {
        lint()
            .files(
                attentiveSdkStub,
                attentiveConfigStub,
                kotlin(
                    """
                    package com.example.app

                    import android.app.Application
                    import com.attentive.androidsdk.AttentiveConfig
                    import com.attentive.androidsdk.AttentiveSdk

                    class MyApplication : Application() {
                        override fun onCreate() {
                            super.onCreate()
                            initAttentiveSdk()
                        }

                        private fun initAttentiveSdk() {
                            val config = AttentiveConfig.Builder()
                                .applicationContext(this)
                                .domain("test-domain")
                                .mode(AttentiveConfig.Mode.PRODUCTION)
                                .build()
                            AttentiveSdk.initialize(config)
                        }
                    }
                    """,
                ).indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun `test warning when initialize called in Activity onCreate`() {
        lint()
            .files(
                attentiveSdkStub,
                attentiveConfigStub,
                kotlin(
                    """
                    package com.example.app

                    import android.app.Activity
                    import com.attentive.androidsdk.AttentiveConfig
                    import com.attentive.androidsdk.AttentiveSdk

                    class MainActivity : Activity() {
                        override fun onCreate(savedInstanceState: android.os.Bundle?) {
                            super.onCreate(savedInstanceState)
                            val config = AttentiveConfig.Builder()
                                .applicationContext(application)
                                .domain("test-domain")
                                .mode(AttentiveConfig.Mode.PRODUCTION)
                                .build()
                            AttentiveSdk.initialize(config)
                        }
                    }
                    """,
                ).indented(),
            )
            .run()
            .expect(
                """
                src/com/example/app/MainActivity.kt:15: Warning: AttentiveSdk.initialize() should be called in the onCreate() method of your Application subclass to ensure proper SDK initialization. [AttentiveSdkInitialization]
                        AttentiveSdk.initialize(config)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """.trimIndent(),
            )
    }

    @Test
    fun `test warning when initialize called in helper class`() {
        lint()
            .files(
                attentiveSdkStub,
                attentiveConfigStub,
                kotlin(
                    """
                    package com.example.app

                    import android.app.Application
                    import com.attentive.androidsdk.AttentiveConfig
                    import com.attentive.androidsdk.AttentiveSdk

                    class SdkHelper {
                        fun initializeSdk(application: Application) {
                            val config = AttentiveConfig.Builder()
                                .applicationContext(application)
                                .domain("test-domain")
                                .mode(AttentiveConfig.Mode.PRODUCTION)
                                .build()
                            AttentiveSdk.initialize(config)
                        }
                    }
                    """,
                ).indented(),
            )
            .run()
            .expect(
                """
                src/com/example/app/SdkHelper.kt:14: Warning: AttentiveSdk.initialize() should be called in the onCreate() method of your Application subclass to ensure proper SDK initialization. [AttentiveSdkInitialization]
                        AttentiveSdk.initialize(config)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """.trimIndent(),
            )
    }

    @Test
    fun `test warning when initialize called in Fragment`() {
        lint()
            .files(
                attentiveSdkStub,
                attentiveConfigStub,
                kotlin(
                    """
                    package com.example.app

                    import android.app.Fragment
                    import android.os.Bundle
                    import android.view.View
                    import com.attentive.androidsdk.AttentiveConfig
                    import com.attentive.androidsdk.AttentiveSdk

                    class MyFragment : Fragment() {
                        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                            super.onViewCreated(view, savedInstanceState)
                            val config = AttentiveConfig.Builder()
                                .applicationContext(activity?.application!!)
                                .domain("test-domain")
                                .mode(AttentiveConfig.Mode.PRODUCTION)
                                .build()
                            AttentiveSdk.initialize(config)
                        }
                    }
                    """,
                ).indented(),
            )
            .run()
            .expect(
                """
                src/com/example/app/MyFragment.kt:17: Warning: AttentiveSdk.initialize() should be called in the onCreate() method of your Application subclass to ensure proper SDK initialization. [AttentiveSdkInitialization]
                        AttentiveSdk.initialize(config)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """.trimIndent(),
            )
    }

    @Test
    fun `test warning when initialize called via static import in Java Activity`() {
        lint()
            .files(
                attentiveSdkJavaStub,
                java(
                    """
                    package com.example.app;

                    import android.app.Activity;
                    import android.os.Bundle;
                    import static com.attentive.androidsdk.AttentiveSdk.initialize;

                    public class MainActivity extends Activity {
                        @Override
                        protected void onCreate(Bundle savedInstanceState) {
                            super.onCreate(savedInstanceState);
                            initialize(new Object());
                        }
                    }
                    """,
                ).indented(),
            )
            .run()
            .expect(
                """
                src/com/example/app/MainActivity.java:11: Warning: AttentiveSdk.initialize() should be called in the onCreate() method of your Application subclass to ensure proper SDK initialization. [AttentiveSdkInitialization]
                        initialize(new Object());
                        ~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """.trimIndent(),
            )
    }

    @Test
    fun `test warning when initialize called via imported function in Kotlin Activity`() {
        lint()
            .files(
                attentiveSdkStub,
                attentiveConfigStub,
                kotlin(
                    """
                    package com.example.app

                    import android.app.Activity
                    import android.os.Bundle
                    import com.attentive.androidsdk.AttentiveSdk.initialize

                    class MainActivity : Activity() {
                        override fun onCreate(savedInstanceState: Bundle?) {
                            super.onCreate(savedInstanceState)
                            initialize(Object())
                        }
                    }
                    """,
                ).indented(),
            )
            .run()
            .expect(
                """
                src/com/example/app/MainActivity.kt:10: Warning: AttentiveSdk.initialize() should be called in the onCreate() method of your Application subclass to ensure proper SDK initialization. [AttentiveSdkInitialization]
                        initialize(Object())
                        ~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """.trimIndent(),
            )
    }

    @Test
    fun `test clean when initialize called via static import in Java Application`() {
        lint()
            .files(
                attentiveSdkJavaStub,
                java(
                    """
                    package com.example.app;

                    import android.app.Application;
                    import static com.attentive.androidsdk.AttentiveSdk.initialize;

                    public class MyApplication extends Application {
                        @Override
                        public void onCreate() {
                            super.onCreate();
                            initialize(new Object());
                        }
                    }
                    """,
                ).indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun `test clean when initialize called via imported function in Kotlin Application`() {
        lint()
            .files(
                attentiveSdkStub,
                attentiveConfigStub,
                kotlin(
                    """
                    package com.example.app

                    import android.app.Application
                    import com.attentive.androidsdk.AttentiveSdk.initialize

                    class MyApplication : Application() {
                        override fun onCreate() {
                            super.onCreate()
                            initialize(Object())
                        }
                    }
                    """,
                ).indented(),
            )
            .run()
            .expectClean()
    }
}
