package net.jr.experimental_features.dsl_editor;

import net.jr.client.ui.dsl.UiCompiledDocument;
import net.jr.client.ui.dsl.DslUiContext;
import java.util.Set;
import net.jr.client.ui.dsl.UiCompiledDocument.*;
import net.jr.client.ui.dsl.UiRenderLayer;
import net.jr.client.ui.dsl.UiDocument;
import net.jr.client.ui.dsl.UiDocumentManager;
import net.jr.api.client.resource.Asset;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Hierarchy of *compiled instances*, including Java-owned blocks and imported blueprints. */
final class DslEditorTree {
    private final List<Node> nodes = new ArrayList<>();
    private final Map<String, Node> byId = new LinkedHashMap<>();

    void rebuild(UiCompiledDocument document, DslUiContext<?> context, Set<String> visibleIds) {
        this.nodes.clear(); this.byId.clear();
        UiDocumentManager.getInstance().get(document.source()).ifPresent(source ->
            this.addSourceStatements(source.source(),source.statements(),0,"@document",false)
        );
        this.addNode(new Node("@compiled","compilado",0,"@document",Map.of(),
            document.location("$document"),true));
        if (document.hasBounds()) {
            Map<String,Object> properties=new LinkedHashMap<>();
            try {
                properties.put("x",document.boundsX(context.data()));
                properties.put("y",document.boundsY(context.data()));
                properties.put("w",document.boundsWidth(context.data()));
                properties.put("h",document.boundsHeight(context.data()));
            } catch (RuntimeException exception) {
                properties.put("value","(dinámico)");
            }
            this.addNode(new Node("$bounds","bounds compilado",1,"@compiled",properties,
                document.location("$bounds"),false));
        }
        for (UiRenderLayer layer : UiRenderLayer.values()) {
            for (EntryDefinition entry : document.entries(layer)) {
                this.add(entry, 1, "@compiled/"+layer.name().toLowerCase(), document, context, visibleIds);
            }
        }
    }

    private void addSourceStatements(Asset source,List<UiDocument.Statement> statements,int depth,
                                     String parent,boolean insideBlueprint) {
        for (UiDocument.Statement statement:statements) {
            String id=sourceId(source,statement.position(),statement.getClass().getSimpleName());
            SourceLocation location=new SourceLocation(source,statement.position().line(),statement.position().column());
            if (statement instanceof UiDocument.ImportDirective imported) {
                this.addNode(new Node(id,"import "+imported.alias(),depth,parent,
                    Map.of("ruta",imported.target().path()),location,true));
            } else if (statement instanceof UiDocument.DefinitionDirective definition) {
                this.addSourceValue(source,id,"def "+definition.name(),definition.value(),depth,parent,!insideBlueprint);
            } else if (statement instanceof UiDocument.VariableDirective variable) {
                this.addSourceValue(source,id,"var "+variable.name(),variable.value(),depth,parent,!insideBlueprint);
            } else if (statement instanceof UiDocument.AssignmentDirective assignment) {
                this.addSourceValue(source,id,"set "+assignment.name(),assignment.value(),depth,parent,!insideBlueprint);
            } else if (statement instanceof UiDocument.BlueprintDirective blueprint) {
                this.addNode(new Node(id,"blueprint "+blueprint.name(),depth,parent,
                    Map.of("parámetros",String.join(", ",blueprint.parameters())),location,true));
                this.addSourceStatements(source,blueprint.body(),depth+1,id,true);
            } else if (statement instanceof UiDocument.BlueprintUse use) {
                this.addNode(new Node(id,"llamada "+String.join(".",use.path()),depth,parent,
                    Map.of("argumentos",use.arguments().size()),location,true));
                this.addArguments(source,use.arguments(),depth+1,id,!insideBlueprint);
                for (UiDocument.BlueprintMemberUse member:use.members()) {
                    String memberId=sourceId(source,member.position(),"member");
                    this.addNode(new Node(memberId,"miembro ."+member.name(),depth+1,id,Map.of(),
                        new SourceLocation(source,member.position().line(),member.position().column()),true));
                    this.addArguments(source,member.arguments(),depth+2,memberId,!insideBlueprint);
                }
                this.addSourceStatements(source,use.content(),depth+1,id,insideBlueprint);
            } else if (statement instanceof UiDocument.ConditionalStatement conditional) {
                this.addNode(new Node(id,"if",depth,parent,
                    Map.of("condición",DslConditionalValueTree.format(conditional.condition())),location,true));
                this.addSourceStatements(source,conditional.body(),depth+1,id,insideBlueprint);
            } else if (statement instanceof UiDocument.Node node) {
                this.addNode(new Node(id,node.name(),depth,parent,Map.of(),location,true));
                this.addArguments(source,node.arguments(),depth+1,id,!insideBlueprint);
                this.addSourceStatements(source,node.body(),depth+1,id,insideBlueprint);
            } else if (statement instanceof UiDocument.Property property) {
                this.addSourceValue(source,id,"prop "+property.name(),property.value(),depth,parent,!insideBlueprint);
            } else if (statement instanceof UiDocument.ContentInsertion insertion) {
                this.addNode(new Node(id,"contenido insertado",depth,parent,Map.of(),location,true));
                this.addSourceStatements(insertion.source(),insertion.body(),depth+1,id,insideBlueprint);
            } else {
                this.addNode(new Node(id,statement.getClass().getSimpleName(),depth,parent,Map.of(),location,true));
            }
        }
    }

