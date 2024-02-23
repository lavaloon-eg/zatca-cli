package com.lavaloon.zatca

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.zatca.config.ResourcesPaths
import com.zatca.sdk.dto.ApplicationPropertyDto
import com.zatca.sdk.service.InvoiceValidationService
import kotlinx.serialization.Serializable
import java.io.FileOutputStream
import java.nio.file.Path
import java.nio.file.Paths

@Serializable
data class ValidateResult(val messages: Array<String>, val errorsAndWarnings: Array<String>)

fun writeTempFile(content: String): Path {
    val path = kotlin.io.path.createTempFile(prefix = "pih")
    FileOutputStream(path.toFile()).use {
        it.write(content.encodeToByteArray())
    }

    return path
}

@Parameters(commandNames = ["validate"], commandDescription = "Validate an invoice")
class ValidateCommand(private val infoListener: LogEventListener, private val errorListener: LogEventListener) {
    @Parameter(names = ["-c", "--cert"], required = true, description = "Signing certificate path")
    lateinit var certFile: String

    @Parameter(names = ["-p", "--previous-invoice-hash"], required = true, description = "Previous invoice hash")
    lateinit var previousInvoiceHash: String

    @Parameter(
        names = ["-b", "--base-path"],
        required = true,
        description = "Base path of lava-zatca, under which xsd and schematrons can be found"
    )
    lateinit var basePath: String

    @Parameter(required = true, description = "Input XML file (invoice)")
    lateinit var inputFile: String


    fun run() {
        val props = ApplicationPropertyDto()
        props.isValidateInvoice = true
        props.invoiceFileName = inputFile

        val paths = ResourcesPaths.Paths()
        paths.xsdPth = Paths.get(basePath, "xsd/maindoc/UBL-Invoice-2.1.xsd").toString()
        paths.enSchematronPath = Paths.get(basePath, "schematrons/CEN-EN16931-UBL.xsl").toString()
        paths.zatcaSchematronPath =
            Paths.get(basePath, "schematrons/20210819_ZATCA_E-invoice_Validation_Rules.xsl").toString()
        paths.certificatePath = prepareCertForZatca(certFile).toString()
        paths.pihPath = writeTempFile(previousInvoiceHash).toString()
        ResourcesPaths.setPaths(paths)

        val service = InvoiceValidationService()
        if (!service.generate(props))
            Err("Failed to validate invoice", errorListener.messages.toTypedArray())

        return Ok(
            "Invoice validated",
            ValidateResult(infoListener.messages.toTypedArray(), errorListener.messages.toTypedArray())
        )
    }
}