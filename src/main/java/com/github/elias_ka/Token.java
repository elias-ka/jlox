package com.github.elias_ka;

public record Token(TokenType type, String lexeme, Object literal, int line) {

    @Override
    public String toString() {
        return "%s %s %s".formatted(type, lexeme, literal);
    }
}
