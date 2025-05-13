package eu.vendeli.sqlparser.types

data class SelectItem(val expr: Expression, val alias: String?)