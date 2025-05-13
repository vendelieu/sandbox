package eu.vendeli.sqlparser.core

import eu.vendeli.sqlparser.types.*

class SqlParser(private val tokens: List<Token>) {
    private var pos = 0
    private fun peek() = tokens[pos]
    private fun next() = tokens[pos++]
    private fun expect(type: TokenType): Token {
        val t = peek()
        if (t.type != type) throw ParseException(t, type)
        return next()
    }

    private fun match(vararg types: TokenType): Boolean {
        if (peek().type in types) {
            next()
            return true
        }
        return false
    }

    fun parseQuery(): SelectQuery {
        expect(TokenType.SELECT)
        val select = parseSelectList()
        val from = if (match(TokenType.FROM)) {
            parseFromList()
        } else {
            emptyList()
        }
        val joins = parseJoins()
        val where = if (match(TokenType.WHERE)) parseExpression() else null
        val groupBy = if (match(TokenType.GROUP)) {
            expect(TokenType.BY); parseExpressionList()
        } else emptyList()
        val having = if (match(TokenType.HAVING)) {
            parseExpression()
        } else null
        val orderBy = if (match(TokenType.ORDER)) {
            expect(TokenType.BY); parseOrderingList()
        } else emptyList()
        val limit = if (match(TokenType.LIMIT)) next().takeIf { it.type == TokenType.NUMBER }?.text?.toInt() else null
        val offset = if (match(TokenType.OFFSET)) next().takeIf { it.type == TokenType.NUMBER }?.text?.toInt() else null
        return SelectQuery(select, from, joins, where, groupBy, having, orderBy, limit, offset)
    }

    private fun parseSelectList(): List<SelectItem> {
        if (match(TokenType.STAR)) return listOf(SelectItem(Literal("*"), null))
        return parseSelectItem().let { first ->
            mutableListOf<SelectItem>().apply {
                add(first)
                while (match(TokenType.COMMA)) add(parseSelectItem())
            }
        }
    }

    private fun parseSelectItem(): SelectItem {
        val expr = parseExpression()
        val alias = if (match(TokenType.AS) || peek().type == TokenType.IDENTIFIER) {
            next().text
        } else null
        return SelectItem(expr, alias)
    }

    private fun parseFromList(): List<TableReference> {
        val first = parseTableReference()
        return mutableListOf<TableReference>().apply {
            add(first)
            while (match(TokenType.COMMA)) add(parseTableReference())
        }
    }

    private fun parseTableReference(): TableReference {
        return if (match(TokenType.LPAREN)) {
            val sub = parseQuery()
            expect(TokenType.RPAREN)
            val alias = next().takeIf { it.type == TokenType.IDENTIFIER }?.text
                ?: throw WrongTableReferenceException("Subquery requires alias")
            SubqueryTable(sub, alias)
        } else {
            val name = expect(TokenType.IDENTIFIER).text
            val alias = if (peek().type == TokenType.IDENTIFIER) next().text else null
            TableName(name, alias)
        }
    }

    private fun parseJoins(): List<Join> {
        val joins = mutableListOf<Join>()
        while (true) {
            val type = when {
                match(TokenType.CROSS) -> {
                    expect(TokenType.JOIN); JoinType.CROSS
                }

                match(TokenType.NATURAL) -> {
                    expect(TokenType.JOIN); JoinType.NATURAL
                }

                match(TokenType.INNER) -> {
                    expect(TokenType.JOIN); JoinType.INNER
                }

                match(TokenType.LEFT) -> {
                    expect(TokenType.JOIN)
                    JoinType.LEFT
                }

                match(TokenType.RIGHT) -> {
                    expect(TokenType.JOIN); JoinType.RIGHT
                }

                match(TokenType.FULL) -> {
                    match(TokenType.OUTER)
                    expect(TokenType.JOIN)
                    JoinType.FULL
                }

                else -> break
            }
            val table = parseTableReference()
            val on = if (type in listOf(JoinType.INNER, JoinType.LEFT, JoinType.RIGHT, JoinType.FULL)) {
                expect(TokenType.ON); parseExpression()
            } else null
            joins += Join(type, table, on)
        }
        return joins
    }

    private fun parseExpressionList(): List<Expression> =
        mutableListOf<Expression>().apply {
            add(parseExpression())
            while (match(TokenType.COMMA)) add(parseExpression())
        }

    private fun parseOrderingList(): List<Ordering> =
        mutableListOf<Ordering>().apply {
            add(parseOrdering())
            while (match(TokenType.COMMA)) add(parseOrdering())
        }

    private fun parseOrdering(): Ordering {
        val expr = parseExpression()
        val ord = when {
            match(TokenType.ASC) -> Order.ASC
            match(TokenType.DESC) -> Order.DESC
            else -> Order.ASC
        }
        return Ordering(expr, ord)
    }

