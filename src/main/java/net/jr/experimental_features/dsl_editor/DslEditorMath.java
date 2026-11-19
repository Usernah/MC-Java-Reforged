package net.jr.experimental_features.dsl_editor;

/** Small arithmetic parser for numeric inspector fields. */
final class DslEditorMath {
    private final String input;
    private int cursor;

    private DslEditorMath(String input) { this.input = input; }

    static String resolve(String input) {
        if (input == null || !input.matches("[0-9eE+\\-*/().\\s]+")) return input;
        DslEditorMath parser = new DslEditorMath(input);
        double value = parser.expression();
        parser.spaces();
        if (parser.cursor != input.length() || !Double.isFinite(value)) {
            throw new IllegalArgumentException("Cuenta numérica inválida");
        }
        long integer = Math.round(value);
        return Math.abs(value - integer) < 0.0000001D
            ? Long.toString(integer)
            : DslSourceEditor.number((float)value);
    }

    private double expression() {
        double value = term();
        while (true) {
            spaces();
            if (take('+')) value += term();
            else if (take('-')) value -= term();
            else return value;
        }
    }

    private double term() {
        double value = unary();
        while (true) {
            spaces();
            if (take('*')) value *= unary();
            else if (take('/')) {
                double divisor = unary();
                if (divisor == 0.0D) throw new IllegalArgumentException("División por cero");
                value /= divisor;
            } else return value;
        }
    }

    private double unary() {
        spaces();
        if (take('+')) return unary();
        if (take('-')) return -unary();
        if (take('(')) {
            double value = expression();
            spaces();
            if (!take(')')) throw new IllegalArgumentException("Falta ')' en la cuenta");
            return value;
        }
        return number();
    }

    private double number() {
        spaces();
        int start = cursor;
        while (cursor < input.length()) {
            char ch = input.charAt(cursor);
            if (Character.isDigit(ch) || ch == '.' || ch == 'e' || ch == 'E'
                    || ((ch == '+' || ch == '-') && cursor > start
                        && (input.charAt(cursor - 1) == 'e' || input.charAt(cursor - 1) == 'E'))) cursor++;
            else break;
        }
        if (start == cursor) throw new IllegalArgumentException("Se esperaba un número");
        try { return Double.parseDouble(input.substring(start, cursor)); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Número inválido"); }
    }

    private boolean take(char expected) {
        if (cursor < input.length() && input.charAt(cursor) == expected) { cursor++; return true; }
        return false;
    }

    private void spaces() { while (cursor < input.length() && Character.isWhitespace(input.charAt(cursor))) cursor++; }
}
