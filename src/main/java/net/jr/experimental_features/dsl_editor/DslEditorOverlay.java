package net.jr.experimental_features.dsl_editor;

import net.jr.api.client.resource.Asset;
import net.jr.client.ui.container.DslContainerBridge;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.ui.dsl.DslUiContext;
import net.jr.client.ui.dsl.ScreenEditorHooks;
import net.jr.client.ui.dsl.UiCompiledDocument;
import net.jr.client.ui.dsl.UiDocumentManager;
import net.jr.client.ui.dsl.UiDataUsageTracker;
import net.jr.client.ui.screen.DslScreenBridge;
import net.jr.mixin.accessors.ScreenAccesor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import java.util.Collections;
import java.util.IdentityHashMap;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Deque;

/** The normal screen stays alive; this is just an inspector and source editor layered over it. */
final class DslEditorOverlay implements ScreenEditorHooks.Listener {
    private static final int CYAN = 0xFF35D7FF;
    private static final int GREEN = 0xFF48D66D;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int PURPLE = 0xFFCC83FF;
    private static final int TEXT = 0xFFECEFF2;
    private static final int MUTED = 0xFF9BA4B0;
    private static final int RED = 0xFFFF6363;
    private static final int PANEL_WIDTH = 340;
    private static final int TOOLBAR_HEIGHT = 30;
    private static final int HANDLE = 14;

    private Screen screen;
    private final int slotId;
    private final DslEditorTree tree = new DslEditorTree();
    // Retained across F10 open/close: leaving edit mode must not discard the restore point.
    private static final Map<Asset, String> memoryBaseline = new LinkedHashMap<>();
    private static final Set<Asset> touched = new HashSet<>();
    private final Map<String, DslUiContext.ResolvedVisual> boxes = new LinkedHashMap<>();
    private DslUiContext<?> context;
    private UiCompiledDocument compiled;
    private String selectedId;
    private final Set<String> selectedIds = new LinkedHashSet<>();
    private String editingKey;
    private String editingValue = "";
    private int editingCursor;
    private UiDataUsageTracker.Usage editingData;
    private UiCompiledDocument.SourceLocation editingBranch;
    private String error = "";
    private String status = "F10 cierra el editor";
    private int compiledTreeScroll;
    private int sourceTreeScroll;
    private int propertiesScroll;
    private int dataScroll;
    private InspectorTab inspectorTab = InspectorTab.MAIN;
    private boolean structureExpanded = true;
    private boolean propertiesExpanded = true;
    private boolean dataExpanded = true;
    private boolean panelHidden;
    private boolean toolbarHidden;
    private Drag drag = Drag.NONE;
    private double dragStartX, dragStartY;
    private float startX, startY, startW, startH;
    private float editX, editY, editW, editH;
    private boolean dragDirty;
    private boolean selectionPending;
    private boolean selectionControl;
    private boolean selectionShift;
    private double panDragStartX, panDragStartY;
    private float panStartX, panStartY;
    private float zoom = 1.0F;
    private float panX;
    private float panY;
    private double cursorX;
    private double cursorY;
    private final Deque<HistoryEntry> undo = new ArrayDeque<>();
    private final Deque<HistoryEntry> redo = new ArrayDeque<>();
    private boolean applyingHistory;

    DslEditorOverlay(Screen screen) {
        this.screen = screen;
        Integer activeSlot = SlotScope.idOrNull();
        this.slotId = activeSlot == null ? 0 : activeSlot;
    }

    @Override
    public boolean matches(Screen current) {
        Integer activeSlot = SlotScope.idOrNull();
        if (activeSlot != null && activeSlot != slotId) return false;
        // A resource override may rebuild the original Screen instance. Follow an equivalent
        // replacement, but never silently start editing a completely unrelated screen.
        if (current == this.screen) return true;
        if (current instanceof AbstractContainerScreen<?> nextContainer
                && this.screen instanceof AbstractContainerScreen<?> previousContainer
                && current.getClass() == this.screen.getClass()
                && nextContainer.getMenu() == previousContainer.getMenu()) {
            this.screen = current;
            return true;
        }
        DslUiContext<?> next = current == null ? null : findContext(current);
        if (current != null && this.screen != null && current.getClass() == this.screen.getClass()
                && next != null && (this.compiled == null
                || next.compiledDocument().source().equals(this.compiled.source()))) {
            this.screen = current;
            return true;
        }
        return false;
    }

    private static DslUiContext<?> findContext(Screen current) {
        if (current instanceof DslContainerBridge containers) {
            DslUiContext<?> container = containers.javaReforged$peekDslContainerContext();
            if (container != null) return container;
        }
        if (current instanceof DslScreenBridge screens) {
            return screens.javaReforged$peekDslContext();
        }
        return null;
    }

    private void inspect(Screen current) {
        DslUiContext<?> updated = findContext(current);
        this.context = updated;
        this.compiled = updated == null ? null : updated.compiledDocument();
        this.boxes.clear();
        Set<Object> known = Collections.newSetFromMap(new IdentityHashMap<>());
        if (updated != null) {
            known.addAll(updated.boundElements());
            for (DslUiContext.ResolvedVisual box : updated.resolvedVisuals()) {
                if (Float.isFinite(box.x()) && Float.isFinite(box.y())
                        && box.width() > 0 && box.height() > 0) this.boxes.put(box.id(), box);
            }
            this.tree.rebuild(this.compiled, this.context, this.boxes.keySet());
        } else this.tree.clear();
        // Real Java-owned widgets are visible even when they do not declare DSL properties.
        // They are inspectable but not written to .style/.container files.
        int index = 0;
        for (var child : current.children()) {
            if (!(child instanceof AbstractWidget widget) || known.contains(widget)) continue;
            String id = "@java/" + index++;
            this.boxes.put(id, new DslUiContext.ResolvedVisual(id, widget.getX(), widget.getY(),
                widget.getWidth(), widget.getHeight()));
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("message", widget.getMessage().getString());
            properties.put("visible", widget.visible);
            properties.put("active", widget.active);
            properties.put("x", widget.getX()); properties.put("y", widget.getY());
            properties.put("width", widget.getWidth()); properties.put("height", widget.getHeight());
            this.tree.addRuntimeWidget(id, widget.getClass().getSimpleName(), properties);
        }
    }

    @Override
    public void before(Screen current, GuiGraphicsExtractor graphics) {
        // Drawn into ScreenFramebufferPass BEFORE the regular screen, not via a new FBO.
        graphics.fill(0, 0, current.width, current.height, 0x80000000);
        graphics.pose().pushMatrix();
        graphics.pose().translate(this.panX, this.panY);
        graphics.pose().scale(this.zoom, this.zoom);
    }

    @Override
    public void after(Screen current, GuiGraphicsExtractor graphics) {
        graphics.pose().popMatrix();
        this.inspect(current); // The original screen has already laid out and rendered its real objects.
        if (toolbarHidden) {
            this.button(graphics, 7, 5, 26, 20, "+");
        } else {
            this.fill(graphics, 0, 0, current.width, TOOLBAR_HEIGHT, 0xEC1A1E25);
            this.button(graphics, 7, 5, 65, 20, "Guardar");
            this.button(graphics, 77, 5, 83, 20, "Restaurar");
            this.button(graphics, 165, 5, 70, 20, "Deshacer");
            this.button(graphics, 240, 5, 65, 20, "Rehacer");
            this.button(graphics, 310, 5, 96, 20, this.panelHidden ? "Inspector" : "Ocultar");
            this.button(graphics, 411, 5, 25, 20, "-");
            this.label(graphics, this.context == null ? "Widgets Java (sin DSL)" : this.compiled.source().toString(),
                442, 11, MUTED);
        }
        for (String id : this.selectedIds) {
            if (this.boxes.containsKey(id) && !id.equals(this.selectedId)) {
                this.drawSelection(graphics, id, GREEN, false);
            }
        }
        if (this.selectedId != null && this.boxes.containsKey(this.selectedId)) {
            this.drawSelection(graphics, this.selectedId, CYAN, true);
        }
        if (this.drag != Drag.NONE && this.drag != Drag.PAN) this.drawDragDelta(graphics);
        if (!this.panelHidden) this.drawInspector(graphics);
        this.fill(graphics, 0, current.height - 21, current.width, current.height, 0xD51A1E25);
        String message = error.isBlank()
            ? status + " · zoom " + Math.round(this.zoom * 100.0F) + "%"
            : error;
        this.label(graphics, message, 7, current.height - 15,
            error.isBlank() ? MUTED : RED);
    }

