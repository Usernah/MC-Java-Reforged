package net.jr.client.meta;

import net.jr.api.client.meta.Meta;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MetaParser {
    private MetaParser() {}

    public static Meta parse(Reader reader) throws IOException {
        StringBuilder source = new StringBuilder();
        reader.transferTo(new java.io.Writer() {
            @Override public void write(char[] chars, int offset, int length) { source.append(chars, offset, length); }
            @Override public void flush() {}
            @Override public void close() {}
        });
        Parser parser = new Parser(source.toString());
        Map<String, Object> root = parser.object(false);
        parser.skip();
        if (!parser.end()) throw parser.error("Unexpected content");
        Object configurations = root.remove("configuration");
        Map<String, Map<String, Object>> configs = new LinkedHashMap<>();
        if (configurations instanceof Map<?, ?> raw) {
            raw.forEach((key, value) -> {
                if (key instanceof String id && value instanceof Map<?, ?> values) {
                    Map<String, Object> config = new LinkedHashMap<>();
                    values.forEach((k, v) -> { if (k instanceof String text) config.put(text, v); });
                    configs.put(id, config);
                }
            });
        } else if (configurations != null) {
            throw new IOException("configuration must be an object");
        }
        return new Meta(root, configs);
    }

    private static final class Parser {
        final String source;
        int position;
        Parser(String source) { this.source = source; }
        boolean end() { return position >= source.length(); }
        IOException error(String message) { return new IOException(message + " at index " + position); }
        void skip() {
            while (!end()) {
                if (Character.isWhitespace(source.charAt(position))) { position++; continue; }
                if (source.charAt(position) == '#' || source.startsWith("//", position)) {
                    while (!end() && source.charAt(position) != '\n') position++;
                    continue;
                }
                break;
            }
        }
        boolean peek(char value) { return !end() && source.charAt(position) == value; }
        void expect(char value) throws IOException {
            skip();
            if (!peek(value)) throw error("Expected '" + value + "'");
            position++;
        }
        Map<String, Object> object(boolean braced) throws IOException {
            if (braced) expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skip();
            while (!end() && !(braced && peek('}'))) {
                String key = string(true);
                expect(':');
                result.put(key, value());
                skip();
                if (peek(',')) position++;
                else if (!(braced && peek('}')) && !end()) throw error("Expected comma");
                skip();
            }
            if (braced) expect('}');
            return result;
        }
        Object value() throws IOException {
            skip();
            if (peek('{')) return object(true);
            if (peek('[')) return list();
            if (peek('"') || peek('\'')) return string(false);
            String token = string(false);
            if ("true".equalsIgnoreCase(token) || "false".equalsIgnoreCase(token)) return Boolean.parseBoolean(token);
            try { return Integer.parseInt(token); } catch (NumberFormatException ignored) {}
            try { return Double.parseDouble(token); } catch (NumberFormatException ignored) {}
            return token;
        }
        List<Object> list() throws IOException {
            expect('[');
            List<Object> values = new ArrayList<>();
            skip();
            while (!peek(']')) {
                values.add(value());
                skip();
                if (peek(',')) position++;
                else if (!peek(']')) throw error("Expected comma");
                skip();
            }
            expect(']');
            return values;
        }
        String string(boolean key) throws IOException {
            skip();
            if (peek('"') || peek('\'')) {
                char quote = source.charAt(position++);
                StringBuilder result = new StringBuilder();
                while (!end() && !peek(quote)) {
                    char next = source.charAt(position++);
                    result.append(next == '\\' && !end() ? source.charAt(position++) : next);
                }
                expect(quote);
                return result.toString();
            }
            int start = position;
            while (!end()) {
                char next = source.charAt(position);
                if (Character.isWhitespace(next) || (key ? next == ':' : ",}]".indexOf(next) >= 0)) break;
                position++;
            }
            if (start == position) throw error(key ? "Expected key" : "Expected value");
            return source.substring(start, position).trim();
        }
    }
}
