package eu.vendeli.sqlparser.types

enum class TokenType {
    // Keywords
    SELECT, FROM, WHERE, GROUP, BY, ORDER, ASC, DESC, LIMIT, OFFSET,
    INNER, LEFT, RIGHT, FULL, CROSS, NATURAL, OUTER, JOIN, ON, AS,
    AND, OR, NOT, EXISTS, HAVING,

    // Symbols
    STAR, COMMA, DOT, LPAREN, RPAREN, EQ, NEQ, LT, LTE, GT, GTE,
    SEMICOLON, BETWEEN, LIKE, IN, IS, NULL,

    // Literals & identifiers
    IDENTIFIER, NUMBER, STRING,

    // End of input
    EOF
}