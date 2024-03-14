package com.github.elias_ka.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;

    public LoxFunction(Stmt.Function declaration) {
        this.declaration = declaration;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        final Environment environment = new Environment(interpreter.globals);

        declaration.params.forEach(param -> {
            final int paramIndex = declaration.params.indexOf(param);
            environment.define(param.lexeme(), arguments.get(paramIndex));
        });

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }
        return null;
    }

    @Override
    public String toString() {
        return "<fn %s>".formatted(declaration.name.lexeme());
    }
}
