package eu.vendeli.uip

import java.io.File

class UniqueIpv4Counter {
    private val bitsPerBucket = 1 shl 16
    private val bytesPerBucket = bitsPerBucket / 8
    private val bucketCount = 1 shl 16
    private val buckets = arrayOfNulls<ByteArray>(bucketCount)

    private fun parseIpv4ToInt(s: String): Int {
        var result = 0
        var octet = 0
        for (c in s) {
            if (c == '.') {
                result = (result shl 8) or octet
                octet = 0
            } else {
                octet = octet * 10 + (c.code - '0'.code)
            }
        }
        return (result shl 8) or octet
    }

    fun processFile(path: String) {
        File(path).bufferedReader().use { reader ->
            reader.forEachLine { line ->
                if (line.isEmpty()) return@forEachLine
                val ipInt = parseIpv4ToInt(line.trim())
                val prefix = ipInt ushr 16
                val offset = ipInt and 0xFFFF

                // Allocate bucket on first use
                if (buckets[prefix] == null) {
                    buckets[prefix] = ByteArray(bytesPerBucket)
                }
                val bucket = buckets[prefix]!!

                val byteIndex = offset ushr 3
                val bitMask = (1 shl (offset and 7))
                bucket[byteIndex] = (bucket[byteIndex].toInt() or bitMask).toByte()
            }
        }
    }

    fun getUniqueCount(): Long {
        var count = 0L
        for (bucket in buckets) {
            if (bucket == null) continue
            for (b in bucket) {
                count += Integer.bitCount(b.toInt() and 0xFF)
            }
        }
        return count
    }
}