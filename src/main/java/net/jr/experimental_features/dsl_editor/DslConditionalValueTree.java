package net.jr.experimental_features.dsl_editor;

import java.util.ArrayList;
import java.util.List;
import net.jr.client.ui.dsl.UiCompiledDocument;
import net.jr.client.ui.dsl.UiDocument;
import net.jr.client.ui.dsl.UiDocumentManager;

/** Source-side view of conditional values, shared by every DSL document kind. */
final class DslConditionalValueTree {
    private DslConditionalValueTree() { }

    static Details inspect(UiCompiledDocument.SourceLocation location) {
        if (location == null) return null;
        UiDocument document=UiDocumentManager.getInstance().get(location.source()).orElse(null);
        if (document==null) return null;
        UiDocument.Value value=findStatements(document.statements(),location.line(),location.column());
        if (!(value instanceof UiDocument.ConditionalValue conditional)) return null;
        List<Branch> branches=new ArrayList<>();
        append(branches,"true",conditional.whenTrue(),0,location);
        append(branches,"false",conditional.whenFalse(),0,location);
        return new Details(format(conditional.condition()),List.copyOf(branches));
    }

    private static void append(List<Branch> output,String name,UiDocument.Value value,int depth,
                               UiCompiledDocument.SourceLocation owner) {
        boolean editable=owner.branchesEditable() && isLiteral(value);
        output.add(new Branch(name,editableText(value),new UiCompiledDocument.SourceLocation(
            owner.source(),value.position().line(),value.position().column(),editable,editable
        ),editable,depth));
        if (value instanceof UiDocument.ConditionalValue nested) {
            output.add(new Branch("condición",format(nested.condition()),null,false,depth+1));
            append(output,"true",nested.whenTrue(),depth+1,owner);
            append(output,"false",nested.whenFalse(),depth+1,owner);
        }
    }

    static boolean isLiteral(UiDocument.Value value) {
        return value instanceof UiDocument.StringValue || value instanceof UiDocument.NumberValue
            || value instanceof UiDocument.BooleanValue || value instanceof UiDocument.BareValue
            || value instanceof UiDocument.ColorValue || value instanceof UiDocument.NullValue;
    }

    static String editableText(UiDocument.Value value) {
        return value instanceof UiDocument.StringValue text ? text.value() : format(value);
    }

    private static UiDocument.Value findStatements(List<UiDocument.Statement> statements,int line,int column) {
        for (UiDocument.Statement statement:statements) {
            UiDocument.Value found=find(statement,line,column);
            if (found!=null) return found;
        }
        return null;
    }

    private static UiDocument.Value find(UiDocument.Statement statement,int line,int column) {
        if (statement instanceof UiDocument.DefinitionDirective value) return find(value.value(),line,column);
        if (statement instanceof UiDocument.VariableDirective value) return find(value.value(),line,column);
        if (statement instanceof UiDocument.AssignmentDirective value) return find(value.value(),line,column);
        if (statement instanceof UiDocument.Property value) return find(value.value(),line,column);
        if (statement instanceof UiDocument.ConditionalStatement value) {
            UiDocument.Value found=find(value.condition(),line,column);
            return found!=null?found:findStatements(value.body(),line,column);
        }
        if (statement instanceof UiDocument.BlueprintDirective value) return findStatements(value.body(),line,column);
        if (statement instanceof UiDocument.ContentInsertion value) return findStatements(value.body(),line,column);
        if (statement instanceof UiDocument.Node value) {
            UiDocument.Value found=findValues(value.arguments(),line,column);
            return found!=null?found:findStatements(value.body(),line,column);
        }
        if (statement instanceof UiDocument.BlueprintUse value) {
            UiDocument.Value found=findValues(value.arguments(),line,column);
            if (found!=null) return found;
            for (UiDocument.BlueprintMemberUse member:value.members()) {
                found=findValues(member.arguments(),line,column);
                if (found!=null) return found;
            }
            return findStatements(value.content(),line,column);
        }
        return null;
    }

    private static UiDocument.Value findValues(List<UiDocument.Value> values,int line,int column) {
        for (UiDocument.Value value:values) {
            UiDocument.Value found=find(value,line,column);
            if (found!=null) return found;
        }
        return null;
    }

