package com.github.elias_ka;

public class Interpreter implements Expr.Visitor<Object> {

    public void interpret(Expr expression) {
        try {
            final Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        final Object left = evaluate(expr.left);
        final Object right = evaluate(expr.right);

        switch (expr.operator.type()) {
            case TokenType.GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case TokenType.GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case TokenType.LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case TokenType.LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            case TokenType.BANG_EQUAL:
                return !isEqual(left, right);
            case TokenType.EQUAL_EQUAL:
                return isEqual(left, right);
            case TokenType.MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case TokenType.PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String l && right instanceof String r) {
                    return l + r;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case TokenType.SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double) left / (double) right;
            case TokenType.STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
        }

        // Unreachable.
        return null;
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
    public Object visitUnaryExpr(Expr.Unary expr) {
        final Object right = evaluate(expr.right);

        switch (expr.operator.type()) {
            case TokenType.BANG:
                return !isTruthy(right);
            case TokenType.MINUS:
                checkNumberOperands(expr.operator, right);
                return -(double) right;
        }

        // Unreachable.
        return null;
    }

    private void checkNumberOperands(Token operator, Object... operands) {
        for (final Object operand : operands) {
            if (operand instanceof Double) continue;

            final String msg = operands.length == 1
                    ? "Operand must be a number."
                    : "Operands must be numbers.";
            throw new RuntimeError(operator, msg);
        }

    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

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
}
