package com.github.elias_ka.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    private static final Interpreter interpreter = new Interpreter();
    private static boolean hadError = false;
    private static boolean hadRuntimeError = false;

    public static void main(String[] args) {
        try {
            if (args.length > 1) {
                System.out.println("Usage: jlox [script]");
                System.exit(64);
            } else if (args.length == 1) {
                runFile(args[0]);
            } else {
                runPrompt();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(74);
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (; ; ) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null)
                break;

            run(line);
            hadError = false;
        }
    }

    private static void run(String source) {
        final Scanner scanner = new Scanner(source);
        final List<Token> tokens = scanner.scanTokens();
        final Parser parser = new Parser(tokens);
        final List<Stmt> statements = parser.parse();

        // Stop if there was a syntax error.
        if (hadError) return;

        final Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        // Stop if there was a resolution error.
        if (hadError) return;

        interpreter.interpret(statements);
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.printf("[line %d] Error %s: %s%n", line, where, message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.type() == TokenType.EOF) {
            report(token.line(), "at end", message);
        } else {
            report(token.line(), "at '" + token.lexeme() + "'", message);
        }
    }

    static void runtimeError(RuntimeError error) {
        System.err.printf("[line %d] Error: %s %n", error.getToken().line(), error.getMessage());
        hadRuntimeError = true;
    }
}
