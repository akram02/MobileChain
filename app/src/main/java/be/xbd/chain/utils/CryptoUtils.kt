package be.xbd.chain.utils

import org.apache.tomcat.util.buf.HexUtils
import java.security.MessageDigest
import java.util.*

fun String.toSha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val byteArray = digest.digest(this.toByteArray())
    val byteString = HexUtils.toHexString(byteArray)
    return byteString
}