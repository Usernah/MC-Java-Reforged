package net.jr.client.ui.dsl;

import net.jr.api.client.resource.Asset;

import java.util.ArrayList;
import java.util.List;

final class UiLexer {
    enum Type {
        AT,
        AT_AT,
        AMPERSAND,
        DATA_START,
        IDENTIFIER,
        STRING,
        NUMBER,
        COLOR,
        LEFT_BRACE,
        RIGHT_BRACE,
        LEFT_BRACKET,
        RIGHT_BRACKET,
        LEFT_PAREN,
        RIGHT_PAREN,
        COLON,
        EQUALS,
        EQUAL_EQUAL,
        NOT_EQUAL,
        LESS,
        LESS_OR_EQUAL,
        GREATER,
        GREATER_OR_EQUAL,
        PLUS,
        MINUS,
        STAR,
        SLASH,
        PERCENT,
        COMMA,
        DOT,
        EOF
    }

    record Token(Type type, String text, int line, int column) {
    }

    private final Asset sourceAsset;
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int index;
    private int line = 1;
    private int column = 1;

    UiLexer(Asset sourceAsset, String source) {
        this.sourceAsset = sourceAsset;
        this.source = source;
    }

    List<Token> lex() throws UiParseException {
        while (!this.end()) {
            this.skipWhitespaceAndComments();
            if (this.end()) {
                break;
            }

            int tokenLine = this.line;
            int tokenColumn = this.column;
            char current = this.peek();

            if (current == '@') {
                this.advance();
                if (this.match('@')) {
                    this.add(Type.AT_AT, "@@", tokenLine, tokenColumn);
                } else {
                    this.add(Type.AT, "@", tokenLine, tokenColumn);
                }
                continue;
            }
            if (current == '$' && this.peekNext() == '{') {
                this.advance();
                this.advance();
                this.add(Type.DATA_START, "${", tokenLine, tokenColumn);
                continue;
            }
            if (current == '&') {
                this.advance();
                this.add(Type.AMPERSAND, "&", tokenLine, tokenColumn);
                continue;
            }
            if (current == '"' || current == '\'') {
                this.lexString(current, tokenLine, tokenColumn);
                continue;
            }
            if (current == '#' && this.isColorStart()) {
                this.lexColor(tokenLine, tokenColumn);
                continue;
            }
            if (Character.isDigit(current)) {
                this.lexNumber(tokenLine, tokenColumn);
                continue;
            }
            if (isIdentifierStart(current)) {
                this.lexIdentifier(tokenLine, tokenColumn);
                continue;
            }

            if (current == '=' || current == '!' || current == '<' || current == '>') {
                this.advance();
                boolean followedByEquals = this.match('=');
                Type type = switch (current) {
                    case '=' -> followedByEquals ? Type.EQUAL_EQUAL : Type.EQUALS;
                    case '!' -> {
                        if (!followedByEquals) {
                            throw this.error(tokenLine, tokenColumn, "Expected '=' after '!'");
                        }
                        yield Type.NOT_EQUAL;
                    }
                    case '<' -> followedByEquals ? Type.LESS_OR_EQUAL : Type.LESS;
                    case '>' -> followedByEquals ? Type.GREATER_OR_EQUAL : Type.GREATER;
                    default -> throw new IllegalStateException();
                };
                this.add(type, followedByEquals ? current + "=" : Character.toString(current), tokenLine, tokenColumn);
                continue;
            }

            this.advance();
            Type type = switch (current) {
                case '{' -> Type.LEFT_BRACE;
                case '}' -> Type.RIGHT_BRACE;
                case '[' -> Type.LEFT_BRACKET;
                case ']' -> Type.RIGHT_BRACKET;
                case '(' -> Type.LEFT_PAREN;
                case ')' -> Type.RIGHT_PAREN;
                case ':' -> Type.COLON;
                case '+' -> Type.PLUS;
                case '-' -> Type.MINUS;
                case '*' -> Type.STAR;
                case '/' -> Type.SLASH;
                case '%' -> Type.PERCENT;
                case ',' -> Type.COMMA;
                case '.' -> Type.DOT;
                default -> throw this.error(tokenLine, tokenColumn, "Unexpected character '" + current + "'");
            };
            this.add(type, Character.toString(current), tokenLine, tokenColumn);
        }

        this.tokens.add(new Token(Type.EOF, "", this.line, this.column));
        return List.copyOf(this.tokens);
    }

