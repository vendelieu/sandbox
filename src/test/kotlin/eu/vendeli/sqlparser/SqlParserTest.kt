package eu.vendeli.sqlparser

import eu.vendeli.sqlparser.core.SqlLexer
import eu.vendeli.sqlparser.types.*
import eu.vendeli.sqlparser.utils.toSelectQuery
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeTypeOf
import org.intellij.lang.annotations.Language

class SqlParserTest : AnnotationSpec() {
    @Test
    fun `lexer should tokenize identifiers, numbers, strings, and symbols correctly`() {
        val tokens = SqlLexer(@Language("SQL") "SELECT a1, 'foo', 42 FROM tbl;").tokenize()
        tokens.map { it.type } shouldBe listOf(
            TokenType.SELECT, TokenType.IDENTIFIER, TokenType.COMMA,
            TokenType.STRING, TokenType.COMMA, TokenType.NUMBER,
            TokenType.FROM, TokenType.IDENTIFIER, TokenType.SEMICOLON,
            TokenType.EOF
        )
    }

    @Test
    fun `simple select star from table`() {
        val ast = @Language("SQL") "SELECT * FROM book".toSelectQuery()
        ast.select.size shouldBe 1
        (ast.select.first().expr as Literal).value shouldBe "*"
        (ast.from.first() as TableName).name shouldBe "book"
    }

    @Test
    fun `implicit join with multiple tables`() {
        val ast = @Language("SQL") "SELECT * FROM A, B, C".toSelectQuery()
        ast.from.map { (it as TableName).name } shouldBe listOf("A", "B", "C")
        ast.joins shouldBe emptyList()
    }

    @Test
    fun `explicit joins parse correctly`() {
        val sql = @Language("SQL") """
            SELECT u.id, o.id 
            FROM users u
            LEFT JOIN orders o ON u.id = o.user_id
            INNER JOIN payments p ON p.order_id = o.id
            """.trimIndent()
        val ast = sql.toSelectQuery()
        ast.joins.map { it.type } shouldBe listOf(JoinType.LEFT, JoinType.INNER)
        ast.joins[0].table.asTableName().alias shouldBe "o"
        (ast.joins[1].on as BinaryOp).op shouldBe "="
    }

    @Test
    fun `nested subqueries supported to depth`() {
        val sql = @Language("SQL") "SELECT * FROM (SELECT x FROM (SELECT y FROM Z) t2) t1"
        val ast = sql.toSelectQuery()
        val sub1 = (ast.from.first() as SubqueryTable).query
        val sub2 = ((sub1.from.first() as SubqueryTable).query.from.first() as TableName).name
        sub2 shouldBe "Z"
    }

    @Test
    fun `parses having clause correctly`() {
        val sql = """
        SELECT department, COUNT(*) AS cnt
        FROM employees
        GROUP BY department
        HAVING COUNT(*) > 5
    """.trimIndent()

        val ast = sql.toSelectQuery()
        // Ensure `having` is populated and is a BinaryOp
        ast.having.shouldNotBeNull().shouldBeTypeOf<BinaryOp>()
        (ast.having as BinaryOp).op shouldBe ">"  // > 5

        // Round-trip
        ast.toSql().shouldContain("HAVING COUNT(*) > 5")
    }

    @Test
    fun `group by, having, order by, limit and offset round trip`() {
        val sql = @Language("SQL") """
            SELECT name, COUNT(*) AS cnt
            FROM author
            GROUP BY name
            HAVING COUNT(*) > 1
            ORDER BY cnt DESC
            LIMIT 5 OFFSET 10
        """.trimIndent()
        val ast = sql.toSelectQuery()
        ast.groupBy.map { (it as Column).name } shouldBe listOf("name")
        ast.where shouldBe null
        // Assuming parser supports HAVING by storing as 'where' under a flag
        ast.orderBy.first().order shouldBe Order.DESC
        ast.limit shouldBe 5
        ast.offset shouldBe 10

        // Round-trip SQL
        val regenerated = ast.toSql()
        regenerated.shouldStartWith("SELECT name, COUNT(*) AS cnt FROM author")
    }

    @Test
    fun `invalid SQL throws UnexpectedTokenException`() {
        shouldThrow<UnexpectedTokenException> {
            @Language("SQL") "SELECT FROM WHERE".toSelectQuery()
        }.message shouldStartWith "Unexpected"
    }

