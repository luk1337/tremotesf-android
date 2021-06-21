package org.equeim.tremotesf.gradle.tasks

import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

internal object ExecUtils {
    const val MAKE = "make"
    fun defaultMakeArguments(gradle: Gradle) = listOf("-j${gradle.startParameter.maxWorkerCount}")

    fun Task.exec(
        execOperations: ExecOperations,
        executable: String,
        args: List<String>,
        workingDir: File,
        environmentVariables: Map<String, Any> = emptyMap(),
        ignoreExitValue: Boolean = false,
        dropEnvironmentVariables: ((String) -> Boolean)? = null
    ): ExecResult {
        var commandLine: List<String>? = null
        val outputStream = ByteArrayOutputStream()
        return try {
            outputStream.buffered().use { bufferedOutputStream ->
                execOperations.exec {
                    this.executable = executable
                    this.args = args
                    this.workingDir = workingDir

                    if (dropEnvironmentVariables != null) {
                        val iter = environment.iterator()
                        while (iter.hasNext()) {
                            if (dropEnvironmentVariables(iter.next().key)) {
                                iter.remove()
                            }
                        }
                    }
                    environment(environmentVariables)

                    isIgnoreExitValue = ignoreExitValue

                    standardOutput = bufferedOutputStream
                    errorOutput = bufferedOutputStream

                    commandLine = this.commandLine
                }.rethrowFailure()
            }
        } catch (e: Exception) {
            logger.error("Failed to execute $commandLine: $e")
            logger.error("Output:")
            outputStream.writeTo(System.err)
            throw e
        }
    }

    fun isNdkEnvironmentVariable(name: String) = name.startsWith("ANDROID_NDK")
}

internal fun nanosToSecondsString(nanoseconds: Long): String {
    return "%.2f".format(Locale.ROOT, nanoseconds.toDouble() / TimeUnit.SECONDS.toNanos(1).toDouble())
}
