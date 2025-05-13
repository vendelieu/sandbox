package eu.vendeli.sqlparser.types

data class Ordering(val expr: Expression, val order: Order) {
    fun toSql() = "${expr.toSql()} $order"
}