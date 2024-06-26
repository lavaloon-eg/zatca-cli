package com.lavaloon.zatca

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.zatca.sdk.dto.ApplicationPropertyDto
import com.zatca.sdk.service.CsrGenerationService

@Parameters(commandNames = ["csr"], commandDescription = "Generate a CSR for onboarding")
class CsrCommand(private val errorListener: LogEventListener) {
    @Parameter(names = ["-o", "--output-csr"], required = true)
    lateinit var outputFile: String

    @Parameter(names = ["-k", "--output-key"], required = true)
    lateinit var outputKey: String

    @Parameter(names = ["-c", "--config"], required = true)
    lateinit var config: String

    @Parameter(names = ["-p", "--pem"])
    var outputPem = false

    @Parameter(names = ["-s", "--simulation"], description = "Indicates that the CSR is for the simulation environment")
    var simulation: Boolean = false

    fun run() {
        val props = ApplicationPropertyDto()
        props.isGenerateCsr = true
        props.csrFileName = outputFile
        props.privateKeyFileName = outputKey
        props.csrConfigFileName = config
        props.isOutputPemFormat = outputPem
        props.isSimulation = simulation

        val service = CsrGenerationService()
        if (!service.generate(props))
            Err("Failed to generate CSR", errorListener.messages.toTypedArray())

        return Ok("CSR generated successfully")
    }
}
