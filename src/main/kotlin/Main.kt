package com.lavaloon.zatca

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.beust.jcommander.Parameters
import com.zatca.sdk.dto.ApplicationPropertyDto
import com.zatca.sdk.service.CsrGenerationService
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.log4j.Appender
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.spi.LoggingEvent
import kotlin.reflect.typeOf

@Serializable
data class Result(val msg: String, val errors: Array<String>)

fun Ok(msg: String) {
    System.out.println(Json.encodeToString(Result(msg, arrayOf())))
    System.exit(0)
}

fun Err(msg: String, errors: Array<String> = arrayOf(), exitCode: Int = 1) {
    System.out.println(Json.encodeToString(Result(msg, errors)))
    System.exit(exitCode)
}

@Parameters(commandNames = arrayOf("csr"), commandDescription = "Generate a CSR for onboarding")
class CsrCommand(val errorListener: ErrorListener) {
    @Parameter(names = arrayOf("-o", "--output-csr"), required = true)
    lateinit var outputFile: String

    @Parameter(names = arrayOf("-k", "--output-key"), required = true)
    lateinit var outputKey: String

    @Parameter(names = arrayOf("-c", "--config"), required = true)
    lateinit var config: String

    @Parameter(names = arrayOf("-p", "--pem"))
    var outputPem = false

    fun run() {
        val props = ApplicationPropertyDto();
        props.isGenerateCsr = true
        props.csrFileName = outputFile
        props.privateKeyFileName = outputKey
        props.csrConfigFileName = config
        props.isOutputPemFormat = outputPem

        val service = CsrGenerationService()
        if (!service.generate(props))
            Err("Failed to generate CSR", errorListener.errors.toTypedArray())

        return Ok("CSR generated successfully")
    }
}

class Options {
    @Parameter(names = arrayOf("-h", "--help"), description = "Display usage")
    var help: Boolean? = null

    @Parameter(names = arrayOf("-v", "--version"), description = "Display version")
    var version: Boolean? = null
}

class ErrorListener : AppenderSkeleton() {
    val errors = arrayListOf<String>()

    override fun close() {
        errors.clear()
    }

    override fun requiresLayout(): Boolean {
        return false;
    }

    override fun append(event: LoggingEvent?) {
        if (event == null) return;
        if (event.getLevel().toInt() < Level.ERROR.toInt()) return;
        errors.add(event.message.toString())
    }
}

fun main(args: Array<String>) {
    try {
        val errorListener = ErrorListener()
        initLogging(errorListener)

        val options = Options()
        val csr = CsrCommand(errorListener)

        val commander = JCommander.newBuilder()
            .programName("lava-zatca")
            .addObject(options)
            .addCommand(csr)
            .build()

        commander.parse(*args)
        if (options.help == true) {
            val sb = StringBuilder()
            commander.usage(sb)
            Err(sb.toString())
        }

        if (options.version == true) {
            Ok("Version: 1.0.0")
        }

        when (commander.getParsedCommand()) {
            "csr" -> csr.run()
            else -> {
                val sb = StringBuilder()
                commander.usage(sb)
                Err(sb.toString())
            };
        }

        Err("Internal error")
    } catch (e: ParameterException) {
        Err(e.message!!)
    } catch (e: Exception) {
        Err("An unexpected error occurred", arrayOf(e.message ?: "No exception message", e.stackTraceToString()))
    }
}

fun initLogging(appender: Appender) {
    val rootLogger = Logger.getRootLogger()

    // By default, the root logger logs to the console which interferes with our desire to only return JSON
    rootLogger.removeAllAppenders()
    rootLogger.addAppender(appender)
}