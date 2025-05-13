package eu.vendeli.sqlparser.types

data class SelectQuery(
    val select: List<SelectItem>,
    val from: List<TableReference>,
    val joins: List<Join>,
    val where: Expression?,
    val groupBy: List<Expression>,
    val having: Expression?,
    val orderBy: List<Ordering>,
    val limit: Int?,
    val offset: Int?
) {
    fun toSql(): String {
        val selectPart = select.joinToString(", ") {
            val e = it.expr.toSql()
            if (it.alias != null) "$e AS ${it.alias}" else e
        }
        val fromPart = from.joinToString(", ") { it.toSql() }
        val joinPart = if (joins.isEmpty()) "" else " " + joins.joinToString(" ") { it.toSql() }
        val wherePart = where?.let { " WHERE ${it.toSql()}" } ?: ""
        val groupPart = if (groupBy.isEmpty()) "" else " GROUP BY " + groupBy.joinToString(", ") { it.toSql() }
        val havingPart = having?.let { " HAVING ${it.toSql()}" } ?: ""
        val orderPart = if (orderBy.isEmpty()) "" else " ORDER BY " + orderBy.joinToString(", ") { it.toSql() }
        val limitPart = limit?.let { " LIMIT $it" } ?: ""
        val offsetPart = offset?.let { " OFFSET $it" } ?: ""
        return "SELECT $selectPart FROM $fromPart$joinPart$wherePart$groupPart$havingPart$orderPart$limitPart$offsetPart"
    }
}