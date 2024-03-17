package com.github.elias_ka.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private final Environment globals = new Environment();
    private final Map<Expr, Integer> locals = new HashMap<>();
    private Environment environment = globals;

    public Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<native fn>";
            }
        });
    }

    public void interpret(List<Stmt> statements) {
        try {
            for (final Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        final Object left = evaluate(expr.left);
        final Object right = evaluate(expr.right);

        switch (expr.operator.type()) {
            case TokenType.GREATER -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            }
            case TokenType.GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            }
            case TokenType.LESS -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            }
            case TokenType.LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            }
            case TokenType.BANG_EQUAL -> {
                return !isEqual(left, right);
            }
            case TokenType.EQUAL_EQUAL -> {
                return isEqual(left, right);
            }
            case TokenType.MINUS -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            }
            case TokenType.PLUS -> {
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String l && right instanceof String r) {
                    return l + r;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            }
            case TokenType.SLASH -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left / (double) right;
            }
            case TokenType.STAR -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            }
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        final Object callee = evaluate(expr.callee);
        final List<Object> arguments = expr.arguments.stream().map(this::evaluate).toList();

        if (!(callee instanceof LoxCallable function)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        if (arguments.size() != function.arity()) {
            final String plural = function.arity() == 1 ? "" : "s";
            final String msg = "Expected %d argument%s but got %d.".formatted(function.arity(), plural, arguments.size());
            throw new RuntimeError(expr.paren, msg);
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        if (evaluate(expr.object) instanceof LoxInstance instance) {
            return instance.get(expr.name);
        }
        throw new RuntimeError(expr.name, "Only instances have properties.");
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        final Object left = evaluate(expr.left);

        if (expr.operator.type() == TokenType.OR) {
            if (isTruthy(left))
                return left;
        } else {
            if (!isTruthy(left))
                return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        final Object object = evaluate(expr.object);

        if (object instanceof LoxInstance instance) {
            final Object value = evaluate(expr.value);
            instance.set(expr.name, value);
            return value;
        }

        throw new RuntimeError(expr.name, "Only instances have fields.");
    }

    @Override
    public Object visitSuperExpr(Expr.Super expr) {
        final int distance = locals.get(expr);
        final LoxClass superclass = (LoxClass) environment.getAt(distance, "super");
        final LoxInstance object = (LoxInstance) environment.getAt(distance - 1, "this");
        final LoxFunction method = superclass.findMethod(expr.method.lexeme());

        if (method == null) {
            throw new RuntimeError(expr.method, "Undefined property '%s'.".formatted(expr.method.lexeme()));
        }

        return method.bind(object);
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        final Object right = evaluate(expr.right);

        return switch (expr.operator.type()) {
            case TokenType.BANG -> !isTruthy(right);
            case TokenType.MINUS -> {
                checkNumberOperands(expr.operator, right);
                yield -(double) right;
            }
            // Unreachable.
            default -> null;
        };

    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    private Object lookUpVariable(Token name, Expr expr) {
        final Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme());
        }
        return globals.get(name);
    }

    private void checkNumberOperands(Token operator, Object... operands) {
        for (final Object operand : operands) {
            if (operand instanceof Double)
                continue;

            final String msg = operands.length == 1
                    ? "Operand must be a number."
                    : "Operands must be numbers.";
            throw new RuntimeError(operator, msg);
        }

    }

    private boolean isTruthy(Object object) {
        if (object == null)
            return false;
        if (object instanceof Boolean)
            return (boolean) object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null)
            return true;
        if (a == null)
            return false;

        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null)
            return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        final Environment previous = this.environment;

        try {
            this.environment = environment;
            for (final Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        Object superclass = null;
        if (stmt.superclass != null) {
            superclass = evaluate(stmt.superclass);
            if (!(superclass instanceof LoxClass)) {
                throw new RuntimeError(
                        stmt.superclass.name,
                        "Superclass '%s' must be a class.".formatted(stmt.superclass.name.lexeme())
                );
            }
        }

        environment.define(stmt.name.lexeme(), null);

        if (stmt.superclass != null) {
            environment = new Environment(environment);
            environment.define("super", superclass);
        }

        final Map<String, LoxFunction> methods = new HashMap<>();
        for (final Stmt.Function method : stmt.methods) {
            final boolean isInitializer = method.name.lexeme().equals("init");
            final LoxFunction function = new LoxFunction(method, environment, isInitializer);
            methods.put(method.name.lexeme(), function);
        }

        final LoxClass klass = new LoxClass(stmt.name.lexeme(), (LoxClass) superclass, methods);

        if (superclass != null) {
            environment = environment.getEnclosing();
        }

        environment.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        environment.define(stmt.name.lexeme(), new LoxFunction(stmt, environment, false));
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        final Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        final Object value = (stmt.value != null) ? evaluate(stmt.value) : null;
        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        final Object value = (stmt.initializer != null) ? evaluate(stmt.initializer) : null;
        environment.define(stmt.name.lexeme(), value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        final Object value = evaluate(expr.value);
        final Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }
        return value;
    }
}
