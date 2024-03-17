package com.github.elias_ka.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;
    private final Environment closure;
    private final boolean isInitializer;

    public LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
        this.declaration = declaration;
        this.closure = closure;
        this.isInitializer = isInitializer;
    }

    LoxFunction bind(LoxInstance instance) {
        final Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(declaration, environment, isInitializer);
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        final Environment environment = new Environment(closure);

        declaration.params.forEach(param -> {
            final int paramIndex = declaration.params.indexOf(param);
            environment.define(param.lexeme(), arguments.get(paramIndex));
        });

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return ret) {
            if (isInitializer) {
                return closure.getAt(0, "this");
            }
            return ret.getValue();
        }

        return null;
    }

    @Override
    public String toString() {
        return "<fn %s>".formatted(declaration.name.lexeme());
    }
}
