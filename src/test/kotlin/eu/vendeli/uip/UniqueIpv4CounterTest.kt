package eu.vendeli.uip

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path

class UniqueIpv4CounterTests : AnnotationSpec() {
    private lateinit var tempDir: Path

    @BeforeAll
    fun setupSpec() {
        // Create temporary directory for test files
        tempDir = Files.createTempDirectory("ipv4test")
    }

    @AfterAll
    fun tearDownSpec() {
        // Clean up temporary files
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `empty file should yield zero unique addresses`() {
        val file = Files.createTempFile(tempDir, "empty", ".txt").toFile()
        val counter = UniqueIpv4Counter()
        counter.processFile(file.absolutePath)
        counter.getUniqueCount() shouldBe 0L
    }

    @Test
    fun `single address file should yield one unique address`() {
        val file = tempDir.resolve("single.txt").toFile()
        file.writeText("192.168.1.1\n")
        val counter = UniqueIpv4Counter()
        counter.processFile(file.absolutePath)
        counter.getUniqueCount() shouldBe 1L
    }

    @Test
    fun `multiple different addresses should be counted correctly`() {
        val addresses = listOf(
            "10.0.0.1",
            "10.0.0.2",
            "255.255.255.255",
            "0.0.0.0"
        )
        val file = tempDir.resolve("multiple.txt").toFile()
        file.writeText(addresses.joinToString("\n") + "\n")

        val counter = UniqueIpv4Counter()
        counter.processFile(file.absolutePath)
        counter.getUniqueCount() shouldBe addresses.size.toLong()
    }

    @Test
    fun `duplicate addresses should count only once`() {
        val addresses = listOf(
            "8.8.8.8",
            "8.8.8.8",
            "8.8.4.4",
            "8.8.8.8",
            "8.8.4.4"
        )
        val file = tempDir.resolve("duplicates.txt").toFile()
        file.writeText(addresses.joinToString("\n") + "\n")

        val counter = UniqueIpv4Counter()
        counter.processFile(file.absolutePath)
        counter.getUniqueCount() shouldBe 2L
    }

    @Test
    fun `addresses spanning multiple prefix buckets`() {
        // Create addresses that fall into different high-16-bit buckets
        val addresses = listOf(
            "1.2.3.4",    // prefix 0x0102
            "1.2.255.255",// prefix 0x0102
            "2.0.0.1",    // prefix 0x0200
            "2.0.0.2"
        )
        val file = tempDir.resolve("buckets.txt").toFile()
        file.writeText(addresses.joinToString("\n") + "\n")

        val counter = UniqueIpv4Counter()
        counter.processFile(file.absolutePath)
        counter.getUniqueCount() shouldBe addresses.size.toLong()
    }

    @Test
    fun `large file simulation performance`() {
        // Generate a large number of entries with some duplicates
        val sb = StringBuilder()
        repeat(100_000) {
            sb.append("192.0.2.${it % 256}\n")
        }
        sb.append("203.0.113.5\n")
        val file = tempDir.resolve("large.txt").toFile()
        file.writeText(sb.toString())

        val counter = UniqueIpv4Counter()
        counter.processFile(file.absolutePath)
        // Unique: 256 + 1 = 257
        counter.getUniqueCount() shouldBe 257L
    }
}