    @Override
    public boolean fullWindowViewport() {
        return this.slotId == 0;
    }

    @Override
    public Screen editedScreen() {
        return this.screen;
    }

    @Override
    public void resourcesReloaded() {
        memoryBaseline.clear();
        touched.clear();
        undo.clear();
        redo.clear();
        editingKey=null;
        editingData=null;
        editingBranch=null;
        error="";
        status="Recargado desde el resource pack";
    }

    private int topBar() { return toolbarHidden ? 0 : TOOLBAR_HEIGHT; }

    private void drawInspector(GuiGraphicsExtractor graphics) {
        int left = Math.max(0, this.screen.width - PANEL_WIDTH);
        int top = topBar();
        this.fill(graphics, left, top, this.screen.width, this.screen.height - 21, 0xEC171A20);
        this.fill(graphics, left, top, this.screen.width, top + 28, 0xF62A3039);
        this.label(graphics, "Inspector DSL", left + 9, top + 10, WHITE);
        this.button(graphics, this.screen.width - 27, top + 4, 20, 20, "X");
        this.inspectorTab(graphics, left + 6, top + 31, 78, "Principal", InspectorTab.MAIN);
        this.inspectorTab(graphics, left + 89, top + 31, 92, "Compilado", InspectorTab.COMPILED);
        this.inspectorTab(graphics, left + 186, top + 31, 72, "Fuente", InspectorTab.SOURCE);
        InspectorLayout layout = inspectorLayout();
        if (this.inspectorTab != InspectorTab.MAIN) {
            String title = this.inspectorTab == InspectorTab.SOURCE ? "Árbol fuente" : "Instancias compiladas";
            section(graphics, left, layout.structureHeader(), title, structureExpanded);
            List<DslEditorTree.Node> nodes = this.activeStructureNodes();
            if (structureExpanded) {
                int scroll = this.activeTreeScroll();
                int rows = Math.max(1, layout.structureHeight() / 16);
                for (int i = scroll; i < Math.min(nodes.size(), scroll + rows); i++) {
                    DslEditorTree.Node node = nodes.get(i);
                    int y = layout.structureTop() + (i - scroll) * 16;
                    if (node.id().equals(selectedId)) this.fill(graphics, left + 4, y, this.screen.width - 4, y + 16, 0x9A236E9C);
                    int color = this.compiled != null && node.source().source().equals(this.compiled.source()) ? (node.visible() ? TEXT : MUTED) : PURPLE;
                    this.label(graphics, truncate(node.type() + "  " + node.id(), 48 - node.depth()),
                        left + 8 + Math.min(12, node.depth()) * 10, y + 4, color);
                }
            }
        } else {
            section(graphics, left, layout.propertiesHeader(), "Propiedades y variables", propertiesExpanded);
            DslEditorTree.Node node = tree.node(selectedId);
            if (propertiesExpanded) {
                if (node == null) {
                    this.label(graphics, "Selecciona una instancia", left + 9, layout.propertiesTop() + 7, MUTED);
                } else {
                    int py = layout.propertiesTop() + 4;
                    this.label(graphics, truncate(node.id(), 47), left + 9, py, CYAN);
                    py += 15;
                    this.label(graphics, truncate(node.source().source() + " @ " + node.source().line(), 49),
                        left + 9, py, this.compiled != null && node.source().source().equals(this.compiled.source()) ? MUTED : PURPLE);
                    py += 17;
                    List<ValueRow> props = propertyRows();
                    int available = Math.max(0, (layout.propertiesTop() + layout.propertiesHeight() - py) / 22);
                    for (int i = propertiesScroll; i < Math.min(props.size(), propertiesScroll + available); i++) {
                        ValueRow entry = props.get(i);
                        int row = i - propertiesScroll;
                        int y = py + row * 22;
                        this.fill(graphics, left + 6, y, this.screen.width - 5, y + 20,
                            i % 2 == 0 ? 0xB02D333C : 0x88303941);
                        String key = entry.label();
                        boolean editable = entry.location() != null && entry.location().editable();
                        this.label(graphics, truncate(key, 16), left + 11 + entry.depth()*10, y + 6, editable ? PURPLE : MUTED);
                        boolean branchEditing = entry.location()!=null && entry.location().equals(editingBranch);
                        boolean propertyEditing = editingKey!=null && editingKey.equals(key) && entry.depth()==0;
                        boolean editing = branchEditing || propertyEditing;
                        String value = editing ? editorDisplay(28) : entry.value();
                        if ("true".equals(value) || "false".equals(value)) {
                            this.fill(graphics, left + 148, y + 3, left + 185, y + 18,
                                value.equals("true") ? 0xFF146B60 : 0xFF494D55);
                            this.fill(graphics, value.equals("true") ? left + 169 : left + 151,
                                y + 5, value.equals("true") ? left + 183 : left + 165, y + 16, WHITE);
                        } else this.label(graphics, truncate(value, 28), left + 147, y + 6,
                            (key.equals(editingKey) || editing) && !this.error.isEmpty() ? RED : TEXT);
                    }
                }
            }
            section(graphics, left, layout.dataHeader(), "Datos .data consumidos", dataExpanded);
            if (dataExpanded) {
                List<DataValueRow> usages = dataRows();
                int rows = Math.max(1, layout.dataHeight() / 18);
                for (int i = dataScroll; i < Math.min(usages.size(), dataScroll + rows); i++) {
                    DataValueRow usage = usages.get(i);
                    int y = layout.dataTop() + (i - dataScroll) * 18;
                    this.fill(graphics, left + 6, y, this.screen.width - 5, y + 17,
                        i % 2 == 0 ? 0xB02D333C : 0x88303941);
                    UiCompiledDocument.SourceLocation location = usage.location();
                    boolean editable = location != null && location.editable();
                    boolean branchEditing=location!=null && location.equals(editingBranch);
                    String value = branchEditing || (usage.usage()!=null && sameData(editingData,usage.usage()))
                        ? editorDisplay(17) : usage.value();
                    this.label(graphics, truncate(usage.label(), 28), left + 10 + usage.depth()*10, y + 5,
                        editable ? PURPLE : MUTED);
                    this.label(graphics, truncate(value, 17), left + 220, y + 5, editable ? TEXT : MUTED);
                }
                if (usages.isEmpty()) this.label(graphics, "Aún no se registraron lecturas", left + 9, layout.dataTop() + 7, MUTED);
            }
        }
        if (!error.isBlank()) this.label(graphics, truncate(error, 50), left + 8, this.screen.height - 38, RED);
    }

    private void inspectorTab(GuiGraphicsExtractor graphics, int x, int y, int width,
                              String label, InspectorTab tab) {
        boolean active = this.inspectorTab == tab;
        this.fill(graphics, x, y, x + width, y + 20, active ? 0xFF343C47 : 0xFF22272F);
        this.fill(graphics, x, active ? y : y + 19, x + width, active ? y + 2 : y + 20,
            active ? CYAN : 0xFF4A525E);
        this.label(graphics, label, x + 7, y + 6, active ? WHITE : MUTED);
    }

