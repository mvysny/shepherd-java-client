package com.github.mvysny.shepherd.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.ProcessResult
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

/**
 * Runs given [command], awaits until the command succeeds, then returns stdout. Fails with an exception on non-zero exit code.
 *
 * Example: `exec("ls", "-la")`
 * @throws ExecException if the command fails
 */
internal fun exec(vararg command: String): String {
    val result: ProcessResult = ProcessExecutor().command(*command)
        .readOutput(true)
        .execute()
    if (result.exitValue != 0) {
        throw ExecException(command.toList(), result.exitValue, result.outputString())
    }
    return result.outputString()
}

/**
 * @property command the command that was run
 * @property exitValue [ProcessResult.exitValue]
 * @property output [ProcessResult.outputString]. Will include both stdout and stderr.
 */
public class ExecException(public val command: List<String>, public val exitValue: Int, public val output: String) : IOException("${command.joinToString(" ")} failed with exit code $exitValue: $output")

private val whitespaces = "\\s+".toRegex()

internal fun String.splitByWhitespaces(): List<String> =
    split(whitespaces)

/**
 * JSON serialization/deserialization utility class.
 */
@OptIn(ExperimentalSerializationApi::class)
public object JsonUtils {
    private val jsonPrettyPrint = Json { prettyPrint = true }
    public fun getJson(prettyPrint: Boolean): Json = if (prettyPrint) jsonPrettyPrint else Json

    public inline fun <reified T> fromJson(json: String): T = Json.decodeFromString(json)

    public inline fun <reified T> fromFile(file: Path): T = try {
        file.inputStream().use {
            Json.decodeFromStream<T>(it.buffered())
        }
    } catch (e: SerializationException) {
        throw IOException("Failed to decode $file as JSON: '${file.toFile().readText()}'", e)
    }

    /**
     * Saves [obj] as a JSON to given [file]. Pretty-prints the JSON by default;
     * override via the [prettyPrint] parameter.
     */
    public inline fun <reified T> saveToFile(obj: T, file: Path, prettyPrint: Boolean = true) {
        file.outputStream().use {
            it.buffered().use { b ->
                getJson(prettyPrint).encodeToStream<T>(obj, b)
            }
        }
    }

    public inline fun <reified T> toJson(obj: T, prettyPrint: Boolean = true): String =
        getJson(prettyPrint).encodeToString<T>(obj)
}

public fun String.containsWhitespaces(): Boolean = any { it.isWhitespace() }
