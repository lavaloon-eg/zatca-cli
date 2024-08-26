package com.lavaloon.zatca

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.zatca.config.ResourcesPaths
import com.zatca.sdk.service.flow.ValidationProcessorImpl
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.file.Paths

@Serializable
data class ValidationDetails(
    val isValid: Boolean,
    val isValidQr: Boolean,
    val isValidSignature: Boolean,
    val errors: Map<String, String>,
    val warnings: Map<String, String>
)

@Serializable
data class ValidateResult(
    val details: ValidationDetails,
    val messages: Array<String>,
    val errorsAndWarnings: Array<String>
)


@Parameters(commandNames = ["validate"], commandDescription = "Validate an invoice")
class ValidateCommand(private val infoListener: LogEventListener, private val errorListener: LogEventListener) {
    @Parameter(names = ["-c", "--cert"], required = true, description = "Signing certificate path")
    lateinit var certFile: String

    @Parameter(names = ["-p", "--previous-invoice-hash"], required = true, description = "Previous invoice hash")
    lateinit var previousInvoiceHash: String

    @Parameter(
        names = ["-b", "--base-path"],
        required = true,
        description = "Base path of zatca-cli, under which xsd and schematrons can be found"
    )
    lateinit var basePath: String

    @Parameter(required = true, description = "Input XML file (invoice)")
    lateinit var inputFile: String

    fun run() {
        val cert = File(certFile)
        if (!cert.exists())
            return Err("Certificate file does not exist: ${cert.absolutePath}")

        val invoice = File(inputFile)
        if (!invoice.exists())
            return Err("Invoice file does not exist: ${invoice.absolutePath}")

        val paths = ResourcesPaths.Paths()
        paths.xsdPth = Paths.get(basePath, "xsd/maindoc/UBL-Invoice-2.1.xsd").toString()
        paths.enSchematronPath = Paths.get(basePath, "schematrons/CEN-EN16931-UBL.xsl").toString()
        paths.zatcaSchematronPath =
            Paths.get(basePath, "schematrons/20210819_ZATCA_E-invoice_Validation_Rules.xsl").toString()
        paths.certificatePath = prepareCertForZatca(certFile).toString()
        paths.pihPath = writeTempFile("pih", previousInvoiceHash).toString()
        ResourcesPaths.setPaths(paths)

        val validator = ValidationProcessorImpl()
        val result = validator.run(invoice.readText())
        Ok(
            "Invoice validated",
            ValidateResult(
                details = ValidationDetails(
                    isValid = result.isValid,
                    isValidQr = result.isValidQrCode,
                    isValidSignature = result.isValidSignature,
                    errors = result.error ?: HashMap<String, String>(),
                    warnings = result.warning ?: HashMap<String, String>()
                ),
                messages = infoListener.messages.toTypedArray(),
                errorsAndWarnings = errorListener.messages.toTypedArray(),
            )
        )
    }
}
