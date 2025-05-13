package eu.vendeli.sqlparser

import eu.vendeli.sqlparser.core.SqlLexer
import eu.vendeli.sqlparser.core.SqlParser

fun main() {
    val examples = listOf(
        "SELECT * FROM book;",
        """
        SELECT author.name, COUNT(book.id) AS bc, SUM(book.cost)
          FROM author
          LEFT JOIN book ON author.id = book.author_id
         WHERE author.active = 1 AND book.cost > 10
         GROUP BY author.name
         HAVING COUNT(*) > 1
         ORDER BY SUM(book.cost) DESC
         LIMIT 10 OFFSET 5;
        """.trimIndent(),
        "SELECT (SELECT MAX(age) FROM people) AS maxAge;"
    )

    for (sql in examples) {
        println("⟶ SQL INPUT:\n$sql")
        val tokens = SqlLexer(sql).tokenize()
        val ast = SqlParser(tokens).parseQuery()
        println("⟶ AST OBJECT:\n$ast")
        println("⟶ REGENERATED SQL:\n${ast.toSql()}")
        println("------------------------------------------------------------\n")
    }
}