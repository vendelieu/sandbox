package eu.vendeli.sqlparser.types

class ParseException(token: Token, expected: TokenType)
    : RuntimeException("At token ${token.text}: expected $expected, found ${token.type}")