    private void addArguments(Asset source,List<UiDocument.Value> arguments,int depth,String parent,
                              boolean editable) {
        for (int index=0;index<arguments.size();index++) {
            UiDocument.Value value=arguments.get(index);
            this.addSourceValue(source,sourceId(source,value.position(),"arg"+index),"arg"+index,
                value,depth,parent,editable);
        }
    }

    private void addSourceValue(Asset source,String id,String type,UiDocument.Value value,int depth,
                                String parent,boolean allowEdit) {
        boolean editable=allowEdit && DslConditionalValueTree.isLiteral(value);
        boolean branches=allowEdit && value instanceof UiDocument.ConditionalValue;
        SourceLocation location=new SourceLocation(source,value.position().line(),value.position().column(),
            editable,editable||branches);
        this.addNode(new Node(id,type,depth,parent,
            Map.of("value",DslConditionalValueTree.editableText(value)),location,true));
    }

    private static String sourceId(Asset source,UiDocument.Position position,String kind) {
        return "@source/"+source+"/"+position.line()+":"+position.column()+"/"+kind;
    }

    void clear() { this.nodes.clear(); this.byId.clear(); }

    void addRuntimeWidget(String id, String type, Map<String, Object> properties) {
        this.addNode(new Node(id, type, 0, "runtime", properties,
            new SourceLocation(net.jr.api.client.resource.Asset.MOD("editor/runtime"), 0, 0, false), true));
    }

    List<Node> nodes() { return List.copyOf(this.nodes); }
    List<Node> sourceNodes() {
        return this.nodes.stream().filter(Node::sourceTree).toList();
    }
    List<Node> compiledNodes() {
        return this.nodes.stream().filter(node -> !node.sourceTree()).toList();
    }
    Node node(String id) { return this.byId.get(id); }

