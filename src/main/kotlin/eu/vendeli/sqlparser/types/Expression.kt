package eu.vendeli.sqlparser.types

sealed class Expression {
    abstract fun toSql(): String
}

data class Wildcard(val qualifier: String? = null) : Expression() {
    override fun toSql() = qualifier?.let { "$it.*" } ?: "*"
}

data class Column(val table: String?, val name: String) : Expression() {
    override fun toSql() = (table?.let { "$it." } ?: "") + name
}

data class Literal(val value: String) : Expression() {
    override fun toSql() = when {
        value.all { it.isDigit() } -> value
        else -> "'$value'"
    }
}

data class SubqueryExpression(val query: SelectQuery) : Expression() {
    override fun toSql() = "(${query.toSql()})"
}

data class FunctionCall(val name: String, val args: List<Expression>) : Expression() {
    override fun toSql() = "$name(${args.joinToString(", ") { it.toSql() }})"
}

data class BinaryOp(val left: Expression, val op: String, val right: Expression) : Expression() {
    override fun toSql() = "${left.toSql()} $op ${right.toSql()}"
}

data class And(val left: Expression, val right: Expression) : Expression() {
    override fun toSql() = "${left.toSql()} AND ${right.toSql()}"
}

data class Or(val left: Expression, val right: Expression) : Expression() {
    override fun toSql() = "${left.toSql()} OR ${right.toSql()}"
}

data class Not(val expr: Expression) : Expression() {
    override fun toSql() = "NOT ${expr.toSql()}"
}

data class Exists(val subquery: SelectQuery) : Expression() {
    override fun toSql() = "EXISTS(${subquery.toSql()})"
}

data class Between(val expr: Expression, val low: Expression, val high: Expression) : Expression() {
    override fun toSql() = "${expr.toSql()} BETWEEN ${low.toSql()} AND ${high.toSql()}"
}

data class Like(val expr: Expression, val pattern: Expression) : Expression() {
    override fun toSql() = "${expr.toSql()} LIKE ${pattern.toSql()}"
}

data class InList(val expr: Expression, val items: List<Expression>) : Expression() {
    override fun toSql() = "${expr.toSql()} IN (${items.joinToString(",") { it.toSql() }})"
}

data class InSubquery(val expr: Expression, val subquery: SelectQuery) : Expression() {
    override fun toSql() = "${expr.toSql()} IN (${subquery.toSql()})"
}

data class IsNull(val expr: Expression, val negated: Boolean) : Expression() {
    override fun toSql() = "${expr.toSql()} IS${if (negated) " NOT" else ""} NULL"
}