    private void section(GuiGraphicsExtractor graphics, int left, int y, String title, boolean expanded) {
        this.fill(graphics, left + 3, y, this.screen.width - 3, y + 20, 0xF22A3039);
        this.label(graphics, expanded ? "▼" : "▶", left + 8, y + 6, CYAN);
        this.label(graphics, title, left + 24, y + 6, TEXT);
    }

    private InspectorLayout inspectorLayout() {
        int top = topBar() + 54;
        int usable = Math.max(30, this.screen.height - 21 - top);
        if (this.inspectorTab != InspectorTab.MAIN) {
            int structure = structureExpanded ? Math.max(0, usable - 20) : 0;
            return new InspectorLayout(top, top + 20, structure,
                top, top, 0, top, top, 0);
        }
        int bodies = Math.max(0, usable - 40);
        int properties;
        int data;
        if (propertiesExpanded && dataExpanded) {
            properties = Math.round(bodies * 0.58F);
            data = bodies - properties;
        } else if (propertiesExpanded) {
            properties = bodies;
            data = 0;
        } else {
            properties = 0;
            data = dataExpanded ? bodies : 0;
        }
        int propertiesHeader = top;
        int propertiesTop = propertiesHeader + 20;
        int dataHeader = propertiesTop + properties;
        int dataTop = dataHeader + 20;
        return new InspectorLayout(top, top, 0, propertiesHeader,
            propertiesTop, properties, dataHeader, dataTop, data);
    }

    private List<Map.Entry<String, Object>> properties() {
        DslEditorTree.Node node = tree.node(selectedId);
        if (node == null) return List.of();
        Map<String, Object> map = new LinkedHashMap<>(node.properties());
        DslUiContext.ResolvedVisual visual = boxes.get(selectedId);
        if (visual != null) {
            map.put("x", drag != Drag.NONE ? editX : originalLiteral("x", visual.x()));
            map.put("y", drag != Drag.NONE ? editY : originalLiteral("y", visual.y()));
            map.put("w", drag != Drag.NONE ? editW : originalLiteral("w", visual.width()));
            map.put("h", drag != Drag.NONE ? editH : originalLiteral("h", visual.height()));
        }
        return new ArrayList<>(map.entrySet());
    }

    private List<ValueRow> propertyRows() {
        List<ValueRow> rows=new ArrayList<>();
        for (Map.Entry<String,Object> entry:properties()) {
            UiCompiledDocument.SourceLocation raw=propertyLocation(entry.getKey());
            rows.add(new ValueRow(entry.getKey(),String.valueOf(entry.getValue()),
                raw!=null && raw.editable()?raw:null,0));
            appendConditionalRows(rows,raw);
        }
        return rows;
    }

    private void appendConditionalRows(List<ValueRow> rows,UiCompiledDocument.SourceLocation location) {
        DslConditionalValueTree.Details details=DslConditionalValueTree.inspect(location);
        if (details==null) return;
        rows.add(new ValueRow("condición",details.condition(),null,1));
        for (DslConditionalValueTree.Branch branch:details.branches()) {
            rows.add(new ValueRow("↳ "+branch.name(),branch.value(),
                branch.editable()?branch.location():null,branch.depth()+1));
        }
    }

    private float originalLiteral(String property, float fallback) {
        UiCompiledDocument.SourceLocation location = editableLocation(property);
        if (location == null) return fallback;
        String source = UiDocumentManager.getInstance().sourceText(location.source()).orElse("");
        Float value = DslSourceEditor.numericAt(source, location.line(), location.column());
        return value == null ? fallback : value;
    }

    private String owner() {
        return owner(this.selectedId);
    }

    private static String owner(String id) {
        return id != null && id.contains("/slot/")
            ? id.substring(0, id.indexOf("/slot/")) : id;
    }

    private UiCompiledDocument.SourceLocation editableLocation(String key) {
        return editableLocation(this.selectedId, key);
    }

    private UiCompiledDocument.SourceLocation editableLocation(String id, String key) {
        UiCompiledDocument.SourceLocation found=propertyLocation(id, key);
        return found!=null && found.editable() ? found : null;
    }

    private UiCompiledDocument.SourceLocation propertyLocation(String key) {
        return propertyLocation(this.selectedId, key);
    }

    private UiCompiledDocument.SourceLocation propertyLocation(String id, String key) {
        String owner = owner(id);
        if (compiled == null || owner == null || owner.startsWith("@java/")) return null;
        if (owner.startsWith("@source/") && key.equals("value")) {
            DslEditorTree.Node sourceNode=tree.node(owner);
            return sourceNode==null?null:sourceNode.source();
        }
        UiCompiledDocument.SourceLocation found = compiled.propertyLocation(owner, key);
        if (!found.editable() && !found.branchesEditable() && key.equals("w")) found = compiled.propertyLocation(owner, "width");
        if (!found.editable() && !found.branchesEditable() && key.equals("h")) found = compiled.propertyLocation(owner, "height");
        if (!found.editable() && !found.branchesEditable() && key.equals("width")) found = compiled.propertyLocation(owner, "w");
        if (!found.editable() && !found.branchesEditable() && key.equals("height")) found = compiled.propertyLocation(owner, "h");
        return found;
    }

    private void drawSelection(
            GuiGraphicsExtractor g,
            String id,
            int accent,
            boolean active
    ) {
        DslUiContext.ResolvedVisual box = boxes.get(id);
        boolean draggingActive = active && drag != Drag.NONE && drag != Drag.PAN;
        boolean draggingSelection = drag == Drag.MOVE && selectedIds.contains(id);
        float moveX = editX - startX;
        float moveY = editY - startY;
        float logicalX = draggingActive ? editX
            : draggingSelection && canMoveX(id) ? box.x() + moveX : box.x();
        float logicalY = draggingActive ? editY
            : draggingSelection && canMoveY(id) ? box.y() + moveY : box.y();
        float logicalWidth = draggingActive ? editW : box.width();
        float logicalHeight = draggingActive ? editH : box.height();
        int l = Math.round(this.viewX(logicalX));
        int t = Math.round(this.viewY(logicalY));
        int r = Math.round(this.viewX(logicalX + logicalWidth));
        int b = Math.round(this.viewY(logicalY + logicalHeight));
        if (r <= l || b <= t) return;
        // White 2 px exterior within bounds, cyan 2 px inner line.
        fill(g, l, t, r, t + 2, WHITE); fill(g, l, b - 2, r, b, WHITE);
        fill(g, l, t + 2, l + 2, b - 2, WHITE); fill(g, r - 2, t + 2, r, b - 2, WHITE);
        if (r - l >= 6 && b - t >= 6) {
            fill(g, l + 2, t + 2, r - 2, t + 4, accent);
            fill(g, l + 2, b - 4, r - 2, b - 2, accent);
            fill(g, l + 2, t + 4, l + 4, b - 4, accent);
            fill(g, r - 4, t + 4, r - 2, b - 4, accent);
        }
        if (!active) return;
        if (isReadOnly()) return;
        boolean left = canResizeLeft();
        boolean right = canResizeRight();
        boolean top = canResizeTop();
        boolean bottom = canResizeBottom();
        if (left && top) handle(g,l,t,accent);
        if (right && top) handle(g,r,t,accent);
        if (left && bottom) handle(g,l,b,accent);
        if (right && bottom) handle(g,r,b,accent);
        if (top) handle(g,(l+r)/2,t,accent);
        if (bottom) handle(g,(l+r)/2,b,accent);
        if (left) handle(g,l,(t+b)/2,accent);
        if (right) handle(g,r,(t+b)/2,accent);
    }

