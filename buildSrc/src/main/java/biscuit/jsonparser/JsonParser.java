/* ======================================
 * File: JsonParser.java
 * Date: 2026-10-10
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package biscuit.jsonparser;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.Character;
import java.lang.IllegalArgumentException;
import java.nio.file.Files;
import java.nio.file.Path;


public class JsonParser {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        String string = Files.readString(Path.of("/home/zubo/projs/biscuit/.biscuit/version_manifest_v2.json"));
        Map<String, Object> root = (Map<String, Object>) new Parser(new Lexer(string).scan()).toJson();
        List<Object> versions = (List<Object>) root.get("versions");
        Map<String, Object> first = (Map<String, Object>) versions.get(0);
        System.out.println((String) first.get("id"));
    }
}

class Parser {
    List<Token> tokens;
    int pos;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    public Object toJson() {
        Object result = value();
        if (tokens.get(pos).type() != TokenType.EOF) {
            throw new IllegalArgumentException("Next token should be EOF");
        }
        return result;
    }

    private Object value() {
        Token word = tokens.get(pos);
        if (word.type() == TokenType.LEFT_BRACE) {
            return object();
        }

        if (word.type() == TokenType.LEFT_BRACKET) {
            return array();
        }

        switch (word.type()) {
            case STRING, NUMBER, TRUE, FALSE, NULL: {
                pos++;
                return word.value();
            }
            default:
            throw new IllegalArgumentException("Unexpected token");
        }
    }

    private List<Object> array() {
        List<Object> array = new ArrayList<>();

        Token word = tokens.get(pos);
        if (word.type() != TokenType.LEFT_BRACKET) { // should never happen
            throw new IllegalArgumentException("Array should start with left bracket");
        }

        pos++;
        word = tokens.get(pos);
        if (word.type() == TokenType.RIGHT_BRACKET) {
            pos++;
            return array;
        }
        array.add(value());

        word = tokens.get(pos);
        while (word.type() != TokenType.RIGHT_BRACKET) {
            if (word.type() != TokenType.COMMA) {
                throw new IllegalArgumentException("Expected comma after a value");
            }

            pos++;
            array.add(value());
            word = tokens.get(pos);
        }

        pos++;
        return array;
    }

    private Map<String, Object> object() {
        Map<String, Object> object = new HashMap<>();

        Token word = tokens.get(pos);
        if (word.type() != TokenType.LEFT_BRACE) {
            throw new IllegalArgumentException("Object should start with left brace");
        }

        pos++;
        word = tokens.get(pos);
        if (word.type() == TokenType.RIGHT_BRACE) {
            pos++;
            return object;
        }

        Member member = member();
        object.put(member.key(), member.value());

        word = tokens.get(pos);
        while (word.type() != TokenType.RIGHT_BRACE) {
            if (word.type() != TokenType.COMMA) {
                throw new IllegalArgumentException("Expected comma after an object member");
            }

            pos++;
            member = member();
            if (object.containsKey(member.key())) {
                throw new IllegalArgumentException("Object contains two members with the same key " + member.key());
            }
            object.put(member.key(), member.value());
            word = tokens.get(pos);
        }

        pos++;
        return object;
    }

    private Member member() {
        Token key = tokens.get(pos);
        if (key.type() != TokenType.STRING) {
            throw new IllegalArgumentException("Expected String, found " + key.type());
        }

        pos++;
        Token colon = tokens.get(pos);
        if (colon.type() != TokenType.COLON) {
            throw new IllegalArgumentException("Expected colon, found " + colon.type());
        }

        pos++;
        return new Member((String) key.value(), value());
    }

    record Member(String key, Object value) {}
}

class Lexer {
    private String source;
    private int index;

    public Lexer(String source) {
        this.source = source;
        this.index = 0;
    }

    public List<Token> scan() {
        List<Token> tokens = new ArrayList<>();

        while (index < source.length()) {
            char c = source.charAt(index);

            switch (c) {
                case '{' -> tokens.add(new Token(TokenType.LEFT_BRACE, null));
                case '}' -> tokens.add(new Token(TokenType.RIGHT_BRACE, null));
                case '[' -> tokens.add(new Token(TokenType.LEFT_BRACKET, null));
                case ']' -> tokens.add(new Token(TokenType.RIGHT_BRACKET, null));
                case ':' -> tokens.add(new Token(TokenType.COLON, null));
                case ',' -> tokens.add(new Token(TokenType.COMMA, null));
                case ' ', '\n', '\r', '\t' -> {}
                case '"' -> tokens.add(scanString());
                case 't' -> tokens.add(scanKeyword("true", TokenType.TRUE, Boolean.TRUE));
                case 'f' -> tokens.add(scanKeyword("false", TokenType.FALSE, Boolean.FALSE));
                case 'n' -> tokens.add(scanKeyword("null", TokenType.NULL, null));
                default -> {
                    if (c == '-' || Character.isDigit(c)) tokens.add(scanNumber());
                    else throw new IllegalArgumentException("Unknown operator '" + c + "'");
                }
            }
            index++;
        }
        tokens.add(new Token(TokenType.EOF, null));
        return tokens;
    }

    private Token scanNumber() {
        int start = index;
        int n = source.length();
        if (source.charAt(index) == '-') index++;

        if (index >= n || source.charAt(index) < '0' || source.charAt(index) > '9') {
            throw new IllegalArgumentException("Digit expected at index " + index);
        }
        if (source.charAt(index) == '0') {
            index++;
        } else {
            while (index < n && source.charAt(index) >= '0' && source.charAt(index) <= '9') index++;
        }

        if (index < n && source.charAt(index) == '.') {
            index++;
            if (index >= n || source.charAt(index) < '0' || source.charAt(index) > '9') {
                throw new IllegalArgumentException("Digit expected after '.' at index " + index);
            }
            while (index < n && source.charAt(index) >= '0' && source.charAt(index) <= '9') index++;
        }

        if (index < n && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
            index++;
            if (index < n && (source.charAt(index) == '+' || source.charAt(index) == '-')) index++;
            if (index >= n || source.charAt(index) < '0' || source.charAt(index) > '9') {
                throw new IllegalArgumentException("Digit expected in exponent at index " + index);
            }
            while (index < n && source.charAt(index) >= '0' && source.charAt(index) <= '9') index++;
        }

        index--;
        return new Token(TokenType.NUMBER, Double.parseDouble(source.substring(start, index + 1)));
    }

    private Token scanKeyword(String substring, TokenType type, Object value) {
        if (!source.startsWith(substring, index)) {
            throw new IllegalArgumentException("Bad word at index " + index);
        }

        index += substring.length() - 1;
        return new Token(type, value);
    }

    private Token scanString() {
        int start = index++;
        while (index < source.length()) {
            char ch = source.charAt(index);
            if (ch == '"') break;
            if (ch == '\\') {
                throw new IllegalArgumentException(
                    "Escapes not supported yet, at index " + index);
            }
            if (ch < 0x20) {
                throw new IllegalArgumentException(
                    "Control character (code " + (int) ch + ") in string at index " + index);
            }
            index++;
        }
        if (index >= source.length()) {
            throw new IllegalArgumentException(
                "String starting at index " + (start) + " was never closed");
        }
        return new Token(TokenType.STRING, source.substring(start + 1, index));
    }
}

enum TokenType {
    LEFT_BRACE,
    LEFT_BRACKET,
    STRING,
    NUMBER,
    TRUE,
    FALSE,
    NULL,
    RIGHT_BRACE,
    RIGHT_BRACKET,
    COLON,
    COMMA,
    EOF
}

record Token(TokenType type, Object value) {}
