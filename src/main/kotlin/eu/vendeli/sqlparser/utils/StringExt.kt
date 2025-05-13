package eu.vendeli.sqlparser.utils

import eu.vendeli.sqlparser.core.SqlParser.Companion.parseSelect

fun String.toSelectQuery() = parseSelect(this)