package eu.vendeli.dcopy

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import java.lang.reflect.Proxy

@Suppress("UNCHECKED_CAST")
class DeepCopyTests : AnnotationSpec() {
    @Test
    fun `null remains null`() {
        null.deepCopy().shouldBeNull()
    }

    @Test
    fun `immutable types are reused`() {
        val str = "hello"
        str.deepCopy() shouldBeSameInstanceAs str

        val num = 42
        num.deepCopy() shouldBe num
    }

    @Test
    fun `arrays are deep-copied`() {
        val arr = arrayOf("a", "b", "c")
        val arrCopy = arr.deepCopy() as Array<String>
        arrCopy shouldContainExactly arr
        arrCopy shouldNotBeSameInstanceAs arr
    }

    @Test
    fun `lists are deep-copied including nested lists`() {
        val nested = listOf(listOf(1, 2), listOf(3, 4))
        val copy = nested.deepCopy() as List<List<Int>>
        copy shouldContainExactly nested
        copy shouldNotBeSameInstanceAs nested
        copy[0] shouldNotBeSameInstanceAs nested[0]
    }

    @Test
    fun `maps are deep-copied`() {
        val map = mapOf("x" to 1, "y" to 2)
        val mapCopy = map.deepCopy() as Map<String, Int>
        mapCopy shouldContainExactly map
        mapCopy shouldNotBeSameInstanceAs map
    }

    data class Person(var name: String, var age: Int)

    @Test
    fun `JavaBean objects are deep-copied`() {
        val p = Person("Alice", 30)
        val pCopy = p.deepCopy() as Person
        pCopy shouldBe p
        pCopy shouldNotBeSameInstanceAs p
    }

    class Node(var value: Int) {
        var next: Node? = null
    }

    @Test
    fun `cycles are preserved without sharing`() {
        val a = Node(1)
        val b = Node(2)
        a.next = b
        b.next = a
        val aCopy = a.deepCopy() as Node
        val bCopy = aCopy.next!!
        aCopy.value shouldBe 1
        bCopy.value shouldBe 2
        bCopy.next shouldBeSameInstanceAs aCopy
        aCopy shouldNotBeSameInstanceAs a
        bCopy shouldNotBeSameInstanceAs b
    }

    private interface Greeter {
        fun greet(): String
    }

    @Test
    fun `dynamic proxies preserve type`() {
        val handler = java.lang.reflect.InvocationHandler { _, _, _ -> "hello" }
        val proxy = Proxy.newProxyInstance(
            Greeter::class.java.classLoader,
            arrayOf(Greeter::class.java),
            handler
        ) as Greeter
        val proxyCopy = proxy.deepCopy() as Greeter
        proxyCopy.greet() shouldBe "hello"
        proxyCopy.javaClass shouldBe proxy.javaClass
        proxyCopy shouldNotBeSameInstanceAs proxy
    }
}