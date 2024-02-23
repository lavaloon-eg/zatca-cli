package com.lavaloon.zatca

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.gazt.einvoicing.signing.service.model.InvoiceSigningResult
import com.zatca.config.ResourcesPaths
import com.zatca.sdk.dto.ApplicationPropertyDto
import com.zatca.sdk.service.InvoiceSigningService
import kotlinx.serialization.Serializable
import java.nio.file.Paths

@Serializable
data class SigningData(val hash: String, val qrCode: String)

@Parameters(commandNames = ["sign"], commandDescription = "Generates a signed invoice from a source invoice")
class SignCommand(private val errorListener: LogEventListener) {
    @Parameter(names = ["-o", "--output"], required = true, description = "Output XML file (signed invoice)")
    lateinit var outputFile: String

    @Parameter(names = ["-c", "--cert"], required = true, description = "Signing certificate path")
    lateinit var certFile: String

    @Parameter(names = ["-k", "--private-key"], required = true, description = "Private key path")
    lateinit var privateKeyPath: String

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
        props.isGenerateSignature = true
        props.isGenerateQr = true
        props.isGenerateHash = true
        props.invoiceFileName = inputFile
        props.outputInvoiceFileName = outputFile

        val paths = ResourcesPaths.Paths()
        paths.xsdPth = Paths.get(basePath, "xsd/maindoc/UBL-Invoice-2.1.xsd").toString()
        paths.enSchematronPath = Paths.get(basePath, "schematrons/CEN-EN16931-UBL.xsl").toString()
        paths.zatcaSchematronPath =
            Paths.get(basePath, "schematrons/20210819_ZATCA_E-invoice_Validation_Rules.xsl").toString()
        paths.certificatePath = prepareCertForZatca(certFile).toString()
        paths.privateKeyPath = privateKeyPath
        ResourcesPaths.setPaths(paths)

        val signingService = InvoiceSigningService()
        if (!signingService.generate(props))
            Err("Failed to sign invoice", errorListener.messages.toTypedArray())

        // The signing service does not expose the result directly, and we don't want to rely on parsing text output
        // to extract the hash. The QR code is never output either, and we don't want to have to load the XML to
        // extract it
        val field = signingService::class.java.getDeclaredField("invoiceSigningResult")
        field.isAccessible = true
        val result = field.get(signingService) as InvoiceSigningResult
        return Ok("Invoice signed successfully", SigningData(result.invoiceHash, result.qrCode))
    }
}
