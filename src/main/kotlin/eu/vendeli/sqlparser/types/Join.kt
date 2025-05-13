package eu.vendeli.sqlparser.types

data class Join(val type: JoinType, val table: TableReference, val on: Expression?) {
    fun toSql(): String {
        val base = "${type.name} JOIN ${table.toSql()}"
        return if (on != null) {
            "$base ON ${on.toSql()}"
        } else {
            base
        }
    }
}