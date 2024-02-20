package com.lavaloon.zatca

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.beust.jcommander.Parameters
import com.gazt.einvoicing.signing.service.model.InvoiceSigningResult
import com.starkbank.ellipticcurve.utils.File
import com.zatca.config.ResourcesPaths
import com.zatca.config.ResourcesPaths.Paths
import com.zatca.sdk.dto.ApplicationPropertyDto
import com.zatca.sdk.service.CsrGenerationService
import com.zatca.sdk.service.InvoiceSigningService
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.log4j.Appender
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.spi.LoggingEvent
import java.io.FileOutputStream
import java.util.*
import kotlin.system.exitProcess

@Serializable
data class Result<TData>(val msg: String, val errors: Array<String>, val data: TData?)

@Serializable
data class SigningData(val hash: String, val qrCode: String)

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

@Parameters(commandNames = ["csr"], commandDescription = "Generate a CSR for onboarding")
class CsrCommand(private val errorListener: ErrorListener) {
    @Parameter(names = ["-o", "--output-csr"], required = true)
    lateinit var outputFile: String

    @Parameter(names = ["-k", "--output-key"], required = true)
    lateinit var outputKey: String

    @Parameter(names = ["-c", "--config"], required = true)
    lateinit var config: String

    @Parameter(names = ["-p", "--pem"])
    var outputPem = false

    fun run() {
        val props = ApplicationPropertyDto()
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

@Parameters(commandNames = ["sign"], commandDescription = "Generates a signed invoice from a source invoice")
class SignCommand(private val errorListener: ErrorListener) {
    @Parameter(names = ["-o", "--output"], required = true, description = "Output XML file (signed invoice)")
    lateinit var outputFile: String

    @Parameter(names = ["-c", "--cert"], required = true, description = "Signing certificate path")
    lateinit var certFile: String

    @Parameter(names = ["-k", "--private-key"], required = true, description = "Private key path")
    lateinit var privateKeyPath: String

    @Parameter(required = true, description = "Input XML file (invoice)")
    lateinit var inputFile: String


    fun run() {
        val props = ApplicationPropertyDto()
        props.isGenerateSignature = true
        props.isGenerateQr = true
        props.isGenerateHash = true
        props.invoiceFileName = inputFile
        props.outputInvoiceFileName = outputFile

        val path = kotlin.io.path.createTempFile(prefix = "cert-base64")
        FileOutputStream(path.toFile()).use {
            val certBytes = File.readBytes(certFile)
            val encoded = Base64.getEncoder().encode(certBytes)
            it.write(encoded)
        }

        val loader = ::InvoiceSigningService.javaClass.classLoader
        val paths = Paths()
        paths.xsdPth = loader.getResource("xsds/UBL2.1/xsd/maindoc/UBL-Invoice-2.1.xsd")!!.toString()
        paths.enSchematronPath = loader.getResource("schematrons/CEN-EN16931-UBL.xsl")!!.toString()
        paths.certificatePath = path.toString()
        paths.privateKeyPath = privateKeyPath
        ResourcesPaths.setPaths(paths)

        val signingService = InvoiceSigningService()
        if (!signingService.generate(props))
            Err("Failed to sign invoice", errorListener.errors.toTypedArray())

        // The signing service does not expose the result directly, and we don't want to rely on parsing text output
        // to extract the hash. The QR code is never output either, and we don't want to have to load the XML to
        // extract it
        val field = signingService::class.java.getDeclaredField("invoiceSigningResult")
        field.isAccessible = true
        val result = field.get(signingService) as InvoiceSigningResult
        return Ok("Invoice signed successfully", SigningData(result.invoiceHash, result.qrCode))
    }
}

class Options {
    @Parameter(names = ["-h", "--help"], description = "Display usage")
    var help: Boolean? = null

    @Parameter(names = ["-v", "--version"], description = "Display version")
    var version: Boolean? = null
}

class ErrorListener : AppenderSkeleton() {
    val errors = arrayListOf<String>()

    override fun close() {
        errors.clear()
    }

    override fun requiresLayout(): Boolean {
        return false
    }

    override fun append(event: LoggingEvent?) {
        if (event == null) return
        if (event.getLevel().toInt() < Level.ERROR.toInt()) return
        errors.add(event.message.toString())
    }
}

fun main(args: Array<String>) {
    try {
        val errorListener = ErrorListener()
        initLogging(errorListener)

        val options = Options()
        val csr = CsrCommand(errorListener)
        val sign = SignCommand(errorListener)

        val commander = JCommander.newBuilder()
            .programName("lava-zatca")
            .addObject(options)
            .addCommand(csr)
            .addCommand(sign)
            .build()

        commander.parse(*args)
        if (options.help == true) {
            val sb = StringBuilder()
            commander.usage(sb)
            Err(sb.toString())
        }

        if (options.version == true) {
            Ok("Version: 1.1.0")
        }

        when (commander.parsedCommand) {
            "csr" -> csr.run()
            "sign" -> sign.run()
            else -> {
                val sb = StringBuilder()
                commander.usage(sb)
                Err(sb.toString())
            }
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