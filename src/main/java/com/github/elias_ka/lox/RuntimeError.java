package com.github.elias_ka.lox;

public class RuntimeError extends RuntimeException {
    private final transient Token token;

    public RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }

    public Token getToken() {
        return token;
    }
}
