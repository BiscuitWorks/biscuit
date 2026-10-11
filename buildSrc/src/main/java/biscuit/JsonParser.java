/* ======================================
 * File: JsonParser.java
 * Date: 2026-10-11
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package biscuit;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.Math;
import java.lang.StringBuilder;

public class JsonParser {
    protected String src;
    protected int pos;

    public JsonParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    public static Json parse(String src) {
        JsonParser p = new JsonParser(src);
        Json v = p.parseValue();
        p.skipWhitespace();
        if (p.pos != src.length()) throw new RuntimeException("Unexpected trailing data");
        return v;
    }

    private Json parseValue() {
        skipWhitespace();
        char ch = peek();
        return switch (ch) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> parseString();
            case 't', 'f', 'n' -> parseLiteral();
            case '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> parseNumber();
            case '\0' -> throw error("Unexpected end of input");
            default -> throw error("Unexpected character '" + ch + "'");
        };
    }

    // "{" ( member ("," member)* )? "}"
    private JObject parseObject() {
        JObject object = new JObject(new HashMap<>());
        expect("{");
        skipWhitespace();
        if (match('}')) return object;
        do {
            skipWhitespace();
            JString key = parseString();
            skipWhitespace();
            expect(":");
            Json value = parseValue();
            object.add(key.value(), value);
            skipWhitespace();
        } while (match(','));
        expect("}");
        return object;
    }

    // "[" ( value ("," value)* )? "]"
    private JArray parseArray() {
        JArray array = new JArray(new ArrayList<>());
        expect("[");
        skipWhitespace();
        if (match(']')) return array;
        do {
            skipWhitespace();
            Json value = parseValue();
            array.add(value);
        } while (match(','));
        skipWhitespace();
        expect("]");
        return array;
    }

    private JString parseString() {
        expect("\"");
        int start = pos;
        char ch = next();
        while (ch != '"') {
            if (ch == '\n') throw error("WRITE A GOOD ERROR MESSAGE HERE PLS");
            if (ch == '\\') throw error("Escaping inside strings not supported yet.");
            if (ch == '\0') throw error("Unterminated String");
            ch = next();
        }
        return new JString(src.substring(start, pos - 1));
    }

    private Json parseLiteral() {
        switch(peek()) {
            case 't' -> { expect("true"); return new JBool(true); }
            case 'f' -> { expect("false"); return new JBool(false); }
            case 'n' -> { expect("null"); return JNull.INSTANCE; }
            default -> throw error("Unsupported literal");
        }
    }

    private JNumber parseNumber() {
        int start = pos;
        match('-');
        digits();
        if (match('.')) digits();
        if (match('e') || match('E')) {
            if (!match('+')) match('-');
            digits();
        }

        return new JNumber(Double.parseDouble(src.substring(start, pos)));
    }

    private void digits() {
        if (!isDigit(peek())) throw error("Digit expected");
        while (isDigit(peek())) pos++;
    }

    private boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    // this can be written in a better way for sure. how
    private void skipWhitespace() {
        char ch = peek();
        while (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t') {
            pos++;
            ch = peek();
        }
    }

    private boolean match(char ch) {
        if (peek() == ch) { pos++; return true; }
        return false;
    }

    // this is like an assert
    private void expect(String str) {
        if (!src.startsWith(str, pos)) {
            throw error("Expected " + str + ", found " + peek());
        }
        pos += str.length();
    }

    private char peek() {
        return pos < src.length() ? src.charAt(pos) : '\0';
    }

    private char next() {
        return pos < src.length() ? src.charAt(pos++) : '\0';
    }

    private RuntimeException error(String msg) {
        int from = Math.max(0, pos - 5), to = Math.min(src.length(), pos + 5);
        return new IllegalArgumentException(msg + " at position " + pos + ", near \"" + src.substring(from, to) + "\"");
    }
}

sealed interface Json permits JObject, JArray, JString, JNumber, JBool, JNull {

    default String getString(String path) {
        Json v = get(path);
        if (v instanceof JString s) return s.value();
        throw error(path + ": expected type string, received " + v.getClass().getSimpleName());
    }

    default double getDouble(String path) {
        Json v = get(path);
        if (v instanceof JNumber d) return d.value();
        throw error(path + ": expected type double, received " + v.getClass().getSimpleName());
    }

    default boolean getBool(String path) {
        Json v = get(path);
        if (v instanceof JBool b) return b.value();
        throw error(path + ": expected type boolean, received " + v.getClass().getSimpleName());
    }

    default JArray getArray(String path) {
        Json v = get(path);
        if (v instanceof JArray a) return a;
        throw error(path + ": expected array, got " + v.getClass().getSimpleName());
    }

    default JObject getObject(String path) {
        Json v = get(path);
        if (v instanceof JObject o) return o;
        throw error(path + ": expected object, got " + v.getClass().getSimpleName());
    }

    default Json get(String path) {
        if (path.isEmpty()) throw error("Path is empty.");
        List<Step> steps = new ArrayList<>();

        for (String seg: path.split("\\.", -1)) {
            if (seg.isEmpty()) throw error("Empty segment detected.");

            int pos = seg.indexOf('[');
            if (pos == -1) pos = seg.length();
            if (pos != 0) steps.add(new Key(seg.substring(0, pos)));

            while (pos < seg.length()) {
                if (seg.charAt(pos) != '[') throw error("Expected [.");
                int end = seg.indexOf(']', pos);
                if (end == -1) throw error("No closing bracket found for segment " + seg);
                try {
                    steps.add(new Index(Integer.parseInt(seg.substring(pos + 1, end))));
                } catch (Exception e) {
                    throw new RuntimeException("Could not parse " + seg.substring(pos + 1, end) + " into an integer.");
                }
                pos = end + 1;
            }
        }

        Json curr = this;
        StringBuilder coveredPath = new StringBuilder();

        for (Step step : steps) {
            switch (step) {
                case Key k -> {
                    if (!(curr instanceof JObject o))
                        throw error(coveredPath + ": expected object, found " + curr.getClass().getSimpleName());
                    if (coveredPath.length() > 0) coveredPath.append('.');
                    coveredPath.append(k.name());
                    curr = o.field(k.name());
                    if (curr == null) throw error(coveredPath + ": key not found");
                }
                case Index i -> {
                    if (!(curr instanceof JArray a))
                        throw error(coveredPath + ": expected array, found " + curr.getClass().getSimpleName());
                    coveredPath.append('[').append(i.index()).append(']');
                    if (i.index() < 0 || i.index() >= a.items().size())
                        throw error(coveredPath + ": index out of range for length " + a.items().size());
                    curr = a.at(i.index());
                }
            }
        }
        return curr;
    }

    private static RuntimeException error(String msg) {
        return new RuntimeException(msg);
    }
}

record JObject(Map<String, Json> fields) implements Json {
    public void add(String key, Json value) {
        if (fields.containsKey(key)) {
            throw new RuntimeException("key " + key + " already exists!");
        }

        fields.put(key, value);
    }

    public Json field(String key) {
        return fields.get(key);
    }
}
record JArray(List<Json> items) implements Json {
    public void add(Json item) {
        items.add(item);
    }

    public Json at(int i) {
        return items.get(i);
    }
}
record JString(String value) implements Json {}
record JNumber(double value) implements Json {}
record JBool(boolean value) implements Json {}
enum JNull implements Json { INSTANCE }

sealed interface Step permits Key, Index {}
record Key(String name) implements Step {}
record Index(int index) implements Step {}
