package com.lavaloon.zatca

import com.starkbank.ellipticcurve.utils.File
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.*

fun prepareCertForZatca(certFile: String): Path {
    val path = kotlin.io.path.createTempFile(prefix = "cert-base64")
    FileOutputStream(path.toFile()).use {
        val certBytes = File.readBytes(certFile)
        val encoded = Base64.getEncoder().encode(certBytes)
        it.write(encoded)
    }

    return path
}