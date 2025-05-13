package eu.vendeli.sqlparser.types

sealed class TableReference {
    abstract fun toSql(): String
}
data class TableName(val name: String, val alias: String?): TableReference() {
    override fun toSql() = name + (alias?.let { " AS $it" } ?: "")
}
data class SubqueryTable(val query: SelectQuery, val alias: String): TableReference() {
    override fun toSql() = "(${query.toSql()}) AS $alias"
}

fun TableReference.asTableName() = this as TableName
fun TableReference.asSubqueryTable() = this as SubqueryTable