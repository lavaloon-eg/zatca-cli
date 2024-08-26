package com.lavaloon.zatca

import com.beust.jcommander.JCommander
import com.beust.jcommander.MissingCommandException
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.lavaloon.zatca_cli.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.log4j.Appender
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.spi.LoggingEvent
import kotlin.system.exitProcess

@Serializable
data class Result<TData>(val msg: String, val errors: Array<String>, val data: TData?)

@Serializable
data class VersionResult(val version: String)

inline fun <reified TData> Ok(msg: String, data: TData) {
    println(Json.encodeToString(Result(msg, arrayOf(), data)))
    exitProcess(0)
}

fun Ok(msg: String) {
    println(Json.encodeToString(Result(msg, arrayOf(), null)))
    exitProcess(0)
}

fun Err(msg: String, errors: Array<String> = arrayOf(), exitCode: Int = 1) {
    println(Json.encodeToString(Result(msg, errors, null)))
    exitProcess(exitCode)
}


class Options {
    @Parameter(names = ["-h", "--help"], description = "Display usage")
    var help: Boolean? = null

    @Parameter(names = ["-v", "--version"], description = "Display version")
    var version: Boolean? = null
}

class LogEventListener(private val levels: Array<Level>) : AppenderSkeleton() {
    val messages = arrayListOf<String>()

    override fun close() {
        messages.clear()
    }

    override fun requiresLayout(): Boolean {
        return false
    }

    override fun append(event: LoggingEvent?) {
        if (event == null) return
        if (levels.contains(event.getLevel()))
            messages.add(event.message.toString())
    }
}

fun main(args: Array<String>) {
    try {
        val infoListener = LogEventListener(arrayOf(Level.INFO))
        val errorListener = LogEventListener(arrayOf(Level.WARN, Level.ERROR, Level.FATAL))
        initLogging(arrayOf(infoListener, errorListener))

        val options = Options()
        val csr = CsrCommand(errorListener)
        val sign = SignCommand(errorListener)
        val validate = ValidateCommand(infoListener, errorListener)

        val commander = JCommander.newBuilder()
            .programName("zatca-cli")
            .addObject(options)
            .addCommand(csr)
            .addCommand(sign)
            .addCommand(validate)
            .build()

        commander.parse(*args)
        if (options.help == true) {
            val sb = StringBuilder()
            commander.usage(sb)
            Err(sb.toString())
        }

        if (options.version == true) {
            val version = BuildConfig.APP_VERSION
            Ok("Version $version", VersionResult(version))
        }

        when (commander.parsedCommand) {
            "csr" -> csr.run()
            "sign" -> sign.run()
            "validate" -> validate.run()
            else -> {
                val sb = StringBuilder()
                commander.usage(sb)
                Err(sb.toString())
            }
        }

        Err("Internal error")
    } catch (e: MissingCommandException) {
        Err("Unknown command: ${e.unknownCommand}\nConsider updating ZATCA-CLI to the latest version")
    } catch (e: ParameterException) {
        Err(e.message!!)
    } catch (e: Exception) {
        Err("An unexpected error occurred", arrayOf(e.message ?: "No exception message", e.stackTraceToString()))
    }
}

fun initLogging(appenders: Array<Appender>) {
    val rootLogger = Logger.getRootLogger()

    // By default, the root logger logs to the console which interferes with our desire to only return JSON
    rootLogger.removeAllAppenders()
    for (appender in appenders)
        rootLogger.addAppender(appender)
}