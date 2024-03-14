package com.github.elias_ka.lox;

public class Return extends RuntimeException {
    final transient Object value;

    public Return(final Object value) {
        super(null, null, false, false);
        this.value = value;
    }
}
