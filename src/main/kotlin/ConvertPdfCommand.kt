package com.lavaloon.zatca

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import kotlinx.serialization.Serializable
import java.io.File

import org.apache.pdfbox.pdmodel.common.PDStream
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.*
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.springframework.core.io.ResourceLoader
import java.io.FileInputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class PdfAFile(val filePath: String)

@Parameters(commandNames = ["convert-pdf"], commandDescription = "Converts a PDF to PDF/A-3b")
class ConvertPdfCommand(private val errorListener: LogEventListener) {
    @Parameter(names = ["-i", "--input"], required = true, description = "Input PDF file")
    lateinit var inputFile: String

    @Parameter(names = ["-xml"], required = true, description = "XML To Attach")
    lateinit var xmlFile: String

    fun run() {
        val inputFile = File(inputFile)
        val xmlFile = File(xmlFile)

        if (!inputFile.exists())
            return Err("Input File Does not exists: ${inputFile.absolutePath}")
        val outputFile = convertToPdfA3B(inputFile, xmlFile)
        return Ok("PDF converted successfully: ${outputFile.absolutePath}", PdfAFile(outputFile.absolutePath))
    }

    private fun convertToPdfA3B(inputFile: File, xmlFile: File): File {
        val doc: PDDocument = PDDocument.load(inputFile);
        val colorPFile: File = File("/Users/mostafaessam/Desktop/Lavaloon/zatca/zatca-bench/apps/zatca-cli/src/main/resources/sRGB Color Space Profile.icm")
        val colorProfile  = FileInputStream(colorPFile)
        val cat: PDDocumentCatalog = makeA3Compliant(doc);
        attachXML(doc, xmlFile)

        // Add XMP metadata
        val xmpMetadata = """
        <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.6.0">
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about="" xmlns:pdfaid="http://www.aiim.org/pdfa/ns/id/">
                    <pdfaid:part>3</pdfaid:part>
                    <pdfaid:conformance>B</pdfaid:conformance>
                </rdf:Description>
            </rdf:RDF>
        </x:xmpmeta>
    """.trimIndent()

        val metadata = PDMetadata(doc)
        metadata.importXMPMetadata(xmpMetadata.toByteArray(StandardCharsets.UTF_8))
        cat.metadata = metadata
        val outputIntent = PDOutputIntent(doc, colorProfile)
        outputIntent.info = "sRGB IEC61966-2.1"
        outputIntent.outputCondition = "sRGB IEC61966-2.1"
        outputIntent.outputConditionIdentifier = "sRGB IEC61966-2.1"
        outputIntent.registryName = "http://www.color.org"
        cat.outputIntents.add(outputIntent)
        val fontFile = File("/Users/mostafaessam/Desktop/Lavaloon/zatca/zatca-bench/apps/zatca-cli/src/main/resources/Helvetica.ttf")
        doc.version = 1.7f;
        doc.save(inputFile.absolutePath)
        doc.close()
        colorProfile.close()
        return inputFile
    }

    private fun makeA3Compliant(doc: PDDocument): PDDocumentCatalog {
        val cat: PDDocumentCatalog = doc.documentCatalog;
        val pdd: PDDocumentInformation = doc.documentInformation;
        val metadata = PDMetadata(doc);
        cat.metadata = metadata;
        val pdi = PDDocumentInformation()
        pdi.producer = pdd.producer ?: "Unknown Producer"
        pdi.author = pdd.author ?: "Unknown Author"
        pdi.title = pdd.title ?: "Untitled"
        pdi.subject = pdd.subject ?: "No Subject"
        pdi.keywords = pdd.keywords ?: "No Keywords"
        doc.documentInformation = pdi;
        return cat
    }

    private fun attachXML(doc: PDDocument, xmlFile: File){
        val efTree = PDEmbeddedFilesNameTreeNode();
        val subType: String = Files.probeContentType(xmlFile.toPath()) ?: "application/octet-stream"
        val fileName: String = xmlFile.name
        val complexFileSpec: PDComplexFileSpecification = PDComplexFileSpecification()
        complexFileSpec.file = fileName
        val cosDict: COSDictionary = complexFileSpec.cosObject
        cosDict.setName("AFRelationship", "Source")
        cosDict.setString("UF", fileName)
        val inputStream = FileInputStream(xmlFile)
        val embeddedFile = PDEmbeddedFile(doc, inputStream)
        val ksaDate = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"))
        embeddedFile.modDate = ksaDate
        embeddedFile.size = embeddedFile.length
        embeddedFile.creationDate = embeddedFile.modDate
        complexFileSpec.embeddedFile = embeddedFile
        embeddedFile.subtype = subType
        efTree.names = Collections.singletonMap(fileName, complexFileSpec)
        val catalog = doc.documentCatalog
        val names = PDDocumentNameDictionary(doc.documentCatalog)
        names.embeddedFiles = efTree
        catalog.names = names
        val catCosDict = catalog.cosObject
        val afArray = COSArray()
        afArray.add(complexFileSpec.cosObject)
        catCosDict.setItem("AF", afArray)
    }

    private fun getResourceFile(resourceName: String): File? {
        val resourceUrl = ResourceLoader::class.java.classLoader.getResource(resourceName)
        if (resourceUrl != null) {
            return File(resourceUrl.toURI())
        }
        return null
    }
}
