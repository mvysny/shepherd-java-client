package com.github.mvysny.shepherd.api

import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.ProcessResult

/**
 * Runs given [command], awaits until the command succeeds, then returns stdout. Fails with an exception on non-zero exit code.
 *
 * Example: `exec("ls", "-la")`
 */
internal fun exec(vararg command: String): String {
    val result: ProcessResult = ProcessExecutor().command(*command)
        .readOutput(true)
        .execute()
    require(result.exitValue == 0) { "${command.joinToString(" ")} failed with exit code ${result.exitValue}: ${result.outputString()}" }
    return result.outputString()
}

private val whitespaces = "\\s+".toRegex()

internal fun String.splitByWhitespaces(): List<String> =
    split(whitespaces)
