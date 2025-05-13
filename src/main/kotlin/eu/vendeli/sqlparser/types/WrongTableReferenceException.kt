package eu.vendeli.sqlparser.types

class WrongTableReferenceException(override val message: String? = null) : RuntimeException()