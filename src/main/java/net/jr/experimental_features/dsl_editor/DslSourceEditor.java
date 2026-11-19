package net.jr.experimental_features.dsl_editor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Writes only the literal tokens tracked by the compiler; never inserts properties into a block. */
final class DslSourceEditor {
    private static final Pattern NUMBER = Pattern.compile(
        "[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?"
    );
    private static final Pattern BARE = Pattern.compile("[A-Za-z][A-Za-z0-9_-]*");
    private static final Pattern COLOR = Pattern.compile("(?:#[A-Fa-f0-9]{3,8}|0[xX][A-Fa-f0-9]{6,8})");

    private DslSourceEditor() {
    }

    static String replaceValues(String source, List<ValueEdit> edits) {
        if (edits.isEmpty()) {
            return source;
        }
        List<Replacement> replacements = new ArrayList<>(edits.size());
        Set<Integer> usedOffsets = new HashSet<>();
        for (ValueEdit edit : edits) {
            int start = offset(source, edit.line(), edit.column());
            if (start < 0 || start >= source.length() || !usedOffsets.add(start)) {
                throw new IllegalArgumentException(
                    "Posición de argumento inválida o compartida: " + edit.line() + ":" + edit.column()
                );
            }
            Matcher matcher = NUMBER.matcher(source);
            matcher.region(start, source.length());
            if (!matcher.lookingAt()) {
                throw new IllegalArgumentException(
                    "La propiedad no apunta a un número literal: " + edit.line() + ":" + edit.column()
                );
            }
            int end = matcher.end();
            if (end < source.length() && !isBoundary(source.charAt(end))) {
                throw new IllegalArgumentException(
                    "El argumento no es un número aislado: " + edit.line() + ":" + edit.column()
                );
            }
            replacements.add(new Replacement(start, end, edit.value()));
        }
        replacements.sort((a, b) -> Integer.compare(b.start(), a.start()));
        String result = source;
        for (Replacement replacement : replacements) {
            result = result.substring(0, replacement.start())
                + replacement.value() + result.substring(replacement.end());
        }
        return result;
    }

    /** Edits an existing scalar literal only. Never inserts DSL properties or changes a blueprint definition. */
    static String replaceScalars(String source, List<ValueEdit> edits) {
        if (edits.isEmpty()) return source;
        List<Replacement> replacements = new ArrayList<>();
        Set<Integer> usedOffsets = new HashSet<>();
        for (ValueEdit edit : edits) {
            int start = offset(source, edit.line(), edit.column());
            if (start < 0 || start >= source.length() || !usedOffsets.add(start)) {
                throw new IllegalArgumentException("Ubicación duplicada o inválida de propiedad");
            }
            int end;
            String value;
            char first = source.charAt(start);
            if (first == '"') {
                end = start + 1;
                boolean escaped = false;
                while (end < source.length()) {
                    char c = source.charAt(end++);
                    if (!escaped && c == '"') break;
                    if (!escaped && c == '\\') escaped = true;
                    else escaped = false;
                }
                if (end > source.length() || source.charAt(end - 1) != '"') {
                    throw new IllegalArgumentException("Texto original sin cierre");
                }
                value = "\"" + edit.value().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
            } else if (source.startsWith("true", start) || source.startsWith("false", start)) {
                end = start + (source.startsWith("true", start) ? 4 : 5);
                if (!edit.value().equals("true") && !edit.value().equals("false"))
                    throw new IllegalArgumentException("Se esperaba true o false");
                value = edit.value();
            } else if (source.charAt(start) == '#' || source.startsWith("0x", start)
                    || source.startsWith("0X", start)) {
                Matcher matcher = COLOR.matcher(source);
                matcher.region(start, source.length());
                if (!matcher.lookingAt() || !COLOR.matcher(edit.value()).matches())
                    throw new IllegalArgumentException("Se esperaba un color hexadecimal válido");
                end = matcher.end();
                value = edit.value();
            } else if (Character.isLetter(source.charAt(start))) {
                Matcher matcher = BARE.matcher(source);
                matcher.region(start, source.length());
                if (!matcher.lookingAt() || !BARE.matcher(edit.value()).matches())
                    throw new IllegalArgumentException("Se esperaba un identificador literal válido");
                end = matcher.end();
                value = edit.value();
            } else {
                Matcher matcher = NUMBER.matcher(source);
                matcher.region(start, source.length());
                if (!matcher.lookingAt())
                    throw new IllegalArgumentException("Solo se pueden editar literales, no expresiones");
                end = matcher.end();
                try {
                    double number = Double.parseDouble(edit.value());
                    if (!Double.isFinite(number)) throw new NumberFormatException();
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("Se esperaba un número finito");
                }
                value = edit.value();
            }
            if (end < source.length() && !isBoundary(source.charAt(end)))
                throw new IllegalArgumentException("Propiedad no aislada; se conservó la expresión original");
            replacements.add(new Replacement(start, end, value));
        }
        replacements.sort((a, b) -> Integer.compare(b.start(), a.start()));
        String result = source;
        for (Replacement replacement : replacements) {
            result = result.substring(0, replacement.start()) + replacement.value()
                + result.substring(replacement.end());
        }
        return result;
    }

    /** Reads a literal argument at a tracked source location, without editing it. */
    static Float numericAt(String source, int line, int column) {
        int start = offset(source, line, column);
        if (start < 0 || start >= source.length()) return null;
        Matcher matcher = NUMBER.matcher(source);
        matcher.region(start, source.length());
        if (!matcher.lookingAt()) return null;
        if (matcher.end() < source.length() && !isBoundary(source.charAt(matcher.end()))) return null;
        try { return Float.parseFloat(matcher.group()); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static boolean isBoundary(char value) {
        return Character.isWhitespace(value) || value == ',' || value == ')'
            || value == ']' || value == '}' || value == ';';
    }

    private static int offset(String source, int line, int column) {
        if (line < 1 || column < 1) {
            return -1;
        }
        int currentLine = 1;
        int index = 0;
        while (currentLine < line && index < source.length()) {
            if (source.charAt(index++) == '\n') {
                currentLine++;
            }
        }
        if (currentLine != line) {
            return -1;
        }
        int start = index + column - 1;
        if (start >= source.length()) {
            return -1;
        }
        for (int i = index; i < start; i++) {
            if (source.charAt(i) == '\n' || source.charAt(i) == '\r') {
                return -1;
            }
        }
        return start;
    }

    static String conditionNear(String source, int nodeLine) {
        String[] lines = source.split("\\R", -1);
        int start = Math.max(0, Math.min(lines.length - 1, nodeLine - 1));
        for (int index = start; index >= Math.max(0, start - 40); index--) {
            String trimmed = lines[index].trim();
            if (trimmed.startsWith("if (") && trimmed.endsWith("{")) {
                return trimmed.substring(0, trimmed.length() - 1).trim();
            }
        }
        return "";
    }

    static String number(float value) {
        int rounded = Math.round(value);
        return Math.abs(value - rounded) < 0.0001F ? Integer.toString(rounded) : Float.toString(value);
    }

    record ValueEdit(int line, int column, String value) {
    }

    private record Replacement(int start, int end, String value) {
    }
}
