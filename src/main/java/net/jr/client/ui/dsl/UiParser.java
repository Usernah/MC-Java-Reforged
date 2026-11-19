package net.jr.client.ui.dsl;

import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.UiFileType;
import net.jr.client.ui.dsl.UiDocument.BooleanValue;
import net.jr.client.ui.dsl.UiDocument.ArithmeticNegationValue;
import net.jr.client.ui.dsl.UiDocument.ArithmeticOperator;
import net.jr.client.ui.dsl.UiDocument.ArithmeticValue;
import net.jr.client.ui.dsl.UiDocument.BlueprintDirective;
import net.jr.client.ui.dsl.UiDocument.BlueprintContent;
import net.jr.client.ui.dsl.UiDocument.BlueprintMemberUse;
import net.jr.client.ui.dsl.UiDocument.BlueprintParameterValue;
import net.jr.client.ui.dsl.UiDocument.BlueprintUse;
import net.jr.client.ui.dsl.UiDocument.CallValue;
import net.jr.client.ui.dsl.UiDocument.ConditionalValue;
import net.jr.client.ui.dsl.UiDocument.ConditionalStatement;
import net.jr.client.ui.dsl.UiDocument.ComparisonOperator;
import net.jr.client.ui.dsl.UiDocument.ComparisonValue;
import net.jr.client.ui.dsl.UiDocument.ColorValue;
import net.jr.client.ui.dsl.UiDocument.DefinitionDirective;
import net.jr.client.ui.dsl.UiDocument.AssignmentDirective;
import net.jr.client.ui.dsl.UiDocument.ExposedDataValue;
import net.jr.client.ui.dsl.UiDocument.GlyphValue;
import net.jr.client.ui.dsl.UiDocument.ElementValue;
import net.jr.client.ui.dsl.UiDocument.BareValue;
import net.jr.client.ui.dsl.UiDocument.ImportDirective;
import net.jr.client.ui.dsl.UiDocument.ImportTarget;
import net.jr.client.ui.dsl.UiDocument.ListValue;
import net.jr.client.ui.dsl.UiDocument.LogicalOperator;
import net.jr.client.ui.dsl.UiDocument.LogicalValue;
import net.jr.client.ui.dsl.UiDocument.NegatedValue;
import net.jr.client.ui.dsl.UiDocument.Node;
import net.jr.client.ui.dsl.UiDocument.NullValue;
import net.jr.client.ui.dsl.UiDocument.NumberValue;
import net.jr.client.ui.dsl.UiDocument.ObjectValue;
import net.jr.client.ui.dsl.UiDocument.Position;
import net.jr.client.ui.dsl.UiDocument.Property;
import net.jr.client.ui.dsl.UiDocument.ReferenceValue;
import net.jr.client.ui.dsl.UiDocument.RegisteredStyleValue;
import net.jr.client.ui.dsl.UiDocument.Statement;
import net.jr.client.ui.dsl.UiDocument.StringValue;
import net.jr.client.ui.dsl.UiDocument.TupleValue;
import net.jr.client.ui.dsl.UiDocument.Value;
import net.jr.client.ui.dsl.UiDocument.VariableDirective;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class UiParser {
    private static final int MAX_BLOCK_DEPTH = 128;
    private final Asset source;
    private final UiFileType type;
    private final List<UiLexer.Token> tokens;
    private final List<UiParseException> diagnostics;
    private int index;
    private int blockDepth;
    private int blueprintNesting;
    private final Set<Integer> blueprintBodyDepths = new HashSet<>();

    private UiParser(
        Asset source,
        UiFileType type,
        List<UiLexer.Token> tokens,
        List<UiParseException> diagnostics
    ) {
        this.source = source;
        this.type = type;
        this.tokens = tokens;
        this.diagnostics = new ArrayList<>(diagnostics);
    }

    public static Result parse(Reader reader, Asset source, UiFileType type)
        throws IOException {
        StringWriter writer = new StringWriter();
        reader.transferTo(writer);
        String sourceText = writer.toString();
        UiLexer.Result lexed = new UiLexer(source, sourceText).lex();
        return new UiParser(
            source, type, lexed.tokens(), lexed.diagnostics()
        ).document(sourceText);
    }

    private Result document(String sourceText) {
        List<Statement> statements = this.statementsUntil(UiLexer.Type.EOF);
        statements = this.sanitizeSymbols(statements, Set.of(), false);
        return new Result(
            new UiDocument(this.source, this.type, statements, sourceText),
            List.copyOf(this.diagnostics)
        );
    }

    private List<Statement> statementsUntil(UiLexer.Type terminator) {
        List<Statement> statements = new ArrayList<>();
        while (!this.check(terminator) && !this.check(UiLexer.Type.EOF)) {
            if (this.match(UiLexer.Type.COMMA)) {
                continue;
            }
            int startIndex = this.index;
            try {
                statements.add(this.statement());
                this.match(UiLexer.Type.COMMA);
            } catch (UiParseException exception) {
                this.diagnostics.add(exception);
                this.synchronizeStatement(terminator, startIndex);
            }
        }
        return statements;
    }

    private Statement statement() throws UiParseException {
        if (this.match(UiLexer.Type.AT_AT)) {
            return this.directive(this.previous());
        }
        if (this.match(UiLexer.Type.AT)) {
            UiLexer.Token marker = this.previous();
            if (this.check(UiLexer.Type.IDENTIFIER) && this.peek().text().equals("content")) {
                return this.blueprintContent(marker);
            }
            return this.blueprintUse(marker, false);
        }
        if (this.match(UiLexer.Type.DOUBLE_COLON)) {
            return this.blueprintUse(this.previous(), true);
        }
        if (this.type == UiFileType.DATA && this.check(UiLexer.Type.STRING)) {
            UiLexer.Token name = this.advance();
            this.expect(UiLexer.Type.COLON, "Expected ':' after data key");
            return new Property(name.text(), this.value(), this.position(name));
        }
        if (this.check(UiLexer.Type.IDENTIFIER)) {
            if (this.peek().text().equals("if")) {
                return this.conditionalStatement();
            }
            return this.namedStatement();
        }
        throw this.error(this.peek(), "Expected a directive, property or block");
    }

    private Statement directive(UiLexer.Token marker) throws UiParseException {
        UiLexer.Token name = this.expect(UiLexer.Type.IDENTIFIER, "Expected directive name after '@@'");
        return switch (name.text()) {
            case "import" -> this.importDirective(marker);
            case "def" -> this.definitionDirective(marker);
            case "blueprint" -> this.blueprintDirective(marker);
            case "var" -> this.variableDirective(marker);
            case "set" -> this.assignmentDirective(marker);
            default -> throw this.error(name, "Unknown directive '@@" + name.text() + "'");
        };
    }

    private ImportDirective importDirective(UiLexer.Token marker) throws UiParseException {
        UiLexer.Token asset = this.expect(UiLexer.Type.IDENTIFIER, "Expected 'asset' after '@@import'");
        if (!asset.text().equals("asset")) {
            throw this.error(asset, "Only asset imports are supported");
        }
        this.expect(UiLexer.Type.DOT, "Expected '.' after 'asset'");
        UiLexer.Token method = this.expect(UiLexer.Type.IDENTIFIER, "Expected asset import method");
        this.expect(UiLexer.Type.LEFT_PAREN, "Expected '(' after asset import method");

        boolean local;
        String namespace = null;
        String path;
        if (method.text().equals("local")) {
            local = true;
            path = this.expect(UiLexer.Type.STRING, "asset.local requires a path string").text();
        } else if (method.text().equals("from")) {
            local = false;
            namespace = this.expect(UiLexer.Type.STRING, "asset.from requires a namespace string").text();
            this.expect(UiLexer.Type.COMMA, "Expected ',' between namespace and path");
            path = this.expect(UiLexer.Type.STRING, "asset.from requires a path string").text();
        } else {
            throw this.error(method, "Unknown asset import method '" + method.text() + "'");
        }

        this.expect(UiLexer.Type.RIGHT_PAREN, "Expected ')' after asset import");
        UiLexer.Token as = this.expect(UiLexer.Type.IDENTIFIER, "Expected 'as' after asset import");
        if (!as.text().equals("as")) {
            throw this.error(as, "Expected 'as' after asset import");
        }
        String alias = this.expect(UiLexer.Type.IDENTIFIER, "Expected import alias").text();
        if (path.indexOf('\\') >= 0) {
            throw this.error(method, "Imported asset paths must use '/'");
        }
        return new ImportDirective(
            new ImportTarget(local, namespace, path),
            alias,
            this.position(marker)
        );
    }

    private DefinitionDirective definitionDirective(UiLexer.Token marker) throws UiParseException {
        String name = this.expect(UiLexer.Type.IDENTIFIER, "Expected definition name").text();
        this.expect(UiLexer.Type.EQUALS, "Expected '=' after definition name");
        return new DefinitionDirective(name, this.value(), this.position(marker));
    }

    private BlueprintDirective blueprintDirective(UiLexer.Token marker) throws UiParseException {
        if (this.blockDepth != 0 && !this.blueprintBodyDepths.contains(this.blockDepth)) {
            throw this.error(marker,
                "@@blueprint is only valid at document scope or directly inside another blueprint");
        }
        String name = this.expect(UiLexer.Type.IDENTIFIER, "Expected blueprint name").text();
        this.expect(UiLexer.Type.LEFT_PAREN, "Expected '(' after blueprint name");
        List<String> parameters = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        while (!this.check(UiLexer.Type.RIGHT_PAREN) && !this.check(UiLexer.Type.EOF)) {
            UiLexer.Token parameter = this.expect(UiLexer.Type.IDENTIFIER, "Expected blueprint parameter name");
            if (!unique.add(parameter.text())) {
                throw this.error(parameter, "Duplicate blueprint parameter '" + parameter.text() + "'");
            }
            parameters.add(parameter.text());
            if (!this.match(UiLexer.Type.COMMA) && !this.check(UiLexer.Type.RIGHT_PAREN)) {
                throw this.error(this.peek(), "Expected ',' between blueprint parameters");
            }
        }
        this.expect(UiLexer.Type.RIGHT_PAREN, "Expected ')' after blueprint parameters");
        this.expect(UiLexer.Type.LEFT_BRACE, "Expected '{' after blueprint declaration");
        int bodyDepth = this.blockDepth + 1;
        this.blueprintBodyDepths.add(bodyDepth);
        this.blueprintNesting++;
        List<Statement> body;
        try {
            body = this.blockStatements();
        } finally {
            this.blueprintNesting--;
            this.blueprintBodyDepths.remove(bodyDepth);
        }
        this.expect(UiLexer.Type.RIGHT_BRACE, "Expected '}' after blueprint declaration");
        return new BlueprintDirective(name, parameters, body, this.position(marker));
    }

    private VariableDirective variableDirective(UiLexer.Token marker) throws UiParseException {
        this.requireBlueprintBody(marker, "@@var");
        String name = this.expect(UiLexer.Type.IDENTIFIER, "Expected variable name").text();
        this.expect(UiLexer.Type.EQUALS, "Expected '=' after variable name");
        return new VariableDirective(name, this.value(), this.position(marker));
    }

    private AssignmentDirective assignmentDirective(UiLexer.Token marker) throws UiParseException {
        this.requireBlueprintBody(marker, "@@set");
        String name = this.expect(UiLexer.Type.IDENTIFIER, "Expected variable name").text();
        this.expect(UiLexer.Type.EQUALS, "Expected '=' after variable name");
        return new AssignmentDirective(name, this.value(), this.position(marker));
    }

    private void requireBlueprintBody(UiLexer.Token marker, String directive) throws UiParseException {
        if (!this.blueprintBodyDepths.contains(this.blockDepth)) {
            throw this.error(marker, directive + " is only valid directly inside a blueprint");
        }
    }

    private BlueprintUse blueprintUse(UiLexer.Token marker, boolean registeredStyle)
        throws UiParseException {
        List<String> path = this.symbolPath(registeredStyle
            ? "Expected registered style ID after '::'"
            : "Expected blueprint name after '@'");
        this.expect(UiLexer.Type.LEFT_PAREN, "Expected '(' after blueprint name");
        List<Value> arguments = this.arguments(UiLexer.Type.RIGHT_PAREN);
        this.expect(UiLexer.Type.RIGHT_PAREN, "Expected ')' after blueprint arguments");
        List<BlueprintMemberUse> members = new ArrayList<>();
        while (this.match(UiLexer.Type.DOT)) {
            UiLexer.Token member = this.expect(UiLexer.Type.IDENTIFIER,
                "Expected nested blueprint name after '.'");
            this.expect(UiLexer.Type.LEFT_PAREN,
                "Expected '(' after nested blueprint name");
            List<Value> memberArguments = this.arguments(UiLexer.Type.RIGHT_PAREN);
            this.expect(UiLexer.Type.RIGHT_PAREN,
                "Expected ')' after nested blueprint arguments");
            members.add(new BlueprintMemberUse(
                member.text(), memberArguments, this.position(member)
            ));
        }
        boolean contentProvided = this.match(UiLexer.Type.LEFT_BRACE);
        List<Statement> content = List.of();
        if (contentProvided) {
            content = this.blockStatements();
            this.expect(UiLexer.Type.RIGHT_BRACE, "Expected '}' after blueprint content");
        }
        return new BlueprintUse(
            path, arguments, members, content, contentProvided, registeredStyle,
            this.position(marker)
        );
    }

    private BlueprintContent blueprintContent(UiLexer.Token marker) throws UiParseException {
        UiLexer.Token name = this.advance();
        if (this.blueprintNesting == 0) {
            throw this.error(name, "@content is only valid inside a blueprint");
        }
        return new BlueprintContent(this.position(marker));
    }

    private Statement namedStatement() throws UiParseException {
        UiLexer.Token name = this.advance();
        if (this.match(UiLexer.Type.COLON)) {
            return new Property(name.text(), this.value(), this.position(name));
        }

        List<Value> arguments = List.of();
        if (this.match(UiLexer.Type.LEFT_PAREN)) {
            arguments = this.arguments(UiLexer.Type.RIGHT_PAREN);
            this.expect(UiLexer.Type.RIGHT_PAREN, "Expected ')' after block arguments");
        } else if (!this.check(UiLexer.Type.LEFT_BRACE)) {
            arguments = List.of(this.value());
        }

        this.expect(UiLexer.Type.LEFT_BRACE, "Expected ':' or '{' after '" + name.text() + "'");
        List<Statement> body = this.blockStatements();
        this.expect(UiLexer.Type.RIGHT_BRACE, "Expected '}' after block");
        return new Node(name.text(), arguments, body, this.position(name));
    }

    private ConditionalStatement conditionalStatement() throws UiParseException {
        UiLexer.Token start = this.advance();
        this.expect(UiLexer.Type.LEFT_PAREN, "Expected '(' after 'if'");
        Value condition = this.conditionExpression();
        this.expect(UiLexer.Type.RIGHT_PAREN, "Expected ')' after condition");
        this.expect(UiLexer.Type.LEFT_BRACE, "Expected '{' after condition");
        List<Statement> body = this.blockStatements();
        this.expect(UiLexer.Type.RIGHT_BRACE, "Expected '}' after conditional block");
        return new ConditionalStatement(condition, body, this.position(start));
    }

    private Value value() throws UiParseException {
        UiLexer.Token token = this.advance();
        return switch (token.type()) {
            case STRING -> new StringValue(token.text(), this.position(token));
            case NUMBER -> new NumberValue(token.text(), this.position(token));
            case MINUS -> this.negativeNumber(token);
            case COLOR -> new ColorValue(token.text(), this.position(token));
            case AT -> new ReferenceValue(this.symbolPath("Expected reference name after '@'"), this.position(token));
            case DOUBLE_COLON -> new RegisteredStyleValue(
                this.symbolPath("Expected registered style ID after '::'"),
                this.position(token)
            );
            case AMPERSAND -> new BlueprintParameterValue(
                this.symbolPath("Expected blueprint parameter name after '&'"),
                this.position(token)
            );
            case DATA_START -> this.exposedData(token);
            case LEFT_BRACKET -> this.list(token);
            case LEFT_PAREN -> this.tuple(token);
            case LEFT_BRACE -> this.object(token);
            case IDENTIFIER -> token.text().equals("if")
                ? this.conditional(token)
                : this.identifierOrCall(token);
            default -> throw this.error(token, "Expected value");
        };
    }

    private TupleValue tuple(UiLexer.Token start) throws UiParseException {
        List<Value> values = this.arguments(UiLexer.Type.RIGHT_PAREN);
        this.expect(UiLexer.Type.RIGHT_PAREN, "Expected ')' after tuple");
        return new TupleValue(values, this.position(start));
    }

    private ConditionalValue conditional(UiLexer.Token start) throws UiParseException {
        this.expect(UiLexer.Type.LEFT_PAREN, "Expected '(' after 'if'");
        Value condition = this.conditionExpression();
        this.expect(UiLexer.Type.RIGHT_PAREN, "Expected ')' after condition");
        Value whenTrue = this.value();
        UiLexer.Token otherwise = this.expect(UiLexer.Type.IDENTIFIER, "Expected 'else' after conditional value");
        if (!otherwise.text().equals("else")) {
            throw this.error(otherwise, "Expected 'else' after conditional value");
        }
        Value whenFalse = this.value();
        return new ConditionalValue(condition, whenTrue, whenFalse, this.position(start));
    }

    private Value conditionExpression() throws UiParseException {
        return this.conditionOr();
    }

    private Value conditionOr() throws UiParseException {
        Value value = this.conditionAnd();
        while (this.matchIdentifier("or")) {
            UiLexer.Token operator = this.previous();
            value = new LogicalValue(
                value,
                LogicalOperator.OR,
                this.conditionAnd(),
                this.position(operator)
            );
        }
        return value;
    }

    private Value conditionAnd() throws UiParseException {
        Value value = this.conditionComparison();
        while (this.matchIdentifier("and")) {
            UiLexer.Token operator = this.previous();
            value = new LogicalValue(
                value,
                LogicalOperator.AND,
                this.conditionComparison(),
                this.position(operator)
            );
        }
        return value;
    }

    private Value conditionComparison() throws UiParseException {
        Value left = this.conditionUnary();
        ComparisonOperator operator = null;
        UiLexer.Token operatorToken = this.peek();
        if (this.match(UiLexer.Type.EQUAL_EQUAL)) {
            operator = ComparisonOperator.EQUAL;
        } else if (this.match(UiLexer.Type.NOT_EQUAL)) {
            operator = ComparisonOperator.NOT_EQUAL;
        } else if (this.match(UiLexer.Type.LESS)) {
            operator = ComparisonOperator.LESS;
        } else if (this.match(UiLexer.Type.LESS_OR_EQUAL)) {
            operator = ComparisonOperator.LESS_OR_EQUAL;
        } else if (this.match(UiLexer.Type.GREATER)) {
            operator = ComparisonOperator.GREATER;
        } else if (this.match(UiLexer.Type.GREATER_OR_EQUAL)) {
            operator = ComparisonOperator.GREATER_OR_EQUAL;
        }
        if (operator == null) {
            return left;
        }
        return new ComparisonValue(
            left,
            operator,
            this.conditionUnary(),
            this.position(operatorToken)
        );
    }

    private Value conditionUnary() throws UiParseException {
        if (this.matchIdentifier("not")) {
            UiLexer.Token operator = this.previous();
            return new NegatedValue(this.conditionUnary(), this.position(operator));
        }
        if (this.match(UiLexer.Type.LEFT_PAREN)) {
            Value nested = this.conditionExpression();
            this.expect(UiLexer.Type.RIGHT_PAREN, "Expected ')' after grouped condition");
            return nested;
        }
        return this.value();
    }

    private Value identifierOrCall(UiLexer.Token first) throws UiParseException {
        if (first.text().equals("true")) {
            return new BooleanValue(true, this.position(first));
        }
        if (first.text().equals("false")) {
            return new BooleanValue(false, this.position(first));
        }
        if (first.text().equals("null")) {
            return new NullValue(this.position(first));
        }

        StringBuilder name = new StringBuilder(first.text());
        while (this.match(UiLexer.Type.DOT)) {
            name.append('.').append(
                this.expect(UiLexer.Type.IDENTIFIER, "Expected name after '.'").text()
            );
        }
        if (!this.match(UiLexer.Type.LEFT_PAREN)) {
            return new BareValue(name.toString(), this.position(first));
        }

        if (name.toString().equals("mat")) {
            if (this.check(UiLexer.Type.RIGHT_PAREN)) {
                throw this.error(this.peek(), "mat() requires one mathematical expression");
            }
            Value expression = this.arithmeticExpression();
            this.expect(UiLexer.Type.RIGHT_PAREN, "Expected ')' after mat expression");
            return new CallValue("mat", List.of(expression), this.position(first));
        }

        List<Value> arguments = this.arguments(UiLexer.Type.RIGHT_PAREN);
        this.expect(UiLexer.Type.RIGHT_PAREN, "Expected ')' after function arguments");
        if (name.toString().equals("element")) {
            if (arguments.size() != 1 || !(arguments.getFirst() instanceof StringValue id)
                || id.value().isBlank()) {
                throw this.error(first, "element() requires one non-empty quoted id");
            }
            this.expect(UiLexer.Type.DOT, "Expected a property after element(...)");
            UiLexer.Token property = this.expect(
                UiLexer.Type.IDENTIFIER, "Expected a property after element(...)."
            );
            return new ElementValue(id.value(), property.text(), this.position(first));
        }
        return new CallValue(name.toString(), arguments, this.position(first));
    }

    private Value arithmeticExpression() throws UiParseException {
        return this.arithmeticAddition();
    }

    private Value arithmeticAddition() throws UiParseException {
        Value value = this.arithmeticMultiplication();
        while (this.check(UiLexer.Type.PLUS) || this.check(UiLexer.Type.MINUS)) {
            UiLexer.Token operator = this.advance();
            value = new ArithmeticValue(
                value,
                operator.type() == UiLexer.Type.PLUS
                    ? ArithmeticOperator.ADD
                    : ArithmeticOperator.SUBTRACT,
                this.arithmeticMultiplication(),
                this.position(operator)
            );
        }
        return value;
    }

    private Value arithmeticMultiplication() throws UiParseException {
        Value value = this.arithmeticUnary();
        while (this.check(UiLexer.Type.STAR)
            || this.check(UiLexer.Type.SLASH)
            || this.check(UiLexer.Type.PERCENT)) {
            UiLexer.Token operator = this.advance();
            ArithmeticOperator operation = switch (operator.type()) {
                case STAR -> ArithmeticOperator.MULTIPLY;
                case SLASH -> ArithmeticOperator.DIVIDE;
                case PERCENT -> ArithmeticOperator.MODULO;
                default -> throw new IllegalStateException();
            };
            value = new ArithmeticValue(
                value,
                operation,
                this.arithmeticUnary(),
                this.position(operator)
            );
        }
        return value;
    }

    private Value arithmeticUnary() throws UiParseException {
        if (this.match(UiLexer.Type.MINUS)) {
            UiLexer.Token operator = this.previous();
            return new ArithmeticNegationValue(this.arithmeticUnary(), this.position(operator));
        }
        if (this.match(UiLexer.Type.PLUS)) {
            return this.arithmeticUnary();
        }
        if (this.match(UiLexer.Type.LEFT_PAREN)) {
            Value grouped = this.arithmeticExpression();
            this.expect(UiLexer.Type.RIGHT_PAREN, "Expected ')' after grouped mathematical expression");
            return grouped;
        }
        return this.value();
    }

    private NumberValue negativeNumber(UiLexer.Token minus) throws UiParseException {
        UiLexer.Token number = this.expect(UiLexer.Type.NUMBER, "Expected a number after '-'");
        return new NumberValue("-" + number.text(), this.position(minus));
    }

    private Value exposedData(UiLexer.Token start) throws UiParseException {
        UiLexer.Token first = this.expect(
            UiLexer.Type.IDENTIFIER,
            "Expected exposed data or glyph name after '${'"
        );
        if (this.match(UiLexer.Type.DOUBLE_COLON)) {
            UiLexer.Token second = this.expect(
                UiLexer.Type.IDENTIFIER,
                "Expected glyph ID or provider after '::'"
            );
            String provider = null;
            String id = second.text();
            if (this.match(UiLexer.Type.SLASH)) {
                provider = second.text();
                id = this.expect(
                    UiLexer.Type.IDENTIFIER,
                    "Expected glyph ID after provider"
                ).text();
            }
            this.expect(UiLexer.Type.RIGHT_BRACE, "Expected '}' after glyph reference");
            return new GlyphValue(
                first.text(), provider, id, this.position(start)
            );
        }
        if (this.match(UiLexer.Type.SLASH)) {
            String id = this.expect(
                UiLexer.Type.IDENTIFIER,
                "Expected glyph ID after provider"
            ).text();
            this.expect(UiLexer.Type.RIGHT_BRACE, "Expected '}' after glyph reference");
            return new GlyphValue(
                null, first.text(), id, this.position(start)
            );
        }

        List<String> path = new ArrayList<>();
        path.add(first.text());
        while (this.match(UiLexer.Type.DOT)) {
            path.add(this.expect(
                UiLexer.Type.IDENTIFIER,
                "Expected exposed data name after '.'"
            ).text());
        }
        this.expect(UiLexer.Type.RIGHT_BRACE, "Expected '}' after exposed data path");
        return new ExposedDataValue(path, this.position(start));
    }

    private List<String> symbolPath(String message) throws UiParseException {
        List<String> path = new ArrayList<>();
        path.add(this.expect(UiLexer.Type.IDENTIFIER, message).text());
        while (this.match(UiLexer.Type.DOT)) {
            path.add(this.expect(UiLexer.Type.IDENTIFIER, "Expected name after '.'").text());
        }
        return path;
    }

    private ListValue list(UiLexer.Token start) throws UiParseException {
        List<Value> values = this.arguments(UiLexer.Type.RIGHT_BRACKET);
        this.expect(UiLexer.Type.RIGHT_BRACKET, "Expected ']' after list");
        return new ListValue(values, this.position(start));
    }

    private ObjectValue object(UiLexer.Token start) throws UiParseException {
        Map<String, Value> values = new LinkedHashMap<>();
        while (!this.check(UiLexer.Type.RIGHT_BRACE) && !this.check(UiLexer.Type.EOF)) {
            if (this.match(UiLexer.Type.COMMA)) {
                continue;
            }
            UiLexer.Token key = this.advance();
            if (key.type() != UiLexer.Type.IDENTIFIER && key.type() != UiLexer.Type.STRING) {
                throw this.error(key, "Expected object property name");
            }
            this.expect(UiLexer.Type.COLON, "Expected ':' after object property name");
            if (values.putIfAbsent(key.text(), this.value()) != null) {
                throw this.error(key, "Duplicate object property '" + key.text() + "'");
            }
            this.match(UiLexer.Type.COMMA);
        }
        this.expect(UiLexer.Type.RIGHT_BRACE, "Expected '}' after object");
        return new ObjectValue(values, this.position(start));
    }

    private List<Value> arguments(UiLexer.Type terminator) throws UiParseException {
        List<Value> arguments = new ArrayList<>();
        while (!this.check(terminator) && !this.check(UiLexer.Type.EOF)) {
            arguments.add(this.value());
            if (!this.match(UiLexer.Type.COMMA) && !this.check(terminator)) {
                throw this.error(this.peek(), "Expected ',' between values");
            }
        }
        return arguments;
    }

    private List<Statement> blockStatements() throws UiParseException {
        if (this.blockDepth >= MAX_BLOCK_DEPTH) {
            throw this.error(
                this.peek(), "UI DSL block nesting exceeds " + MAX_BLOCK_DEPTH + " levels"
            );
        }
        this.blockDepth++;
        try {
            return this.statementsUntil(UiLexer.Type.RIGHT_BRACE);
        } finally {
            this.blockDepth--;
        }
    }

    private List<Statement> sanitizeSymbols(
        List<Statement> statements,
        Set<String> initialSymbols,
        boolean blueprintScope
    ) {
        Set<String> symbols = new HashSet<>(initialSymbols);
        List<Statement> accepted = new ArrayList<>();
        for (Statement statement : statements) {
            String name = null;
            if (blueprintScope) {
                if (statement instanceof VariableDirective variable) {
                    name = variable.name();
                } else if (statement instanceof BlueprintDirective blueprint) {
                    name = blueprint.name();
                }
            } else {
                if (statement instanceof ImportDirective imported) {
                    name = imported.alias();
                } else if (statement instanceof DefinitionDirective definition) {
                    name = definition.name();
                } else if (statement instanceof BlueprintDirective blueprint) {
                    name = blueprint.name();
                }
            }
            if (name != null && !symbols.add(name)) {
                this.diagnostics.add(new UiParseException(
                    this.source,
                    statement.position().line(),
                    statement.position().column(),
                    (blueprintScope ? "Duplicate blueprint-local symbol '" : "Duplicate symbol '")
                        + name + "'"
                ));
                continue;
            }
            if (statement instanceof BlueprintDirective blueprint) {
                accepted.add(new BlueprintDirective(
                    blueprint.name(),
                    blueprint.parameters(),
                    this.sanitizeSymbols(
                        blueprint.body(), new HashSet<>(blueprint.parameters()), true
                    ),
                    blueprint.position()
                ));
            } else {
                accepted.add(statement);
            }
        }
        return List.copyOf(accepted);
    }

    private void synchronizeStatement(
        UiLexer.Type terminator,
        int startIndex
    ) {
        if (this.index <= startIndex && !this.check(UiLexer.Type.EOF)) {
            this.advance();
        }
        int statementLine = this.tokens.get(Math.min(startIndex, this.tokens.size() - 1)).line();
        int braces = 0;
        int parentheses = 0;
        int brackets = 0;
        for (int cursor = startIndex; cursor < this.index && cursor < this.tokens.size(); cursor++) {
            UiLexer.Type type = this.tokens.get(cursor).type();
            switch (type) {
                case LEFT_BRACE -> braces++;
                case RIGHT_BRACE -> braces = Math.max(0, braces - 1);
                case LEFT_PAREN -> parentheses++;
                case RIGHT_PAREN -> parentheses = Math.max(0, parentheses - 1);
                case LEFT_BRACKET -> brackets++;
                case RIGHT_BRACKET -> brackets = Math.max(0, brackets - 1);
                default -> {
                }
            }
        }
        while (!this.check(UiLexer.Type.EOF)) {
            UiLexer.Token token = this.peek();
            boolean atBoundary = braces == 0 && parentheses == 0 && brackets == 0;
            if (atBoundary) {
                if (token.type() == terminator || token.type() == UiLexer.Type.RIGHT_BRACE) {
                    return;
                }
                if (token.type() == UiLexer.Type.COMMA) {
                    this.advance();
                    return;
                }
                if (token.line() > statementLine && isStatementStart(token.type())) {
                    return;
                }
            }
            switch (token.type()) {
                case LEFT_BRACE -> braces++;
                case RIGHT_BRACE -> braces = Math.max(0, braces - 1);
                case LEFT_PAREN -> parentheses++;
                case RIGHT_PAREN -> parentheses = Math.max(0, parentheses - 1);
                case LEFT_BRACKET -> brackets++;
                case RIGHT_BRACKET -> brackets = Math.max(0, brackets - 1);
                default -> {
                }
            }
            this.advance();
        }
    }

    private static boolean isStatementStart(UiLexer.Type type) {
        return type == UiLexer.Type.IDENTIFIER
            || type == UiLexer.Type.STRING
            || type == UiLexer.Type.AT
            || type == UiLexer.Type.DOUBLE_COLON
            || type == UiLexer.Type.AT_AT;
    }

    private boolean match(UiLexer.Type type) {
        if (!this.check(type)) {
            return false;
        }
        this.advance();
        return true;
    }

    private boolean matchIdentifier(String identifier) {
        if (!this.check(UiLexer.Type.IDENTIFIER) || !this.peek().text().equals(identifier)) {
            return false;
        }
        this.advance();
        return true;
    }

    private boolean check(UiLexer.Type type) {
        return this.peek().type() == type;
    }

    private UiLexer.Token expect(UiLexer.Type type, String message) throws UiParseException {
        if (!this.check(type)) {
            throw this.error(this.peek(), message);
        }
        return this.advance();
    }

    private UiLexer.Token advance() {
        UiLexer.Token current = this.peek();
        if (this.index < this.tokens.size() - 1) {
            this.index++;
        }
        return current;
    }

    private UiLexer.Token peek() {
        return this.tokens.get(this.index);
    }

    private UiLexer.Token previous() {
        return this.tokens.get(Math.max(0, this.index - 1));
    }

    private Position position(UiLexer.Token token) {
        return new Position(token.line(), token.column());
    }

    private UiParseException error(UiLexer.Token token, String message) {
        return new UiParseException(this.source, token.line(), token.column(), message);
    }

    public record Result(UiDocument document, List<UiParseException> diagnostics) {
        public Result {
            diagnostics = List.copyOf(diagnostics);
        }
    }
}