    @Test
    fun `parameterized tests for various simple selects`() {
        @Language("SQL") listOf(
            "SELECT a FROM t",
            "SELECT a AS x FROM tbl",
            "SELECT f(a, b) FROM funs"
        ).forEach {
            shouldNotThrowAny { it.toSelectQuery() }
        }
    }

    @Test
    fun `deeply nested whitespace and comments`() {
        val sql = @Language("SQL") """
            SELECT  /* comment */ *
            FROM
              A   -- inline comment
            INNER
            JOIN B 
            ON A.id=B.id
        """.trimIndent()
        // Lexing should ignore comments and parse fine
        val ast = sql.toSelectQuery()
        (ast.from.first() as TableName).name shouldBe "A"
        ast.joins.size shouldBe 1
    }

    // --
    @Test
    fun `qualified wildcard table dot star`() {
        val ast = "SELECT t.* FROM tbl t".toSelectQuery()
        (ast.select[0].expr as Wildcard).toSql() shouldBe "t.*"
    }

    @Test
    fun `cross join and natural join and full outer`() {
        listOf(
            "SELECT * FROM A CROSS JOIN B",
            "SELECT * FROM A NATURAL JOIN B",
            "SELECT * FROM A FULL OUTER JOIN B ON A.id=B.a_id"
        ).forEach { sql ->
            sql.toSelectQuery()  // should not throw
        }
    }

    @Test
    fun `unary NOT operator is parsed correctly`() {
        val sql = "SELECT * FROM t WHERE NOT (a = 1)"
        val ast = sql.toSelectQuery()

        ast.where.shouldBeTypeOf<Not>()
        val inner = ast.where.expr
        inner.shouldBeTypeOf<BinaryOp>()
        inner.op shouldBe "="
    }


    @Test
    fun `BETWEEN and LIKE predicates parse as expected`() {
        val sql = "SELECT * FROM sales WHERE amount BETWEEN 10 AND 20 AND name LIKE 'A%'"
        val ast = sql.toSelectQuery()

        ast.where.shouldBeInstanceOf<And>()
        val and = ast.where

        and.left.shouldBeInstanceOf<Between>()
        and.left.low shouldBe Literal("10")
        and.left.high shouldBe Literal("20")

        and.right.shouldBeTypeOf<Like>()
        val like = and.right.shouldBeTypeOf<Like>()
        like.pattern.shouldBeTypeOf<Literal>().value shouldBe "A%"
    }


    @Test
    fun `IN-list and subquery in WHERE clause`() {
        val sql = "SELECT * FROM users WHERE id IN (1,2,3) AND dept IN (SELECT id FROM dept)"
        val ast = sql.toSelectQuery()

        ast.where.shouldBeInstanceOf<And>()
        val and = ast.where

        and.left.shouldBeInstanceOf<InList>()
        val inList = and.left
        inList.items.size shouldBe 3

        and.right.shouldBeInstanceOf<InSubquery>()
        val inSub = and.right
        inSub.subquery.shouldBeTypeOf<SelectQuery>()
    }


    @Test
    fun `IS NULL and IS NOT NULL predicates`() {
        val sql = "SELECT * FROM t WHERE col IS NULL OR col IS NOT NULL"
        val ast = sql.toSelectQuery()

        ast.where.shouldBeInstanceOf<Or>()
        val or = ast.where

        or.left.shouldBeInstanceOf<IsNull>()
        or.left.negated shouldBe false

        or.right.shouldBeInstanceOf<IsNull>()
        or.right.negated shouldBe true
    }


    @Test
    fun `EXISTS subquery in WHERE clause is parsed`() {
        val sql = "SELECT * FROM t WHERE EXISTS (SELECT 1 FROM u WHERE u.id = t.u_id)"
        val ast = sql.toSelectQuery()

        ast.where.shouldBeInstanceOf<Exists>()
        val exists = ast.where
        exists.subquery.shouldBeTypeOf<SelectQuery>()
    }


    @Test
    fun `only first SQL statement is parsed in multi-statement input`() {
        val sql = "SELECT 1; SELECT 2;"
        val ast = sql.toSelectQuery()

        (ast.select[0].expr as Literal).value shouldBe "1"
    }


    @Test
    fun `schema and database qualified columns parse correctly`() {
        val sql = "SELECT db1.schem1.tbl1.col FROM db1.schem1.tbl1"
        val ast = sql.toSelectQuery()

        val col = ast.select[0].expr as Column
        col.toSql() shouldBe "db1.schem1.tbl1.col"
        col.name shouldBe "col"
    }

}
