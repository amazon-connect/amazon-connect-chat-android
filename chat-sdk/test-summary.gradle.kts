import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestResult


tasks.withType<Test> {
    testLogging {
        events = setOf(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.STANDARD_ERROR,
            TestLogEvent.STANDARD_OUT
        )
        exceptionFormat = TestExceptionFormat.FULL
    }

    afterSuite(KotlinClosure<TestDescriptor, TestResult, Unit>({ desc, result ->
        if (desc.parent == null) { // Will match the outermost suite
            val totalTests = result.testCount
            val passedTests = result.successfulTestCount
            val failedTests = result.failedTestCount
            val skippedTests = result.skippedTestCount
            val resultType = result.resultType

            val output = """


                ${Color.CYAN}─────────────────────────────────────────────────────────────────────────────────${Color.NONE}
                ${Color.CYAN}| ${Color.WHITE}Test Summary${Color.CYAN}                                                                  |
                ${Color.CYAN}|───────────────────────────────────────────────────────────────────────────────|${Color.NONE}
                ${Color.CYAN}| ${Color.WHITE}Total Tests   : ${Color.GREEN}$totalTests${Color.CYAN}                                                            |
                ${Color.CYAN}| ${Color.WHITE}Passed        : ${Color.GREEN}$passedTests${Color.CYAN}                                                            |
                ${Color.CYAN}| ${Color.WHITE}Failed        : ${Color.RED}$failedTests${Color.CYAN}                                                             |
                ${Color.CYAN}| ${Color.WHITE}Skipped       : ${Color.YELLOW}$skippedTests${Color.CYAN}                                                             |
                ${Color.CYAN}| ${Color.WHITE}Result        : ${when (resultType) {
                TestResult.ResultType.SUCCESS -> "${Color.GREEN}$resultType"
                TestResult.ResultType.FAILURE -> "${Color.RED}$resultType"
                TestResult.ResultType.SKIPPED -> "${Color.YELLOW}$resultType"
            }}${Color.CYAN}                                                       |
                ${Color.CYAN}─────────────────────────────────────────────────────────────────────────────────${Color.NONE}
            """.trimIndent()
            println(output)
        }
    }, this))
}

// Helper class to convert Groovy closure to Kotlin lambda
class KotlinClosure<in T1, in T2, out R>(
    private val function: (T1, T2) -> R,
    private val owner: Any? = null,
    private val thisObject: Any? = null
) : groovy.lang.Closure<@UnsafeVariance R>(owner, thisObject) {
    @Suppress("unused")
    fun doCall(var1: T1, var2: T2): R = function(var1, var2)
}

internal enum class Color(val ansiCode: String) {
    NONE("\u001B[0m"),
    BLACK("\u001B[30m"),
    RED("\u001B[31m"),
    GREEN("\u001B[32m"),
    YELLOW("\u001B[33m"),
    BLUE("\u001B[34m"),
    PURPLE("\u001B[35m"),
    CYAN("\u001B[36m"),
    WHITE("\u001B[37m");

    override fun toString(): String {
        return ansiCode
    }
}