    private boolean isReadOnly() {
        return editableLocation("x") == null && editableLocation("y") == null
            && editableLocation("w") == null && editableLocation("h") == null;
    }

    private boolean canMoveX() { return editableLocation("x") != null; }
    private boolean canMoveY() { return editableLocation("y") != null; }
    private boolean canMoveX(String id) { return editableLocation(id, "x") != null; }
    private boolean canMoveY(String id) { return editableLocation(id, "y") != null; }
    private boolean canResizeLeft() { return canMoveX() && editableLocation("w") != null; }
    private boolean canResizeRight() { return editableLocation("w") != null; }
    private boolean canResizeTop() { return canMoveY() && editableLocation("h") != null; }
    private boolean canResizeBottom() { return editableLocation("h") != null; }
    private void handle(GuiGraphicsExtractor g, int x, int y, int accent) {
        int half = HANDLE / 2;
        fill(g,x-half,y-half,x+half,y+half,WHITE);
        fill(g,x-half+2,y-half+2,x+half-2,y+half-2,accent);
    }
    private void button(GuiGraphicsExtractor g, int x, int y, int width, int height, String label) {
        fill(g,x,y,x+width,y+height,0xFF373D47);
        fill(g,x,y,x+width,y+1,CYAN);
        label(g,label,x+5,y+7,TEXT);
    }
    private void label(GuiGraphicsExtractor g, String value, int x, int y, int color) {
        g.text(Minecraft.getInstance().font, Component.literal(value), x, y, color, false);
    }
    private void fill(GuiGraphicsExtractor g, int l,int t,int r,int b,int color) {
        if (r > l && b > t) g.fill(l,t,r,b,color);
    }
    private static String truncate(String text,int max) {
        return text.length() > max ? text.substring(0, Math.max(1,max-1)) + "…" : text;
    }

    private String editorDisplay(int max) {
        int cursor=clamp(this.editingCursor,0,this.editingValue.length());
        String text=this.editingValue.substring(0,cursor)+"|"+this.editingValue.substring(cursor);
        if (text.length()<=max) return text;
        int start=clamp(cursor-max/2,0,Math.max(0,text.length()-max));
        String visible=text.substring(start,Math.min(text.length(),start+max));
        if (start>0 && visible.length()>1) visible="…"+visible.substring(1);
        if (start+max<text.length() && visible.length()>1)
            visible=visible.substring(0,visible.length()-1)+"…";
        return visible;
    }

