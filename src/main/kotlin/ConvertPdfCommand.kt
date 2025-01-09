package com.lavaloon.zatca

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.spire.pdf.conversion.PdfStandardsConverter
import kotlinx.serialization.Serializable
import org.apache.pdfbox.cos.COSArray
import org.apache.pdfbox.cos.COSDictionary
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.util.*

@Serializable
data class PdfAFile(val filePath: String)

@Parameters(commandNames = ["convert-pdf"], commandDescription = "Converts a PDF to PDF/A-3b")
class ConvertPdfCommand {
    @Parameter(names = ["-x", "--xml"], required = true, description = "XML To Attach")
    lateinit var xmlFile: String

    @Parameter(names = ["-i", "--invoice"], required = true, description = "Invoice ID")
    lateinit var invoice: String

    @Parameter(required = true, description = "Input PDF file")
    lateinit var inputFile: String

    fun run() {
        val inputFile = File(inputFile)
        if (!inputFile.exists())
            return Err("Input file does not exist: ${inputFile.absolutePath}")

        val xmlFile = File(xmlFile)
        if (!xmlFile.exists())
            return Err("XML file does not exist: ${xmlFile.absolutePath}")

        val pathWithAttachment = attachXml(inputFile, xmlFile, invoice)
        val outPath = kotlin.io.path.createTempFile(suffix = "_a3b.pdf").toString()
        val converter = PdfStandardsConverter(pathWithAttachment)
        converter.toPdfA3B(outPath)

        return Ok("PDF converted successfully: $outPath", PdfAFile(outPath))
    }

    private fun attachXml(inputFile: File, xmlFile: File, invoice: String): String {
        val doc = PDDocument.load(inputFile)
        val efTree = PDEmbeddedFilesNameTreeNode()
        val subType = Files.probeContentType(xmlFile.toPath()) ?: "application/octet-stream"
        val complexFileSpec = PDComplexFileSpecification()
        val attachmentFileName = "$invoice.xml"
        complexFileSpec.file = attachmentFileName
        val cosDict: COSDictionary = complexFileSpec.cosObject
        cosDict.setName("AFRelationship", "Source")
        cosDict.setString("UF", attachmentFileName)
        val inputStream = FileInputStream(xmlFile)
        val embeddedFile = PDEmbeddedFile(doc, inputStream)
        val ksaDate = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"))
        embeddedFile.modDate = ksaDate
        embeddedFile.size = embeddedFile.length
        embeddedFile.creationDate = embeddedFile.modDate
        complexFileSpec.embeddedFile = embeddedFile
        embeddedFile.subtype = subType
        efTree.names = Collections.singletonMap(attachmentFileName, complexFileSpec)
        val catalog = doc.documentCatalog
        val names = PDDocumentNameDictionary(doc.documentCatalog)
        names.embeddedFiles = efTree
        catalog.names = names
        val catCosDict = catalog.cosObject
        val afArray = COSArray()
        afArray.add(complexFileSpec.cosObject)
        catCosDict.setItem("AF", afArray)

        val path = kotlin.io.path.createTempFile(suffix = "-attached.pdf").toString()
        doc.save(File(path))
        return path
    }
}