    private void skipWhitespaceAndComments() {
        boolean repeat;
        do {
            repeat = false;
            while (!this.end() && Character.isWhitespace(this.peek())) {
                this.advance();
            }
            if (!this.end() && this.peek() == '/' && this.peekNext() == '/') {
                this.skipLine();
                repeat = true;
            } else if (!this.end() && this.peek() == '#' && !this.isColorStart()) {
                this.skipLine();
                repeat = true;
            }
        } while (repeat);
    }

    private void skipLine() {
        while (!this.end() && this.peek() != '\n') {
            this.advance();
        }
    }

    private void lexString(char quote, int tokenLine, int tokenColumn) throws UiParseException {
        this.advance();
        StringBuilder value = new StringBuilder();
        while (!this.end() && this.peek() != quote) {
            char current = this.advance();
            if (current == '\\') {
                if (this.end()) {
                    throw this.error(tokenLine, tokenColumn, "Unterminated string");
                }
                char escaped = this.advance();
                value.append(switch (escaped) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case '\\' -> '\\';
                    case '\'' -> '\'';
                    case '"' -> '"';
                    default -> escaped;
                });
            } else {
                value.append(current);
            }
        }
        if (this.end()) {
            throw this.error(tokenLine, tokenColumn, "Unterminated string");
        }
        this.advance();
        this.add(Type.STRING, value.toString(), tokenLine, tokenColumn);
    }

    private void lexColor(int tokenLine, int tokenColumn) {
        int start = this.index;
        this.advance();
        while (!this.end() && isHexDigit(this.peek())) {
            this.advance();
        }
        this.add(Type.COLOR, this.source.substring(start, this.index), tokenLine, tokenColumn);
    }

    private void lexNumber(int tokenLine, int tokenColumn) {
        int start = this.index;
        while (!this.end() && Character.isDigit(this.peek())) {
            this.advance();
        }
        if (!this.end() && this.peek() == '.' && Character.isDigit(this.peekNext())) {
            this.advance();
            while (!this.end() && Character.isDigit(this.peek())) {
                this.advance();
            }
        }
        this.add(Type.NUMBER, this.source.substring(start, this.index), tokenLine, tokenColumn);
    }

    private void lexIdentifier(int tokenLine, int tokenColumn) {
        int start = this.index;
        this.advance();
        while (!this.end() && isIdentifierPart(this.peek())) {
            this.advance();
        }
        this.add(Type.IDENTIFIER, this.source.substring(start, this.index), tokenLine, tokenColumn);
    }

    private boolean isColorStart() {
        if (this.peek() != '#') {
            return false;
        }
        int cursor = this.index + 1;
        while (cursor < this.source.length() && isHexDigit(this.source.charAt(cursor))) {
            cursor++;
        }
        int length = cursor - this.index - 1;
        if (length != 3 && length != 4 && length != 6 && length != 8) {
            return false;
        }
        return cursor >= this.source.length() || isValueBoundary(this.source.charAt(cursor));
    }

    private char advance() {
        char current = this.source.charAt(this.index++);
        if (current == '\n') {
            this.line++;
            this.column = 1;
        } else {
            this.column++;
        }
        return current;
    }

    private boolean match(char expected) {
        if (this.end() || this.peek() != expected) {
            return false;
        }
        this.advance();
        return true;
    }

    private char peek() {
        return this.source.charAt(this.index);
    }

    private char peekNext() {
        return this.index + 1 < this.source.length() ? this.source.charAt(this.index + 1) : '\0';
    }

    private boolean end() {
        return this.index >= this.source.length();
    }

    private void add(Type type, String text, int tokenLine, int tokenColumn) {
        this.tokens.add(new Token(type, text, tokenLine, tokenColumn));
    }

    private UiParseException error(int errorLine, int errorColumn, String message) {
        return new UiParseException(this.sourceAsset, errorLine, errorColumn, message);
    }

    private static boolean isIdentifierStart(char value) {
        return Character.isLetter(value) || value == '_';
    }

    private static boolean isIdentifierPart(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '-';
    }

    private static boolean isHexDigit(char value) {
        return Character.digit(value, 16) >= 0;
    }

    private static boolean isValueBoundary(char value) {
        return Character.isWhitespace(value) || value == ',' || value == '}' || value == ']';
    }
}
