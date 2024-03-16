package com.github.elias_ka.lox;

import java.util.List;
import java.util.Map;

class LoxClass implements LoxCallable {
    final String name;
    private final Map<String, LoxFunction> methods;

    LoxClass(String name, Map<String, LoxFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    LoxFunction findMethod(String name) {
        return methods.get(name);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int arity() {
        final LoxFunction initializer = findMethod("init");
        if (initializer != null) {
            return initializer.arity();
        }

        return 0;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        final LoxInstance instance = new LoxInstance(this);
        final LoxFunction initializer = findMethod("init");

        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }
}
