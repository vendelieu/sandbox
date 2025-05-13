package eu.vendeli.sqlparser.core

import eu.vendeli.sqlparser.types.Token
import eu.vendeli.sqlparser.types.TokenType

class SqlLexer(private val input: String) {
    private var pos = 0
    private val len = input.length

    private fun peek(): Char? = if (pos < len) input[pos] else null
    private fun next(): Char? = peek()?.also { pos++ }

    private fun skipWhitespace() {
        while (peek()?.isWhitespace() == true) pos++
    }

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        skipWhitespace()
        while (true) {
            val c = peek() ?: break
            if (c.isWhitespace()) {
                pos++
                continue
            }

            if (c == '-' && input.getOrNull(pos + 1) == '-') {
                pos += 2
                while (peek() != null && peek() != '\n') pos++
                continue
            }

            // Skip block comment (/* ... */)
            if (c == '/' && input.getOrNull(pos + 1) == '*') {
                pos += 2
                while (!(peek() == '*' && input.getOrNull(pos + 1) == '/')) {
                    if (peek() == null) throw RuntimeException("Unclosed block comment")
                    pos++
                }
                pos += 2 // Skip closing */
                continue
            }

            when {
                c == '*' -> { tokens += Token(TokenType.STAR, "*"); pos++ }
                c == ',' -> { tokens += Token(TokenType.COMMA, ","); pos++ }
                c == '.' -> { tokens += Token(TokenType.DOT, "."); pos++ }
                c == '(' -> { tokens += Token(TokenType.LPAREN, "("); pos++ }
                c == ')' -> { tokens += Token(TokenType.RPAREN, ")"); pos++ }
                c == ';' -> { tokens += Token(TokenType.SEMICOLON, ";"); pos++ }
                c == '=' -> { tokens += Token(TokenType.EQ, "="); pos++ }
                c == '!' && input.getOrNull(pos+1) == '=' -> {
                    tokens += Token(TokenType.NEQ, "!="); pos += 2
                }
                c == '<' -> {
                    if (input.getOrNull(pos+1) == '=') {
                        tokens += Token(TokenType.LTE, "<="); pos += 2
                    } else {
                        tokens += Token(TokenType.LT, "<"); pos++
                    }
                }
                c == '>' -> {
                    if (input.getOrNull(pos+1) == '=') {
                        tokens += Token(TokenType.GTE, ">="); pos += 2
                    } else {
                        tokens += Token(TokenType.GT, ">"); pos++
                    }
                }
                c == '\'' -> {
                    pos++
                    val sb = StringBuilder()
                    while (peek() != null && peek() != '\'') {
                        sb.append(next())
                    }
                    if (peek() == '\'') pos++ else throw RuntimeException("Unterminated string")
                    tokens += Token(TokenType.STRING, sb.toString())
                }
                c.isDigit() -> {
                    val sb = StringBuilder()
                    while (peek()?.isDigit() == true) sb.append(next())
                    tokens += Token(TokenType.NUMBER, sb.toString())
                }
                c.isLetter() || c == '_' -> {
                    val sb = StringBuilder()
                    while (peek()?.let { it.isLetterOrDigit() || it == '_' } == true) {
                        sb.append(next())
                    }
                    val text = sb.toString()
                    val type = when (text.uppercase()) {
                        "SELECT" -> TokenType.SELECT
                        "FROM"   -> TokenType.FROM
                        "WHERE"  -> TokenType.WHERE
                        "GROUP"  -> TokenType.GROUP
                        "BY"     -> TokenType.BY
                        "ORDER"  -> TokenType.ORDER
                        "ASC"    -> TokenType.ASC
                        "DESC"   -> TokenType.DESC
                        "LIMIT"  -> TokenType.LIMIT
                        "OFFSET" -> TokenType.OFFSET
                        "INNER"  -> TokenType.INNER
                        "LEFT"   -> TokenType.LEFT
                        "RIGHT"  -> TokenType.RIGHT
                        "FULL"   -> TokenType.FULL
                        "CROSS"   -> TokenType.CROSS
                        "NATURAL" -> TokenType.NATURAL
                        "OUTER"   -> TokenType.OUTER
                        "JOIN"   -> TokenType.JOIN
                        "ON"     -> TokenType.ON
                        "AS"     -> TokenType.AS
                        "HAVING" -> TokenType.HAVING
                        "AND"    -> TokenType.AND
                        "NOT"    -> TokenType.NOT
                        "IS"     -> TokenType.IS
                        "NULL"   -> TokenType.NULL
                        "IN"      -> TokenType.IN
                        "BETWEEN" -> TokenType.BETWEEN
                        "LIKE"    -> TokenType.LIKE
                        "EXISTS"  -> TokenType.EXISTS
                        "OR"     -> TokenType.OR
                        else     -> TokenType.IDENTIFIER
                    }
                    tokens += Token(type, text)
                }
                else -> throw RuntimeException("Unexpected character '$c' at position $pos")
            }
            skipWhitespace()
        }
        tokens += Token(TokenType.EOF, "")
        return tokens
    }
}