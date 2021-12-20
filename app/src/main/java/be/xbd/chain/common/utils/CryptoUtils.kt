package be.xbd.chain.common.utils

import org.apache.tomcat.util.buf.HexUtils
import java.security.MessageDigest

fun String.toSha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val byteArray = digest.digest(this.toByteArray())
    val byteString = HexUtils.toHexString(byteArray)
    return byteString
}