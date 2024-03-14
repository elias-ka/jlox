package com.github.elias_ka.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

public class GenerateAst {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];
        try {
            defineAst(outputDir, "Expr", List.of(
                    "Assign   : Token name, Expr value",
                    "Binary   : Expr left, Token operator, Expr right",
                    "Call     : Expr callee, Token paren, List<Expr> arguments",
                    "Grouping : Expr expression",
                    "Literal  : Object value",
                    "Logical  : Expr left, Token operator, Expr right",
                    "Unary    : Token operator, Expr right",
                    "Variable : Token name"
            ));

            defineAst(outputDir, "Stmt", List.of(
                    "Block      : List<Stmt> statements",
                    "Expression : Expr expression",
                    "Function   : Token name, List<Token> params, List<Stmt> body",
                    "If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
                    "Print      : Expr expression",
                    "Return     : Token keyword, Expr value",
                    "Var        : Token name, Expr initializer",
                    "While      : Expr condition, Stmt body"
            ));
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
        final Path path = Paths.get(outputDir, baseName + ".java");

        try (PrintWriter writer = new PrintWriter(path.toFile(), StandardCharsets.UTF_8)) {
            writer.println("package com.github.elias_ka.lox;");
            writer.println();
            writer.println("import java.util.List;");
            writer.println();
            writer.println("public abstract class " + baseName + " {");

            defineVisitor(writer, baseName, types);

            // The AST classes.
            for (final String type : types) {
                final String className = type.split(":")[0].trim();
                final String fields = type.split(":")[1].trim();
                defineType(writer, baseName, className, fields);
            }

            // The base accept() method.
            writer.println("    abstract <R> R accept(Visitor<R> visitor);");

            writer.println("}");
        }
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("    interface Visitor<R> {");

        for (final String type : types) {
            final String typeName = type.split(":")[0].trim();
            writer.println("        R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase(Locale.US) + ");");
            writer.println();
        }

        writer.println("    }");
        writer.println();
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
        final String[] fields = fieldList.split(", ");

        writer.println("    public static class " + className + " extends " + baseName + " {");

        // Fields.
        for (String field : fields) {
            writer.println("        public final " + field + ";");
        }

        // Constructor.
        writer.println();
        writer.println("        " + className + "(" + fieldList + ") {");

        // Store parameters in fields.
        for (final String field : fields) {
            String name = field.split(" ")[1];
            writer.println("            this." + name + " = " + name + ";");
        }

        writer.println("        }");

        // Visitor pattern.
        writer.println();
        writer.println("        @Override");
        writer.println("        <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit" + className + baseName + "(this);");
        writer.println("        }");
        writer.println("    }");
        writer.println();
    }
}
