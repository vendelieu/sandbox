package eu.vendeli.dcopy

class Man(var name: String?, var age: Int, var favoriteBooks: MutableList<String?>?)

fun main() {
    // Original Man
    val original = Man(
        name = "John Doe",
        age = 35,
        favoriteBooks = mutableListOf("1984", "Brave New World", "The Hobbit")
    )

    // Perform deep copy
    val copy = original.deepCopy<Man>()

    println("=== Deep Copy Report ===")

    // Print original and copy
    println("Original object:")
    println("  name:          ${original.name}")
    println("  age:           ${original.age}")
    println("  favoriteBooks: ${original.favoriteBooks}")
    println("Copy object:")
    println("  name:          ${copy.name}")
    println("  age:           ${copy.age}")
    println("  favoriteBooks: ${copy.favoriteBooks}")

    // Mutate the copy’s fields to prove independence
    copy.name = "Jane Smith"
    copy.age = 28
    copy.favoriteBooks = mutableListOf("Pride and Prejudice")

    // Print report

    println("=== After mutation:")
    println("Original object:")
    println("  name:          ${original.name}")
    println("  age:           ${original.age}")
    println("  favoriteBooks: ${original.favoriteBooks}")
    println("Copy object:")
    println("  name:          ${copy.name}")
    println("  age:           ${copy.age}")
    println("  favoriteBooks: ${copy.favoriteBooks}")
    println()
    println("Reference equality checks:")
    println("  original === copy?                ${original === copy}")
    println("  original.favoriteBooks === copy.favoriteBooks?  ${original.favoriteBooks === copy.favoriteBooks}")
}