    private void add(EntryDefinition entry, int depth, String parent,
                     UiCompiledDocument doc, DslUiContext<?> context, Set<String> visibleIds) {
        if (entry instanceof ConditionalEntryDefinition conditional) {
            String ifId = entry.id() + "/condition";
            this.addNode(new Node(ifId, "if", depth, parent,
                Map.of("condition", evaluate(conditional.condition(), context)),
                doc.location(entry.id()), false));
            this.add(conditional.entry(), depth + 1, ifId, doc, context, visibleIds);
            return;
        }
        String type = entry instanceof DslNodeDefinition node ? node.type() : entry.getClass().getSimpleName()
            .replace("Definition", "");
        Map<String, Object> properties = new LinkedHashMap<>();
        if (entry instanceof DslNodeDefinition node) {
            for (var property : node.properties().entrySet()) {
                properties.put(property.getKey(), evaluate(property.getValue(), context));
            }
        } else {
            reflectProperties(entry, "", properties, context, 0);
        }
        if (entry instanceof ImageDefinition image && image.nine() != null
                && doc.propertyLocation(entry.id(), "nine.borders").editable()) {
            // A single "borders" scalar controls all four regions, not just one side.
            properties.put("nine.borders", evaluate(image.nine().top(), context));
        }
        this.addNode(new Node(entry.id(), type, depth, parent, properties,
            doc.location(entry.id()), visibleIds.contains(entry.id())));
        int childDepth = depth + 1;
        if (entry instanceof DslNodeDefinition node) {
            for (var group : node.renderBlocks().entrySet()) {
                this.addNode(new Node(entry.id() + "/" + group.getKey(), group.getKey(), childDepth,
                    entry.id(), Map.of(), doc.location(entry.id()), false));
                for (EntryDefinition nested : group.getValue()) {
                    this.add(nested, childDepth + 1, entry.id() + "/" + group.getKey(), doc, context, visibleIds);
                }
            }
            for (DslBlockDefinition child : node.children()) {
                this.addBlock(child, childDepth, entry.id(), doc, context, visibleIds);
            }
            if (node.type().equals("item_slots")) {
                for (String id : visibleIds) {
                    if (id.startsWith(entry.id() + "/slot/")) {
                        this.addNode(new Node(id, "slot", childDepth, entry.id(), Map.of(),
                            doc.location(entry.id()), true));
                    }
                }
            }
        } else if (entry instanceof CanvasDefinition canvas) {
            for (EntryDefinition nested : canvas.entries()) this.add(nested, childDepth, entry.id(), doc, context, visibleIds);
        } else if (entry instanceof GridDefinition grid) {
            for (EntryDefinition nested : grid.entries()) this.add(nested, childDepth, entry.id(), doc, context, visibleIds);
        } else if (entry instanceof LoadingBarDefinition bar) {
            for (EntryDefinition nested : bar.background()) this.add(nested, childDepth, entry.id(), doc, context, visibleIds);
            for (EntryDefinition nested : bar.fill()) this.add(nested, childDepth, entry.id(), doc, context, visibleIds);
        }
    }

    private void addBlock(DslBlockDefinition block, int depth, String parent,
                          UiCompiledDocument doc, DslUiContext<?> context, Set<String> visibleIds) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (var property : block.properties().entrySet()) {
            properties.put(property.getKey(), evaluate(property.getValue(), context));
        }
        for (int index = 0; index < block.arguments().size(); index++) {
            properties.put("arg" + index, evaluate(block.arguments().get(index), context));
        }
        this.addNode(new Node(block.id(), block.type(), depth, parent, properties,
            doc.location(block.id()), visibleIds.contains(block.id())));
        for (var group : block.renderBlocks().entrySet()) {
            this.addNode(new Node(block.id() + "/" + group.getKey(), group.getKey(), depth + 1,
                block.id(), Map.of(), doc.location(block.id()), false));
            for (EntryDefinition nested : group.getValue()) {
                this.add(nested, depth + 2, block.id() + "/" + group.getKey(), doc, context, visibleIds);
            }
        }
        for (DslBlockDefinition nested : block.children()) this.addBlock(nested, depth + 1, block.id(), doc, context, visibleIds);
    }

    private void addNode(Node node) {
        this.nodes.add(node);
        this.byId.put(node.id(), node);
    }

    private static Object evaluate(Expression<?> expression, DslUiContext<?> context) {
        if (context == null) return "(sin contexto)";
        try { return String.valueOf(expression.resolve(context.data())); }
        catch (RuntimeException exception) { return "(dinámico)"; }
    }

    private static void reflectProperties(Object object, String prefix, Map<String, Object> output,
                                          DslUiContext<?> context, int depth) {
        if (object == null || depth > 2) return;
        RecordComponent[] components = object.getClass().getRecordComponents();
        if (components == null) return;
        for (RecordComponent component : components) {
            if (component.getName().equals("id")) continue;
            try {
                Object value = component.getAccessor().invoke(object);
                String key = prefix + component.getName();
                if (value instanceof Expression<?> expression) output.put(key, evaluate(expression, context));
                else if (value instanceof List<?> list) {
                    if (!list.isEmpty()) output.put(key, list.size() + " entradas");
                    for (int i = 0; i < list.size(); i++) {
                        Object item = list.get(i);
                        if (item instanceof Expression<?> expression) {
                            output.put(key + "." + i, evaluate(expression, context));
                        }
                    }
                } else if (value != null && value.getClass().isRecord()) {
                    reflectProperties(value, key + ".", output, context, depth + 1);
                }
            } catch (ReflectiveOperationException ignored) { }
        }
    }

    record Node(String id, String type, int depth, String parent,
                Map<String, Object> properties, SourceLocation source, boolean visible) {
        boolean sourceTree() { return this.id.startsWith("@source/"); }
    }
}
