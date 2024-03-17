package com.github.elias_ka.lox;

import java.util.List;

public interface LoxCallable {
    default int arity() {
        return 0;
    }

    Object call(Interpreter interpreter, List<Object> arguments);
}