    private static UiDocument.Value find(UiDocument.Value value,int line,int column) {
        if (value.position().line()==line && value.position().column()==column) return value;
        if (value instanceof UiDocument.ConditionalValue v) {
            UiDocument.Value found=find(v.condition(),line,column);
            if (found!=null) return found;
            found=find(v.whenTrue(),line,column);
            return found!=null?found:find(v.whenFalse(),line,column);
        }
        if (value instanceof UiDocument.LogicalValue v) return first(find(v.left(),line,column),find(v.right(),line,column));
        if (value instanceof UiDocument.ComparisonValue v) return first(find(v.left(),line,column),find(v.right(),line,column));
        if (value instanceof UiDocument.ArithmeticValue v) return first(find(v.left(),line,column),find(v.right(),line,column));
        if (value instanceof UiDocument.NegatedValue v) return find(v.value(),line,column);
        if (value instanceof UiDocument.ArithmeticNegationValue v) return find(v.value(),line,column);
        if (value instanceof UiDocument.ListValue v) return findValues(v.values(),line,column);
        if (value instanceof UiDocument.TupleValue v) return findValues(v.values(),line,column);
        if (value instanceof UiDocument.ObjectValue v) return findValues(new ArrayList<>(v.values().values()),line,column);
        if (value instanceof UiDocument.CallValue v) return findValues(v.arguments(),line,column);
        return null;
    }

    private static UiDocument.Value first(UiDocument.Value first,UiDocument.Value second) {
        return first!=null?first:second;
    }

    static String format(UiDocument.Value value) {
        if (value instanceof UiDocument.StringValue v) return '"'+v.value()+'"';
        if (value instanceof UiDocument.NumberValue v) return v.literal();
        if (value instanceof UiDocument.BooleanValue v) return Boolean.toString(v.value());
        if (value instanceof UiDocument.NullValue) return "null";
        if (value instanceof UiDocument.ColorValue v) return v.literal();
        if (value instanceof UiDocument.BareValue v) return v.value();
        if (value instanceof UiDocument.ExposedDataValue v) return "${"+String.join(".",v.path())+"}";
        if (value instanceof UiDocument.RegisteredStyleValue v) return "::"+String.join(".",v.path());
        if (value instanceof UiDocument.BlueprintParameterValue v) return "&"+String.join(".",v.path());
        if (value instanceof UiDocument.ReferenceValue v) return String.join(".",v.path());
        if (value instanceof UiDocument.NegatedValue v) return "not "+format(v.value());
        if (value instanceof UiDocument.ArithmeticNegationValue v) return "-"+format(v.value());
        if (value instanceof UiDocument.LogicalValue v) return format(v.left())+" "+v.operator().name().toLowerCase()+" "+format(v.right());
        if (value instanceof UiDocument.ComparisonValue v) return format(v.left())+" "+operator(v.operator())+" "+format(v.right());
        if (value instanceof UiDocument.ArithmeticValue v) return format(v.left())+" "+operator(v.operator())+" "+format(v.right());
        if (value instanceof UiDocument.ConditionalValue v) return "if ("+format(v.condition())+") …";
        if (value instanceof UiDocument.CallValue v) return v.name()+"(…)";
        return value.getClass().getSimpleName().replace("Value","").toLowerCase();
    }

    private static String operator(Enum<?> operator) {
        return switch(operator.name()) {
            case "EQUAL" -> "=="; case "NOT_EQUAL" -> "!="; case "LESS" -> "<";
            case "LESS_OR_EQUAL" -> "<="; case "GREATER" -> ">"; case "GREATER_OR_EQUAL" -> ">=";
            case "ADD" -> "+"; case "SUBTRACT" -> "-"; case "MULTIPLY" -> "*";
            case "DIVIDE" -> "/"; case "MODULO" -> "%"; default -> operator.name().toLowerCase();
        };
    }

    record Details(String condition,List<Branch> branches) { }
    record Branch(String name,String value,UiCompiledDocument.SourceLocation location,
                  boolean editable,int depth) { }
}