    @Override
    public boolean mouseButton(Screen s, double x, double y, int button, int action) {
        this.cursorX = x;
        this.cursorY = y;
        if (action == GLFW.GLFW_RELEASE) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && drag == Drag.PAN) {
                drag = Drag.NONE;
                dragDirty = false;
                return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                boolean changed = dragDirty;
                if (drag != Drag.NONE) finishDrag();
                if (selectionPending && !changed) {
                    applySelectionAt(x, y, selectionControl, selectionShift);
                }
                selectionPending = false;
                selectionControl = false;
                selectionShift = false;
            }
            return true;
        }
        if (action != GLFW.GLFW_PRESS) return true;
        if (toolbarHidden && inside(x,y,7,5,26,20)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) toolbarHidden=false;
            return true;
        }
        if (!toolbarHidden && y < TOOLBAR_HEIGHT) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (inside(x,y,7,5,65,20)) save();
                else if (inside(x,y,77,5,83,20)) restore();
                else if (inside(x,y,165,5,70,20)) undo();
                else if (inside(x,y,240,5,65,20)) redo();
                else if (inside(x,y,310,5,96,20)) panelHidden = !panelHidden;
                else if (inside(x,y,411,5,25,20)) toolbarHidden = true;
            }
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (!panelHidden && x >= Math.max(0, s.width - PANEL_WIDTH)) {
                return true;
            }
            drag = Drag.PAN;
            panDragStartX = x;
            panDragStartY = y;
            panStartX = panX;
            panStartY = panY;
            dragDirty = false;
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        if (!panelHidden && x >= Math.max(0,s.width-PANEL_WIDTH)) {
            inspectorClick(x,y);
            return true;
        }
        selectionPending = true;
        selectionControl = modifierDown(
                GLFW.GLFW_KEY_LEFT_CONTROL,
                GLFW.GLFW_KEY_RIGHT_CONTROL
        );
        selectionShift = modifierDown(
                GLFW.GLFW_KEY_LEFT_SHIFT,
                GLFW.GLFW_KEY_RIGHT_SHIFT
        );
        dragDirty = false;
        if (selectedId != null && !isReadOnly()) {
            Drag hit = hitHandle(x,y);
            if (hit != Drag.NONE) {
                beginDrag(hit,this.logicalX(x),this.logicalY(y));
                return true;
            }
            DslUiContext.ResolvedVisual selected = boxes.get(selectedId);
            double logicalX = logicalX(x);
            double logicalY = logicalY(y);
            if (selected != null
                && logicalX >= selected.x() && logicalX < selected.x() + selected.width()
                && logicalY >= selected.y() && logicalY < selected.y() + selected.height()) {
                if (canMoveX() || canMoveY()) beginDrag(Drag.MOVE, logicalX, logicalY);
                return true;
            }
        }
        return true;
    }

    private void applySelectionAt(double x, double y, boolean control, boolean shift) {
        double logicalX = this.logicalX(x);
        double logicalY = this.logicalY(y);
        DslUiContext.ResolvedVisual found = this.boxes.values().stream()
            .filter(b -> logicalX >= b.x() && logicalX < b.x()+b.width()
                && logicalY >= b.y() && logicalY < b.y()+b.height())
            .min(Comparator.comparingDouble(b -> b.width()*b.height())).orElse(null);
        if (found != null) {
            if (control && shift) {
                this.removeSelection(found.id());
            } else if (control) {
                this.addSelection(found.id());
            } else {
                this.selectOnly(found.id());
            }
        } else {
            this.clearSelection();
            drag = Drag.NONE;
        }
    }

    private void selectOnly(String id) {
        this.selectedIds.clear();
        this.selectedIds.add(id);
        this.activateSelection(id);
    }

    private void addSelection(String id) {
        // Reinsert it so iteration order also represents selection recency.
        this.selectedIds.remove(id);
        this.selectedIds.add(id);
        this.activateSelection(id);
    }

    private void removeSelection(String id) {
        if (!this.selectedIds.remove(id)) {
            return;
        }
        if (id.equals(this.selectedId)) {
            String next = null;
            for (String candidate : this.selectedIds) {
                next = candidate;
            }
            if (next == null) {
                this.clearSelection();
            } else {
                this.activateSelection(next);
            }
        }
    }

    private void activateSelection(String id) {
        this.selectedId = id;
        this.panelHidden = false;
        this.propertiesScroll = 0;
        this.editingKey = null;
        this.editingData = null;
        this.editingBranch = null;
        this.error = "";
    }

    private void clearSelection() {
        this.selectedIds.clear();
        this.selectedId = null;
        this.editingKey = null;
        this.editingData = null;
        this.editingBranch = null;
    }

    private static boolean modifierDown(int leftKey, int rightKey) {
        long window = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(window, leftKey) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, rightKey) == GLFW.GLFW_PRESS;
    }

    private void inspectorClick(double x, double y) {
        int top = topBar();
        int left = Math.max(0, this.screen.width - PANEL_WIDTH);
        if (inside(x,y,screen.width-27,top+4,20,20)) {
            panelHidden = true; editingKey = null; editingData = null; editingBranch = null; error = "";
            return;
        }
        if (inside(x, y, left + 6, top + 31, 78, 20)) {
            this.selectInspectorTab(InspectorTab.MAIN);
            return;
        }
        if (inside(x, y, left + 89, top + 31, 92, 20)) {
            this.selectInspectorTab(InspectorTab.COMPILED);
            return;
        }
        if (inside(x, y, left + 186, top + 31, 72, 20)) {
            this.selectInspectorTab(InspectorTab.SOURCE);
            return;
        }
        InspectorLayout layout = inspectorLayout();
        if (this.inspectorTab != InspectorTab.MAIN
                && y >= layout.structureHeader() && y < layout.structureHeader() + 20) {
            structureExpanded = !structureExpanded; return;
        }
        if (this.inspectorTab == InspectorTab.MAIN
                && y >= layout.propertiesHeader() && y < layout.propertiesHeader() + 20) {
            propertiesExpanded = !propertiesExpanded; return;
        }
        if (this.inspectorTab == InspectorTab.MAIN
                && y >= layout.dataHeader() && y < layout.dataHeader() + 20) {
            dataExpanded = !dataExpanded; return;
        }
        if (this.inspectorTab != InspectorTab.MAIN && structureExpanded
                && y >= layout.structureTop() && y < layout.structureTop()+layout.structureHeight()) {
            int row = (int)(y-layout.structureTop())/16;
            int index = this.activeTreeScroll() + row;
            List<DslEditorTree.Node> nodes = this.activeStructureNodes();
            if (index>=0 && index<nodes.size()) {
                this.selectOnly(nodes.get(index).id());
            }
            return;
        }
        DslEditorTree.Node node = tree.node(selectedId);
        int py = layout.propertiesTop() + (node == null ? 0 : 36);
        if (this.inspectorTab == InspectorTab.MAIN && propertiesExpanded && node != null
                && y >= py && y < layout.propertiesTop()+layout.propertiesHeight()) {
            int index=propertiesScroll+(int)(y-py)/22;
            List<ValueRow> props=propertyRows();
            if (index<0 || index>=props.size()) return;
            ValueRow prop=props.get(index);
            String key=prop.label();
            UiCompiledDocument.SourceLocation loc=prop.location();
            if (loc==null) { error="Propiedad de solo lectura o calculada"; return; }
            String val=prop.value();
            if (val.equals("true") || val.equals("false")) {
                if (key.startsWith("↳")) commitBranch(loc,val.equals("true")?"false":"true");
                else commitValue(key,val.equals("true") ? "false" : "true");
            } else {
                if (key.startsWith("↳")) { editingBranch=loc; editingKey=null; }
                else { editingKey=key; editingBranch=null; }
                editingData=null;
                editingValue=val;
                editingCursor=editingValue.length();
                error="";
            }
            return;
        }
        if (this.inspectorTab == InspectorTab.MAIN && dataExpanded
                && y >= layout.dataTop() && y < layout.dataTop()+layout.dataHeight()) {
            int index=dataScroll+(int)(y-layout.dataTop())/18;
            List<DataValueRow> usages=dataRows();
            if (index<0 || index>=usages.size()) return;
            DataValueRow row=usages.get(index);
            UiDataUsageTracker.Usage usage=row.usage();
            UiCompiledDocument.SourceLocation location=row.location();
            if (location==null || !location.editable()) {
                error="Dato calculado o proveniente de un style: solo lectura"; return;
            }
            String value=row.value();
            if (usage==null) {
                if ("true".equals(value)||"false".equals(value)) commitBranch(location,value.equals("true")?"false":"true");
                else {editingBranch=location;editingData=null;editingKey=null;editingValue=value;editingCursor=value.length();error="";}
            } else if ("true".equals(value) || "false".equals(value)) commitData(usage,value.equals("true")?"false":"true");
            else { editingData=usage; editingBranch=null; editingKey=null; editingValue=value; editingCursor=value.length(); error=""; }
        }
    }

    private void selectInspectorTab(InspectorTab tab) {
        if (this.inspectorTab == tab) return;
        this.inspectorTab = tab;
        this.editingKey = null;
        this.editingData = null;
        this.editingBranch = null;
        this.error = "";
    }

    private List<DslEditorTree.Node> activeStructureNodes() {
        return this.inspectorTab == InspectorTab.SOURCE
            ? this.tree.sourceNodes()
            : this.tree.compiledNodes();
    }

    private int activeTreeScroll() {
        return this.inspectorTab == InspectorTab.SOURCE
            ? this.sourceTreeScroll
            : this.compiledTreeScroll;
    }

    private void scrollActiveTree(int amount) {
        List<DslEditorTree.Node> nodes = this.activeStructureNodes();
        if (this.inspectorTab == InspectorTab.SOURCE) {
            this.sourceTreeScroll = clamp(this.sourceTreeScroll + amount, 0, Math.max(0, nodes.size() - 1));
        } else {
            this.compiledTreeScroll = clamp(this.compiledTreeScroll + amount, 0, Math.max(0, nodes.size() - 1));
        }
    }

    private List<DataValueRow> dataRows() {
        List<DataValueRow> rows=new ArrayList<>();
        UiDocumentManager manager=UiDocumentManager.getInstance();
        for (UiDataUsageTracker.Usage usage:UiDataUsageTracker.usages(this.screen)) {
            UiCompiledDocument.SourceLocation location=manager.dataLocation(usage.file(),usage.id());
            rows.add(new DataValueRow(usage.id(),String.valueOf(usage.value()),
                location!=null && location.editable()?location:null,0,usage));
            DslConditionalValueTree.Details details=DslConditionalValueTree.inspect(location);
            if (details==null) continue;
            rows.add(new DataValueRow("condición",details.condition(),null,1,null));
            for (DslConditionalValueTree.Branch branch:details.branches()) {
                rows.add(new DataValueRow("↳ "+branch.name(),branch.value(),
                    branch.editable()?branch.location():null,branch.depth()+1,null));
            }
        }
        return rows;
    }

    private static boolean sameData(UiDataUsageTracker.Usage first, UiDataUsageTracker.Usage second) {
        return first != null && first.file().equals(second.file()) && first.id().equals(second.id());
    }

    private void commitBranch(UiCompiledDocument.SourceLocation location,String value) {
        UiDocumentManager manager=UiDocumentManager.getInstance();
        try {
            value=DslEditorMath.resolve(value);
            Asset asset=location.source();
            String original=manager.sourceText(asset).orElseThrow(() -> new IllegalArgumentException("Fuente no cargada"));
            String updated=DslSourceEditor.replaceScalars(original,
                List.of(new DslSourceEditor.ValueEdit(location.line(),location.column(),value)));
            memoryBaseline.putIfAbsent(asset,original);
            applyChecked(manager,asset,original,updated);
            touched.add(asset); editingBranch=null; error="";
            status="Rama condicional actualizada: "+asset;
        } catch (RuntimeException exception) {
            error=exception.getMessage()==null?"Valor incompatible":exception.getMessage();
        }
    }

    private void commitData(UiDataUsageTracker.Usage usage,String value) {
        UiDocumentManager manager=UiDocumentManager.getInstance();
        UiCompiledDocument.SourceLocation location=manager.dataLocation(usage.file(),usage.id());
        if (location==null || !location.editable()) { error="Este dato no es un literal editable"; return; }
        try {
            value=DslEditorMath.resolve(value);
            Asset asset=location.source();
            String original=manager.sourceText(asset).orElseThrow(() -> new IllegalArgumentException("Fuente no cargada"));
            String updated=DslSourceEditor.replaceScalars(original,
                List.of(new DslSourceEditor.ValueEdit(location.line(),location.column(),value)));
            memoryBaseline.putIfAbsent(asset,original);
            applyChecked(manager,asset,original,updated);
            touched.add(asset);
            editingData=null; error=""; status="Dato actualizado en memoria: "+asset;
        } catch (RuntimeException exception) {
            error=exception.getMessage()==null?"Valor incompatible":exception.getMessage();
        }
    }

    private void commitValue(String key, String value) {
        UiCompiledDocument.SourceLocation location=editableLocation(key);
        if (location==null) { error="Esta propiedad no tiene un literal editable por instancia"; return; }
        UiDocumentManager manager=UiDocumentManager.getInstance();
        try {
            value = DslEditorMath.resolve(value);
            Asset asset=location.source();
            String original=manager.sourceText(asset).orElseThrow(() -> new IllegalArgumentException("Fuente no cargada"));
            String updated=DslSourceEditor.replaceScalars(original,
                List.of(new DslSourceEditor.ValueEdit(location.line(),location.column(),value)));
            memoryBaseline.putIfAbsent(asset,original);
            applyChecked(manager,asset,original,updated);
            touched.add(asset);
            status="Actualizado en memoria: "+asset;
            error="";
            editingKey=null;
        } catch (RuntimeException exception) {
            error=exception.getMessage()==null ? "Valor incompatible" : exception.getMessage();
        }
    }

    @Override
    public boolean mouseMove(Screen s, double x, double y) {
        this.cursorX = x;
        this.cursorY = y;
        if (drag==Drag.NONE) return true;
        if (drag==Drag.PAN) {
            panX = panStartX + (float)(x - panDragStartX);
            panY = panStartY + (float)(y - panDragStartY);
            clampPan();
            dragDirty = true;
            return true;
        }
        int dx=Math.round((float)(this.logicalX(x)-dragStartX));
        int dy=Math.round((float)(this.logicalY(y)-dragStartY));
        float right=startX+startW, bottom=startY+startH;
        editX=drag.left? Math.min(right-1,startX+dx) : startX;
        editY=drag.top? Math.min(bottom-1,startY+dy) : startY;
        float newRight=drag.right ? Math.max(editX+1,right+dx) : right;
        float newBottom=drag.bottom ? Math.max(editY+1,bottom+dy) : bottom;
        if (drag==Drag.MOVE) {
            editX=canMoveX() ? Math.round(startX+dx) : startX;
            editY=canMoveY() ? Math.round(startY+dy) : startY;
            editW=startW;
            editH=startH;
        }
        else {
            editX=drag.left ? Math.round(editX) : startX;
            editY=drag.top ? Math.round(editY) : startY;
            editW=Math.round(newRight-editX);
            editH=Math.round(newBottom-editY);
        }
        dragDirty=true;
        return true;
    }

    private void beginDrag(Drag mode, double x,double y) {
        DslUiContext.ResolvedVisual box=boxes.get(selectedId);
        if (box==null) return;
        drag=mode; dragStartX=x; dragStartY=y;
        startX=editX=box.x(); startY=editY=box.y(); startW=editW=box.width(); startH=editH=box.height();
        dragDirty=false;
    }

    private void drawDragDelta(GuiGraphicsExtractor graphics) {
        int dx = Math.round((float)(this.logicalX(cursorX) - dragStartX));
        int dy = Math.round((float)(this.logicalY(cursorY) - dragStartY));
        if (drag == Drag.MOVE) {
            if (!canMoveX()) dx = 0;
            if (!canMoveY()) dy = 0;
        }
        String text = "[X " + direction(dx, "←", "→") + ", Y " + direction(dy, "↑", "↓") + "]";
        int width = Minecraft.getInstance().font.width(text) + 10;
        int x = clamp((int)cursorX + 13, 2, Math.max(2, this.screen.width - width - 2));
        int y = clamp((int)cursorY + 13, this.topBar() + 2, Math.max(this.topBar() + 2, this.screen.height - 35));
        this.fill(graphics, x, y, x + width, y + 18, 0xF21A1E25);
        this.fill(graphics, x, y, x + width, y + 1, CYAN);
        this.label(graphics, text, x + 5, y + 6, TEXT);
    }

    private static String direction(int value, String negative, String positive) {
        if (value == 0) return "0";
        return (value < 0 ? negative : positive) + " " + Math.abs(value);
    }
    private void finishDrag() {
        Drag finishedDrag=drag;
        drag=Drag.NONE;
        if (!dragDirty || selectedId==null || compiled==null) return;
        DslUiContext.ResolvedVisual old=boxes.get(selectedId);
        if (old==null) return;
        UiDocumentManager manager=UiDocumentManager.getInstance();
        try {
            float dx=editX-old.x(),dy=editY-old.y();
            Map<Asset, Map<String, DslSourceEditor.ValueEdit>> editsBySource=new LinkedHashMap<>();
            if (finishedDrag==Drag.MOVE) {
                for (String id : selectedIds) {
                    DslUiContext.ResolvedVisual box=boxes.get(id);
                    if (box==null) continue;
                    addMoveEdit(editsBySource,id,"x",0,dx,box);
                    addMoveEdit(editsBySource,id,"y",1,dy,box);
                }
            } else {
                float[] values={dx,dy,editW-old.width(),editH-old.height()};
                String[] keys={"x","y","w","h"};
                for (int i=0;i<keys.length;i++) {
                    if (Math.abs(values[i]) < 0.001F) continue;
                    addValueEdit(editsBySource,selectedId,keys[i],i,values[i],old);
                }
            }
            if (editsBySource.isEmpty()) return;
            Map<Asset,String> originals=new LinkedHashMap<>();
            Map<Asset,String> updates=new LinkedHashMap<>();
            for (Map.Entry<Asset,Map<String,DslSourceEditor.ValueEdit>> entry : editsBySource.entrySet()) {
                String original=manager.sourceText(entry.getKey()).orElseThrow();
                originals.put(entry.getKey(),original);
                updates.put(entry.getKey(),DslSourceEditor.replaceValues(original,new ArrayList<>(entry.getValue().values())));
                memoryBaseline.putIfAbsent(entry.getKey(),original);
            }
            applyChecked(manager,originals,updates);
            originals.keySet().forEach(source -> updateTouched(source,updates.get(source)));
            status=finishedDrag==Drag.MOVE && selectedIds.size()>1
                ? selectedIds.size()+" instancias movidas en memoria"
                : "Instancia modificada en memoria: "+originals.keySet().iterator().next();
            error="";
        } catch (RuntimeException exception) {
            error=exception.getMessage()==null ? "No se pudo modificar la instancia" : exception.getMessage();
        }
    }

    private void addMoveEdit(Map<Asset,Map<String,DslSourceEditor.ValueEdit>> editsBySource,
                             String id,String key,int dimension,float delta,DslUiContext.ResolvedVisual box) {
        if (Math.abs(delta)<0.001F || editableLocation(id,key)==null) return;
        addValueEdit(editsBySource,id,key,dimension,delta,box);
    }

    private void addValueEdit(Map<Asset,Map<String,DslSourceEditor.ValueEdit>> editsBySource,
                              String id,String key,int dimension,float delta,DslUiContext.ResolvedVisual box) {
        UiCompiledDocument.SourceLocation location=editableLocation(id,key);
        if (location==null) throw new IllegalArgumentException(key+": no hay un argumento de instancia editable");
        float raw=resolveOriginal(id,key,box,dimension);
        DslSourceEditor.ValueEdit edit=new DslSourceEditor.ValueEdit(location.line(),location.column(),
            Integer.toString(Math.round(raw+delta)));
        Map<String,DslSourceEditor.ValueEdit> sourceEdits=editsBySource.computeIfAbsent(location.source(),ignored -> new LinkedHashMap<>());
        String position=location.line()+":"+location.column();
        DslSourceEditor.ValueEdit previous=sourceEdits.putIfAbsent(position,edit);
        if (previous!=null && !previous.value().equals(edit.value()))
            throw new IllegalArgumentException("La selección comparte un argumento con valores incompatibles");
    }

    private float resolveOriginal(String id,String key,DslUiContext.ResolvedVisual box,int dimension) {
        if (compiled==null || context==null) return fallback(box,dimension);
        UiCompiledDocument.Expression<?> expr=null;
        UiCompiledDocument.DslNodeDefinition node=compiled.elements().get(owner(id));
        if (node!=null) {
            String name=key.equals("w") ? "width" : key.equals("h") ? "height" : key;
            expr=node.properties().get(name);
        }
        if (expr!=null) {
            try {Object val=expr.resolve(context.data()); if (val instanceof Number n) return n.floatValue();}
            catch (RuntimeException ignored) { }
        }
        // For blueprint arguments this uses the actual source value where possible. Anchoring
        // means resolved coordinates may differ from raw source coordinates.
        UiCompiledDocument.SourceLocation loc=editableLocation(id,dimension==2?"w":dimension==3?"h":key);
        if (loc!=null) {
            String text=UiDocumentManager.getInstance().sourceText(loc.source()).orElse("");
            Float literal=DslSourceEditor.numericAt(text,loc.line(),loc.column());
            if (literal!=null) return literal;
        }
        return fallback(box,dimension);
    }
    private static float fallback(DslUiContext.ResolvedVisual b,int d) {
        return switch(d) {case 0 -> b.x(); case 1 -> b.y(); case 2 -> b.width(); default -> b.height();};
    }
    private Drag hitHandle(double x,double y) {
        DslUiContext.ResolvedVisual box=boxes.get(selectedId);
        if (box==null) return Drag.NONE;
        int l=Math.round(this.viewX(box.x())),t=Math.round(this.viewY(box.y()));
        int r=Math.round(this.viewX(box.x()+box.width()));
        int b=Math.round(this.viewY(box.y()+box.height()));
        int mx=(l+r)/2,my=(t+b)/2;
        if (canResizeLeft() && canResizeTop() && near(x,y,l,t))return Drag.TL;
        if (canResizeRight() && canResizeTop() && near(x,y,r,t))return Drag.TR;
        if (canResizeLeft() && canResizeBottom() && near(x,y,l,b))return Drag.BL;
        if (canResizeRight() && canResizeBottom() && near(x,y,r,b))return Drag.BR;
        if (canResizeTop() && near(x,y,mx,t))return Drag.TOP;
        if (canResizeBottom() && near(x,y,mx,b))return Drag.BOTTOM;
        if (canResizeLeft() && near(x,y,l,my))return Drag.LEFT;
        if (canResizeRight() && near(x,y,r,my))return Drag.RIGHT;
        return Drag.NONE;
    }
    private static boolean near(double x,double y,int cx,int cy) {
        return Math.abs(x-cx)<=HANDLE/2.0 && Math.abs(y-cy)<=HANDLE/2.0;
    }
    private static boolean inside(double x,double y,int l,int t,int w,int h) {
        return x>=l && y>=t && x<l+w && y<t+h;
    }

    @Override
    public boolean mouseScroll(Screen s,double x,double y,double horizontal,double vertical) {
        if (!panelHidden && x>=s.width-PANEL_WIDTH) {
            InspectorLayout layout = inspectorLayout();
            if (this.inspectorTab != InspectorTab.MAIN && structureExpanded
                    && y>=layout.structureTop() && y<layout.structureTop()+layout.structureHeight()) {
                this.scrollActiveTree(vertical < 0 ? 2 : -2);
            } else if (this.inspectorTab == InspectorTab.MAIN && propertiesExpanded
                    && y>=layout.propertiesTop() && y<layout.propertiesTop()+layout.propertiesHeight()) {
                propertiesScroll=clamp(propertiesScroll+(vertical<0?2:-2),0,Math.max(0,propertyRows().size()-1));
            } else if (this.inspectorTab == InspectorTab.MAIN && dataExpanded
                    && y>=layout.dataTop() && y<layout.dataTop()+layout.dataHeight())
                dataScroll=clamp(dataScroll+(vertical<0?2:-2),0,
                    Math.max(0,dataRows().size()-1));
            return true;
        }
        if (vertical != 0.0D) {
            float previous = this.zoom;
            float factor = vertical > 0.0D ? 1.12F : 1.0F / 1.12F;
            this.zoom = Math.max(1.0F, Math.min(5.0F, previous * factor));
            if (this.zoom != previous) {
                this.panX = (float)x - ((float)x - this.panX) * this.zoom / previous;
                this.panY = (float)y - ((float)y - this.panY) * this.zoom / previous;
                this.clampPan();
            }
            status="Zoom "+Math.round(this.zoom*100.0F)+"%";
        }
        return true;
    }
    private static int clamp(int val,int min,int max) {return Math.min(max,Math.max(min,val));}

    private float viewX(float logicalX) { return this.panX + logicalX * this.zoom; }
    private float viewY(float logicalY) { return this.panY + logicalY * this.zoom; }
    private double logicalX(double viewX) { return (viewX - this.panX) / this.zoom; }
    private double logicalY(double viewY) { return (viewY - this.panY) / this.zoom; }

    private void clampPan() {
        float minimumX = this.screen.width * (1.0F - this.zoom);
        float minimumY = this.screen.height * (1.0F - this.zoom);
        this.panX = Math.max(minimumX, Math.min(0.0F, this.panX));
        this.panY = Math.max(minimumY, Math.min(0.0F, this.panY));
    }

    @Override
    public boolean keyPress(Screen s,int action,KeyEvent event) {
        if (action==GLFW.GLFW_RELEASE) return true;
        if (event.key()==GLFW.GLFW_KEY_F10) { DslEditorKeyHandler.closeEditor(); return true; }
        if (event.hasControlDown() && event.key()==GLFW.GLFW_KEY_Z) {
            if (event.hasShiftDown()) redo(); else undo();
            return true;
        }
        if (event.hasControlDown() && event.key()==GLFW.GLFW_KEY_Y) { redo(); return true; }
        if (editingKey!=null || editingBranch!=null || editingData!=null) {
            if (event.key()==GLFW.GLFW_KEY_ENTER || event.key()==GLFW.GLFW_KEY_KP_ENTER) {
                if (editingKey!=null) commitValue(editingKey,editingValue);
                else if (editingBranch!=null) commitBranch(editingBranch,editingValue);
                else commitData(editingData,editingValue);
            } else if (event.key()==GLFW.GLFW_KEY_ESCAPE) {
                editingKey=null;editingBranch=null;editingData=null;error="";
            } else this.editTextKey(event);
            return true;
        }
        if (event.key()==GLFW.GLFW_KEY_ESCAPE) {
            this.clearSelection();
            panelHidden=true;
            return true;
        }
        if (selectedId!=null && boxes.containsKey(selectedId) && !isReadOnly()) {
            int step=event.hasShiftDown()?10:1;
            DslUiContext.ResolvedVisual b=boxes.get(selectedId);
            float dx=0,dy=0;
            switch(event.key()) {
                case GLFW.GLFW_KEY_LEFT -> dx=-step;
                case GLFW.GLFW_KEY_RIGHT -> dx=step;
                case GLFW.GLFW_KEY_UP -> dy=-step;
                case GLFW.GLFW_KEY_DOWN -> dy=step;
                default -> {return true;}
            }
            beginDrag(Drag.MOVE,b.x(),b.y());
            editX=canMoveX() ? Math.round(b.x()+dx) : b.x();
            editY=canMoveY() ? Math.round(b.y()+dy) : b.y();
            dragDirty=(dx != 0 && canMoveX()) || (dy != 0 && canMoveY());
            finishDrag();
        }
        return true; // No keys leak into underlying widgets/containers while editing.
    }

    @Override
    public boolean charTyped(Screen s,CharacterEvent event) {
        if ((editingKey!=null || editingData!=null || editingBranch!=null)
                && event.isAllowedChatCharacter() && editingValue.length()<180) {
            String inserted=event.codepointAsString();
            editingCursor=clamp(editingCursor,0,editingValue.length());
            editingValue=editingValue.substring(0,editingCursor)+inserted+editingValue.substring(editingCursor);
            editingCursor+=inserted.length();error="";
        }
        return true;
    }

    private void editTextKey(KeyEvent event) {
        editingCursor=clamp(editingCursor,0,editingValue.length());
        switch(event.key()) {
            case GLFW.GLFW_KEY_LEFT -> {
                if (editingCursor>0) editingCursor=editingValue.offsetByCodePoints(editingCursor,-1);
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (editingCursor<editingValue.length()) editingCursor=editingValue.offsetByCodePoints(editingCursor,1);
            }
            case GLFW.GLFW_KEY_HOME -> editingCursor=0;
            case GLFW.GLFW_KEY_END -> editingCursor=editingValue.length();
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (editingCursor>0) {
                    int previous=editingValue.offsetByCodePoints(editingCursor,-1);
                    editingValue=editingValue.substring(0,previous)+editingValue.substring(editingCursor);
                    editingCursor=previous;
                }
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (editingCursor<editingValue.length()) {
                    int next=editingValue.offsetByCodePoints(editingCursor,1);
                    editingValue=editingValue.substring(0,editingCursor)+editingValue.substring(next);
                }
            }
            default -> { }
        }
        error="";
    }

    private void applyChecked(UiDocumentManager manager, Asset asset, String original, String updated) {
        applyChecked(manager,Map.of(asset,original),Map.of(asset,updated));
    }

    private void applyChecked(UiDocumentManager manager,Map<Asset,String> originals,Map<Asset,String> updates) {
        Map<Asset,String> changedOriginals=new LinkedHashMap<>();
        Map<Asset,String> changedUpdates=new LinkedHashMap<>();
        Map<Asset,Long> previousErrors=new HashMap<>();
        for (Map.Entry<Asset,String> entry : updates.entrySet()) {
            String original=originals.get(entry.getKey());
            if (original==null || original.equals(entry.getValue())) continue;
            changedOriginals.put(entry.getKey(),original);
            changedUpdates.put(entry.getKey(),entry.getValue());
            previousErrors.put(entry.getKey(),errorCount(manager,entry.getKey()));
        }
        if (changedUpdates.isEmpty()) return;
        manager.updateSourceOverrides(changedUpdates);
        boolean rejected=changedUpdates.keySet().stream().anyMatch(asset ->
            errorCount(manager,asset)>previousErrors.getOrDefault(asset,0L));
        if (rejected) {
            manager.updateSourceOverrides(changedOriginals);
            throw new IllegalArgumentException("El DSL rechazó el valor; se mantuvo el anterior");
        }
        ((ScreenAccesor)this.screen).javaReforged$rebuildWidgets();
        if (!applyingHistory) {
            undo.push(new HistoryEntry(Map.copyOf(changedOriginals),Map.copyOf(changedUpdates)));
            while (undo.size() > 100) undo.removeLast();
            redo.clear();
        }
    }

    private static long errorCount(UiDocumentManager manager,Asset asset) {
        return manager.diagnostics(asset).stream().filter(d ->
            d.severity()==net.jr.client.ui.dsl.UiDiagnostic.Severity.ERROR).count();
    }

    private void undo() {
        if (undo.isEmpty()) { status="No hay cambios para deshacer"; return; }
        HistoryEntry change=undo.pop();
        if (applyHistory(change.before())) {
            redo.push(change); status="Deshecho: "+change.before().size()+" archivo(s)";
        } else undo.push(change);
    }

    private void redo() {
        if (redo.isEmpty()) { status="No hay cambios para rehacer"; return; }
        HistoryEntry change=redo.pop();
        if (applyHistory(change.after())) {
            undo.push(change); status="Rehecho: "+change.after().size()+" archivo(s)";
        } else redo.push(change);
    }

    private boolean applyHistory(Map<Asset,String> target) {
        UiDocumentManager manager=UiDocumentManager.getInstance();
        Map<Asset,String> current=new LinkedHashMap<>();
        target.keySet().forEach(asset -> current.put(asset,manager.sourceText(asset).orElse("")));
        try {
            applyingHistory=true;
            applyChecked(manager,current,target);
            target.forEach(DslEditorOverlay::updateTouched);
            editingKey=null; editingData=null; editingBranch=null; error="";
            return true;
        } catch (RuntimeException exception) {
            error="No se pudo aplicar el historial: "+exception.getMessage();
            return false;
        } finally { applyingHistory=false; }
    }

    private static void updateTouched(Asset asset,String source) {
        if (source.equals(memoryBaseline.get(asset))) touched.remove(asset);
        else touched.add(asset);
    }

    private Asset selectedFile() {
        if (selectedId != null) {
            DslEditorTree.Node node = tree.node(selectedId);
            if (node != null && touched.contains(node.source().source())) return node.source().source();
        }
        if (compiled != null && touched.contains(compiled.source())) return compiled.source();
        return touched.isEmpty() ? null : touched.iterator().next();
    }

    private void save() {
        Asset asset = selectedFile();
        if (asset == null) {status="No hay cambios para guardar";return;}
        UiDocumentManager manager=UiDocumentManager.getInstance();
        try {
            String source=manager.sourceText(asset).orElseThrow();
            DslEditorStorage.save(asset,source);
            memoryBaseline.put(asset,source);
            touched.remove(asset);
            error="";status="Guardado en paquete: "+asset;
        } catch (IOException | RuntimeException exception) {
            error="No se pudo guardar: "+exception.getMessage();
        }
    }

    private void restore() {
        Asset asset = selectedFile();
        if (asset == null || !memoryBaseline.containsKey(asset)) {
            status="Este archivo no tiene cambios sin guardar";return;
        }
        try {
            UiDocumentManager manager=UiDocumentManager.getInstance();
            String original=manager.sourceText(asset).orElseThrow();
            applyChecked(manager,asset,original,memoryBaseline.get(asset));
            updateTouched(asset,memoryBaseline.get(asset));
            error="";status="Restaurado en memoria: "+asset;
        } catch (RuntimeException exception) {error="No se pudo restaurar: "+exception.getMessage();}
    }

    private enum Drag {
        NONE(false,false,false,false), MOVE(false,false,false,false), PAN(false,false,false,false),
        TL(true,true,false,false), TR(false,true,true,false), BL(true,false,false,true), BR(false,false,true,true),
        TOP(false,true,false,false), BOTTOM(false,false,false,true), LEFT(true,false,false,false), RIGHT(false,false,true,false);
        final boolean left,top,right,bottom;
        Drag(boolean l,boolean t,boolean r,boolean b) {left=l;top=t;right=r;bottom=b;}
    }

    private record HistoryEntry(Map<Asset,String> before,Map<Asset,String> after) { }
    private record ValueRow(String label,String value,
                            UiCompiledDocument.SourceLocation location,int depth) { }
    private record DataValueRow(String label,String value,
                                UiCompiledDocument.SourceLocation location,int depth,
                                UiDataUsageTracker.Usage usage) { }
    private enum InspectorTab { MAIN, COMPILED, SOURCE }
    private record InspectorLayout(
        int structureHeader, int structureTop, int structureHeight,
        int propertiesHeader, int propertiesTop, int propertiesHeight,
        int dataHeader, int dataTop, int dataHeight
    ) { }
}