    private fun parseExpression(precedence: Int = 0): Expression {
        if (match(TokenType.NOT)) {
            return Not(parseExpression(precedence = /* higher than AND */ 6))
        }
        if (match(TokenType.EXISTS)) {
            expect(TokenType.LPAREN)
            val sq = parseQuery()
            expect(TokenType.RPAREN)
            return Exists(sq)
        }

        var left = when (peek().type) {
            TokenType.STAR -> {
                next()
                Wildcard(null)
            }

            TokenType.IDENTIFIER -> {
                val chain = parseIdentifierChain()

                when {
                    // function call
                    match(TokenType.LPAREN) -> {
                        val args = mutableListOf<Expression>()
                        if (!match(TokenType.RPAREN)) {
                            do {
                                args += parseExpression()
                            } while (match(TokenType.COMMA))
                            expect(TokenType.RPAREN)
                        }
                        FunctionCall(chain.joinToString("."), args)
                    }

                    // qualified wildcard
                    chain.last() == "*" -> {
                        val qual = chain.dropLast(1).joinToString(".")
                        Wildcard(qual.ifBlank { null })
                    }

                    // plain or qualified column
                    else -> {
                        val qual = chain.dropLast(1).takeIf { it.isNotEmpty() }?.joinToString(".")
                        Column(qual, chain.last())
                    }
                }
            }

            TokenType.NUMBER -> Literal(next().text)
            TokenType.STRING -> Literal(next().text)
            TokenType.LPAREN -> {
                next()
                // Could be grouped expr or subquery
                val expr = if (peek().type == TokenType.SELECT) {
                    SubqueryExpression(parseQuery())
                } else {
                    parseExpression()
                }
                expect(TokenType.RPAREN)
                expr
            }

            else -> throw UnexpectedTokenException("Unexpected token ${peek().type} in expression")
        }
        // binary / logical operators
        while (true) {
            val opInfo = when (peek().type) {
                TokenType.EQ -> "=" to 10
                TokenType.NEQ -> "!=" to 10
                TokenType.LT -> "<" to 10
                TokenType.LTE -> "<=" to 10
                TokenType.GT -> ">" to 10
                TokenType.GTE -> ">=" to 10
                TokenType.AND -> "AND" to 5
                TokenType.OR -> "OR" to 1
                TokenType.BETWEEN -> {
                    next()
                    val low = parseExpression(precedence = 11) // higher than AND
                    expect(TokenType.AND)
                    val high = parseExpression(precedence = 11)
                    left = Between(left, low, high)
                    continue
                }

                TokenType.LIKE -> {
                    next()
                    val pat = parseExpression()
                    left = Like(left, pat)
                    continue
                }

                TokenType.IN -> {
                    next()
                    if (peek().type == TokenType.LPAREN && lookaheadIsSelect()) {
                        expect(TokenType.LPAREN)
                        val sq = parseQuery()
                        expect(TokenType.RPAREN)
                        left = InSubquery(left, sq)
                    } else {
                        expect(TokenType.LPAREN)
                        val items = mutableListOf<Expression>().apply {
                            add(parseExpression())
                            while (match(TokenType.COMMA)) add(parseExpression())
                        }
                        expect(TokenType.RPAREN)
                        left = InList(left, items)
                    }
                    continue
                }

                TokenType.IS -> {
                    next()
                    val neg = match(TokenType.NOT)
                    expect(TokenType.NULL)
                    left = IsNull(left, neg)
                    continue
                }

                else -> null
            } ?: break

            val (op, prec) = opInfo
            if (prec < precedence) break
            next()
            val right = parseExpression(prec + 1)
            left = when (op.uppercase()) {
                "AND" -> And(left, right)
                "OR" -> Or(left, right)
                else -> BinaryOp(left, op, right)
            }
        }
        return left
    }

    private fun lookaheadIsSelect(): Boolean {
        if (pos + 1 >= tokens.size) return false
        // peek two ahead
        return tokens[pos + 1].type == TokenType.SELECT
    }

    private fun parseIdentifierChain(): List<String> {
        val parts = mutableListOf(expect(TokenType.IDENTIFIER).text)
        while (match(TokenType.DOT)) {
            when (peek().type) {
                TokenType.IDENTIFIER -> parts += next().text
                TokenType.STAR -> {
                    next(); parts += "*"
                }

                else -> throw UnexpectedTokenException(
                    "Expected IDENTIFIER or '*' after '.', found ${peek().type}"
                )
            }
        }
        return parts
    }

    companion object {
        fun parseSelect(sql: String): SelectQuery {
            val tokens = SqlLexer(sql).tokenize()
            return SqlParser(tokens).parseQuery()
        }
    }
}