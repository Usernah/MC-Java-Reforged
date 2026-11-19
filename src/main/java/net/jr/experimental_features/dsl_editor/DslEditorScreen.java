package net.jr.experimental_features.dsl_editor;

import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.FramebufferScreen;
import net.jr.api.client.ui.UiFile;
import net.jr.api.client.ui.UiFileType;
import net.jr.api.client.ui.UiRegister;
import net.jr.client.ui.dsl.UiCompiledDocument;
import net.jr.client.ui.dsl.UiDocumentManager;
import net.jr.client.ui.presentation.UiPresentation;
import net.jr.client.ui.presentation.UiResolution;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class DslEditorScreen extends Screen implements FramebufferScreen {
    private static final int TOOLBAR_HEIGHT = 34;
    private static final int CANVAS_WIDTH = 1280;
    private static final int CANVAS_HEIGHT = 720;
    private static final int PANEL_WIDTH = 270;
    private static final int HANDLE_SIZE = 14;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int CYAN = 0xFF35D7FF;
    private static final int PURPLE = 0xFFCA73FF;
    private static final int PANEL = 0xED15171B;
    private static final int PANEL_ALT = 0xED20242A;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int MUTED = 0xFF9A9FA8;

    private final Screen previous;
    private final DslEditorPreview preview = new DslEditorPreview();
    private final List<UiFile> files = UiRegister.allRegisteredFiles().stream()
        .sorted(Comparator.comparing(UiFile::type).thenComparing(UiFile::toString))
        .toList();
    private UiFile file;
    private UiFileType category = UiFileType.SCREEN;
    private DslEditorPreview.ElementBox selected;
    private DragMode dragMode = DragMode.NONE;
    private float dragStartX;
    private float dragStartY;
    private float startX;
    private float startY;
    private float startWidth;
    private float startHeight;
    private float editX;
    private float editY;
    private float editWidth;
    private float editHeight;
    private float zoom = 1.0F;
    private float panX;
    private float panY;
    private boolean toolbarVisible = true;
    private boolean small;
    private int previewMode;
    private int activeViewport;
    private int pickerScroll;
    private String status = "Selecciona un archivo DSL";

    public DslEditorScreen(Screen previous) {
        super(Component.literal("Editor visual DSL (experimental)"));
        this.previous = previous;
    }

    @Override
    public void extractRenderState(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        graphics.fill(0, 0, this.width, this.height, 0xFF0B0D10);
        if (this.file == null) {
            this.renderPicker(graphics, mouseX, mouseY);
            return;
        }

        UiCompiledDocument document = UiDocumentManager.getInstance()
            .getCompiled(this.selectedAsset())
            .orElse(null);
        if (document != null) {
            List<PreviewViewport> viewports = this.previewViewports();
            this.activeViewport = Math.min(this.activeViewport, viewports.size() - 1);
            this.preview.begin(document);
            graphics.pose().pushMatrix();
            graphics.pose().translate(this.panX, this.panY);
            graphics.pose().scale(this.zoom, this.zoom);
            boolean smallPresentation = this.usesSmallPresentation();
            for (int index = 0; index < viewports.size(); index++) {
                PreviewViewport viewport = viewports.get(index);
                graphics.pose().pushMatrix();
                graphics.pose().translate(viewport.canvasX(), viewport.canvasY());
                graphics.pose().scale(viewport.scale(), viewport.scale());
                this.preview.render(
                    graphics,
                    document,
                    smallPresentation,
                    CANVAS_WIDTH,
                    CANVAS_HEIGHT,
                    CANVAS_WIDTH,
                    CANVAS_HEIGHT,
                    index == 0
                );
                graphics.pose().popMatrix();
            }
            graphics.pose().popMatrix();
            this.rebindSelection();
            this.renderPreviewGuides(graphics);
            if (this.selected != null) {
                this.renderSelection(graphics);
            }
        } else {
            this.text(graphics, "No se pudo compilar " + this.selectedAsset(), 18, 58, 0xFFFF7777);
        }

        if (this.toolbarVisible) {
            this.renderToolbar(graphics, mouseX, mouseY);
        } else {
            this.button(graphics, 8, 8, 34, 20, "☰", mouseX, mouseY);
        }
        if (this.selected != null) {
            this.renderProperties(graphics, mouseX, mouseY);
        }
        this.text(graphics, this.status, 10, this.height - 16, MUTED);
    }

    private void renderPicker(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, this.width, this.height, 0xF20F1115);
        this.text(graphics, "Editor visual DSL", 28, 20, WHITE);
        this.text(graphics, "El documento abierto será la copia viva usada por el juego.", 28, 38, MUTED);
        int x = 28;
        x = this.categoryButton(graphics, x, 62, UiFileType.SCREEN, "Pantallas", mouseX, mouseY);
        x = this.categoryButton(graphics, x, 62, UiFileType.CONTAINER, "Contenedores", mouseX, mouseY);
        this.categoryButton(graphics, x, 62, UiFileType.OVERLAY, "Overlays", mouseX, mouseY);

        List<UiFile> visible = this.visibleFiles();
        int y = 104;
        int first = Math.min(this.pickerScroll, Math.max(0, visible.size() - 1));
        for (int index = first; index < visible.size() && y < this.height - 44; index++) {
            UiFile entry = visible.get(index);
            boolean hover = inside(mouseX, mouseY, 28, y, Math.min(700, this.width - 56), 32);
            graphics.fill(28, y, Math.min(this.width - 28, 728), y + 30,
                hover ? 0xFF39424C : 0xFF242A31);
            this.text(graphics, entry.path(), 40, y + 10, TEXT);
            y += 36;
        }
        this.text(graphics, "F10 cierra el editor · rueda para recorrer", 28, this.height - 24, MUTED);
    }

    private int categoryButton(
        GuiGraphicsExtractor graphics,
        int x,
        int y,
        UiFileType type,
        String label,
        int mouseX,
        int mouseY
    ) {
        int width = 130;
        graphics.fill(x, y, x + width, y + 26,
            this.category == type ? 0xFF187A99 : inside(mouseX, mouseY, x, y, width, 26)
                ? 0xFF3A424C : 0xFF252A31);
        this.text(graphics, label, x + 10, y + 9, TEXT);
        return x + width + 8;
    }

    private void renderToolbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(0, 0, this.width, TOOLBAR_HEIGHT, PANEL);
        int x = 6;
        x = this.button(graphics, x, 6, 68, 22, "Archivos", mouseX, mouseY) + 5;
        x = this.button(graphics, x, 6, 56, 22, "Guardar", mouseX, mouseY) + 5;
        x = this.button(graphics, x, 6, 82, 22, "Restablecer", mouseX, mouseY) + 5;
        String variant = this.small ? "Small" : this.previewMode != 0 ? "Small*" : "Normal";
        x = this.button(graphics, x, 6, 70, 22, variant, mouseX, mouseY) + 5;
        String mode = switch (this.previewMode) {
            case 1 -> "Vertical";
            case 2 -> "Horizontal";
            case 3 -> "Four";
            default -> "Single";
        };
        x = this.button(graphics, x, 6, 82, 22, mode, mouseX, mouseY) + 5;
        this.button(graphics, x, 6, 32, 22, "—", mouseX, mouseY);
        String dirty = UiDocumentManager.getInstance().hasSourceOverride(this.editableAsset()) ? " *" : "";
        this.text(graphics, this.file.path() + dirty, x + 44, 13, TEXT);
    }

    private void renderProperties(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int left = this.width - PANEL_WIDTH;
        int top = this.toolbarVisible ? TOOLBAR_HEIGHT : 0;
        graphics.fill(left, top, this.width, this.height, PANEL);
        graphics.fill(left, top, this.width, top + 32, PANEL_ALT);
        this.text(graphics, "Propiedades", left + 12, top + 12, WHITE);
        this.button(graphics, this.width - 28, top + 6, 20, 20, "×", mouseX, mouseY);
        int y = top + 48;
        int accent = this.selected.blueprint() ? PURPLE : CYAN;
        this.text(graphics, this.selected.type(), left + 12, y, accent);
        y += 18;
        this.text(graphics, this.selected.id(), left + 12, y, MUTED);
        y += 26;
        y = this.property(graphics, left, y, "x", this.editedRawX(), accent);
        y = this.property(graphics, left, y, "y", this.editedRawY(), accent);
        y = this.property(graphics, left, y, "w", this.editWidth, accent);
        y = this.property(graphics, left, y, "h", this.editHeight, accent);
        y += 10;
        this.text(graphics, "Origen", left + 12, y, MUTED);
        y += 15;
        this.text(graphics, this.selected.source().toString(), left + 12, y, accent);
        y += 15;
        this.text(graphics, "línea " + this.selected.line(), left + 12, y, MUTED);
        if (!this.selected.condition().isBlank()) {
            y += 26;
            this.text(graphics, "Condición", left + 12, y, MUTED);
            y += 15;
            this.text(graphics, this.selected.condition(), left + 12, y, 0xFFFFCE72);
        }
        if (this.selected.text()) {
            y += 30;
            this.text(graphics, "Los textos se editan desde propiedades;", left + 12, y, MUTED);
            this.text(graphics, "no se mueven con el marco.", left + 12, y + 14, MUTED);
        }
    }

    private int property(GuiGraphicsExtractor graphics, int left, int y, String name, float value, int accent) {
        graphics.fill(left + 10, y, this.width - 10, y + 24, PANEL_ALT);
        this.text(graphics, name, left + 18, y + 8, accent);
        this.text(graphics, format(value), left + 70, y + 8, TEXT);
        return y + 29;
    }

    private void renderSelection(GuiGraphicsExtractor graphics) {
        PreviewViewport viewport = this.activePreviewViewport();
        float x = this.screenX(this.editX, viewport);
        float y = this.screenY(this.editY, viewport);
        float width = this.editWidth * viewport.scale() * this.zoom;
        float height = this.editHeight * viewport.scale() * this.zoom;
        int left = Math.round(x);
        int top = Math.round(y);
        int right = Math.round(x + width);
        int bottom = Math.round(y + height);
        this.inwardBorder(graphics, left, top, right, bottom);
        if (this.selected.text()) {
            return;
        }
        this.handle(graphics, left, top);
        this.handle(graphics, right, top);
        this.handle(graphics, left, bottom);
        this.handle(graphics, right, bottom);
        this.handle(graphics, (left + right) / 2, top);
        this.handle(graphics, (left + right) / 2, bottom);
        this.handle(graphics, left, (top + bottom) / 2);
        this.handle(graphics, right, (top + bottom) / 2);
    }

    private void inwardBorder(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom) {
        graphics.fill(left, top, right, top + 4, WHITE);
        graphics.fill(left, bottom - 4, right, bottom, WHITE);
        graphics.fill(left, top + 4, left + 4, bottom - 4, WHITE);
        graphics.fill(right - 4, top + 4, right, bottom - 4, WHITE);
        graphics.fill(left + 2, top + 2, right - 2, top + 4, CYAN);
        graphics.fill(left + 2, bottom - 4, right - 2, bottom - 2, CYAN);
        graphics.fill(left + 2, top + 4, left + 4, bottom - 4, CYAN);
        graphics.fill(right - 4, top + 4, right - 2, bottom - 4, CYAN);
    }

    private void handle(GuiGraphicsExtractor graphics, int centerX, int centerY) {
        int half = HANDLE_SIZE / 2;
        graphics.fill(centerX - half, centerY - half, centerX + half, centerY + half, WHITE);
        graphics.fill(centerX - half + 2, centerY - half + 2,
            centerX + half - 2, centerY + half - 2, CYAN);
    }

    private void renderPreviewGuides(GuiGraphicsExtractor graphics) {
        int color = 0xAA40BBDD;
        List<PreviewViewport> viewports = this.previewViewports();
        for (int index = 0; index < viewports.size(); index++) {
            PreviewViewport viewport = viewports.get(index);
            int left = Math.round(this.panX + viewport.cellX() * this.zoom);
            int top = Math.round(this.panY + viewport.cellY() * this.zoom);
            int right = Math.round(this.panX + (viewport.cellX() + viewport.cellWidth()) * this.zoom);
            int bottom = Math.round(this.panY + (viewport.cellY() + viewport.cellHeight()) * this.zoom);
            int border = index == this.activeViewport ? 3 : 1;
            graphics.fill(left, top, right, top + border, color);
            graphics.fill(left, bottom - border, right, bottom, color);
            graphics.fill(left, top, left + border, bottom, color);
            graphics.fill(right - border, top, right, bottom, color);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (this.file == null) {
            return this.pickerClick(mouseX, mouseY, event.button());
        }
        if (!this.toolbarVisible && inside(mouseX, mouseY, 8, 8, 34, 20)) {
            this.toolbarVisible = true;
            return true;
        }
        if (this.toolbarVisible && mouseY < TOOLBAR_HEIGHT) {
            return this.toolbarClick(mouseX, mouseY);
        }
        if (this.selected != null && mouseX >= this.width - PANEL_WIDTH) {
            int top = this.toolbarVisible ? TOOLBAR_HEIGHT : 0;
            if (inside(mouseX, mouseY, this.width - 28, top + 6, 20, 20)) {
                this.selected = null;
            }
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            this.dragMode = DragMode.PAN;
            this.dragStartX = (float)mouseX;
            this.dragStartY = (float)mouseY;
            return true;
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (this.selected != null && !this.selected.text()) {
            DragMode handle = this.hitHandle(mouseX, mouseY);
            if (handle != DragMode.NONE) {
                this.beginDrag(handle, mouseX, mouseY);
                return true;
            }
        }
        int viewportIndex = this.viewportAt(mouseX, mouseY);
        if (viewportIndex < 0) {
            this.selected = null;
            return true;
        }
        this.activeViewport = viewportIndex;
        PreviewViewport viewport = this.activePreviewViewport();
        double canvasX = this.canvasX(mouseX, viewport);
        double canvasY = this.canvasY(mouseY, viewport);
        List<DslEditorPreview.ElementBox> boxes = this.preview.boxes();
        for (int index = boxes.size() - 1; index >= 0; index--) {
            DslEditorPreview.ElementBox box = boxes.get(index);
            if (box.contains(canvasX, canvasY)) {
                this.select(box);
                if (!box.text()) {
                    this.beginDrag(DragMode.MOVE, mouseX, mouseY);
                }
                return true;
            }
        }
        this.selected = null;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.dragMode == DragMode.NONE) {
            return false;
        }
        if (this.dragMode == DragMode.PAN) {
            this.panX += (float)event.x() - this.dragStartX;
            this.panY += (float)event.y() - this.dragStartY;
            this.dragStartX = (float)event.x();
            this.dragStartY = (float)event.y();
            return true;
        }
        float presentationScale = this.zoom * this.activePreviewViewport().scale();
        float dx = ((float)event.x() - this.dragStartX) / presentationScale;
        float dy = ((float)event.y() - this.dragStartY) / presentationScale;
        this.applyDrag(dx, dy);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.dragMode == DragMode.NONE) {
            return false;
        }
        DragMode finished = this.dragMode;
        this.dragMode = DragMode.NONE;
        if (finished != DragMode.PAN && this.selected != null) {
            this.commitBounds();
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (this.file == null) {
            int count = this.visibleFiles().size();
            this.pickerScroll = Math.max(0, Math.min(Math.max(0, count - 1),
                this.pickerScroll + (vertical < 0 ? 1 : -1)));
            return true;
        }
        float old = this.zoom;
        this.zoom = Math.max(0.25F, Math.min(4.0F, this.zoom * (vertical > 0 ? 1.1F : 0.9F)));
        this.panX = (float)mouseX - ((float)mouseX - this.panX) * this.zoom / old;
        this.panY = (float)mouseY - ((float)mouseY - this.panY) * this.zoom / old;
        this.status = "Zoom " + Math.round(this.zoom * 100.0F) + "%";
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            if (this.file != null) {
                this.file = null;
                this.selected = null;
            } else {
                this.onClose();
            }
            return true;
        }
        if (this.selected != null && !this.selected.text()) {
            float step = event.hasShiftDown() ? 10.0F : 1.0F;
            boolean moved = switch (event.key()) {
                case GLFW.GLFW_KEY_LEFT -> { this.editX -= step; yield true; }
                case GLFW.GLFW_KEY_RIGHT -> { this.editX += step; yield true; }
                case GLFW.GLFW_KEY_UP -> { this.editY -= step; yield true; }
                case GLFW.GLFW_KEY_DOWN -> { this.editY += step; yield true; }
                default -> false;
            };
            if (moved) {
                this.commitBounds();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.previous);
    }

    @Override
    public void removed() {
        this.preview.close();
        super.removed();
    }

    private boolean pickerClick(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        int x = 28;
        for (UiFileType type : List.of(UiFileType.SCREEN, UiFileType.CONTAINER, UiFileType.OVERLAY)) {
            if (inside(mouseX, mouseY, x, 62, 130, 26)) {
                this.category = type;
                this.pickerScroll = 0;
                return true;
            }
            x += 138;
        }
        int row = (int)((mouseY - 104) / 36);
        List<UiFile> visible = this.visibleFiles();
        int index = this.pickerScroll + row;
        if (mouseX >= 28 && mouseX < Math.min(728, this.width - 28)
            && row >= 0 && index >= 0 && index < visible.size()) {
            this.open(visible.get(index));
            return true;
        }
        return false;
    }

    private boolean toolbarClick(double mouseX, double mouseY) {
        int x = 6;
        if (inside(mouseX, mouseY, x, 6, 68, 22)) {
            this.file = null;
            this.selected = null;
            return true;
        }
        x += 73;
        if (inside(mouseX, mouseY, x, 6, 56, 22)) {
            this.save();
            return true;
        }
        x += 61;
        if (inside(mouseX, mouseY, x, 6, 82, 22)) {
            Asset resetAsset = this.editableAsset();
            UiDocumentManager.getInstance().resetSourceOverride(resetAsset);
            this.selected = null;
            this.status = "Archivo restablecido desde el resource pack";
            return true;
        }
        x += 87;
        if (inside(mouseX, mouseY, x, 6, 70, 22)) {
            this.small = !this.small;
            this.selected = null;
            this.status = this.small ? "Variante small" : "Variante normal";
            return true;
        }
        x += 75;
        if (inside(mouseX, mouseY, x, 6, 82, 22)) {
            this.previewMode = (this.previewMode + 1) % 4;
            this.activeViewport = 0;
            this.selected = null;
            this.status = this.previewMode == 0
                ? "Preview single: DSL normal"
                : "Preview de viewports: presentación small";
            return true;
        }
        x += 87;
        if (inside(mouseX, mouseY, x, 6, 32, 22)) {
            this.toolbarVisible = false;
            return true;
        }
        return true;
    }

    private void open(UiFile file) {
        this.file = file;
        this.small = false;
        this.selected = null;
        this.zoom = 1.0F;
        this.panX = 0.0F;
        this.panY = 0.0F;
        this.activeViewport = 0;
        this.status = "Editando " + file;
    }

    private void save() {
        Asset asset = this.editableAsset();
        String source = UiDocumentManager.getInstance().sourceText(asset).orElse(null);
        if (source == null) {
            this.status = "No hay fuente cargada para guardar";
            return;
        }
        try {
            Path path = DslEditorStorage.save(asset, source);
            this.status = "Guardado: " + path;
        } catch (IOException exception) {
            this.status = "No se pudo guardar: " + exception.getMessage();
        }
    }

    private void commitBounds() {
        if (this.selected == null) {
            return;
        }
        UiDocumentManager manager = UiDocumentManager.getInstance();
        Asset target = this.selectedAsset();
        String source = manager.sourceText(target).orElse(null);
        if (source == null) {
            this.status = "No se encontró la fuente de " + target;
            return;
        }
        List<DslSourceEditor.ValueEdit> changes = new ArrayList<>();
        try {
            this.addValueEdit(changes, target, this.selected.xLocation(),
                this.editedRawX(), this.selected.rawX(), "x");
            this.addValueEdit(changes, target, this.selected.yLocation(),
                this.editedRawY(), this.selected.rawY(), "y");
            this.addValueEdit(changes, target, this.selected.widthLocation(),
                this.editWidth, this.selected.width(), "w");
            this.addValueEdit(changes, target, this.selected.heightLocation(),
                this.editHeight, this.selected.height(), "h");
            if (changes.isEmpty()) {
                return;
            }
            this.checkSharedProperties(changes, target);
            String updated = DslSourceEditor.replaceValues(source, changes);
            manager.updateSourceOverride(target, updated);
            this.status = "Instancia actualizada en memoria: " + target;
        } catch (IllegalArgumentException exception) {
            this.status = "No se editó: " + exception.getMessage();
            this.select(this.selected);
        }
    }

    private void addValueEdit(
        List<DslSourceEditor.ValueEdit> changes,
        Asset target,
        UiCompiledDocument.SourceLocation location,
        float value,
        float previous,
        String property
    ) {
        if (Math.abs(value - previous) < 0.0001F) {
            return;
        }
        if (!Float.isFinite(value) || !location.editable() || !location.source().equals(target)) {
            throw new IllegalArgumentException(
                "'" + property + "' no tiene un argumento numérico editable en esta instancia"
            );
        }
        changes.add(new DslSourceEditor.ValueEdit(
            location.line(), location.column(), DslSourceEditor.number(value)
        ));
    }

    private void checkSharedProperties(List<DslSourceEditor.ValueEdit> changes, Asset target) {
        List<UiCompiledDocument.SourceLocation> properties = List.of(
            this.selected.xLocation(), this.selected.yLocation(),
            this.selected.widthLocation(), this.selected.heightLocation()
        );
        for (DslSourceEditor.ValueEdit change : changes) {
            long references = properties.stream()
                .filter(location -> location.source().equals(target)
                    && location.line() == change.line()
                    && location.column() == change.column())
                .count();
            if (references > 1) {
                throw new IllegalArgumentException(
                    "el argumento controla varias propiedades; editarlas por separado cambiaría otras medidas"
                );
            }
        }
    }

    private void beginDrag(DragMode mode, double mouseX, double mouseY) {
        this.dragMode = mode;
        this.dragStartX = (float)mouseX;
        this.dragStartY = (float)mouseY;
        this.startX = this.editX;
        this.startY = this.editY;
        this.startWidth = this.editWidth;
        this.startHeight = this.editHeight;
    }

    private void applyDrag(float dx, float dy) {
        float right = this.startX + this.startWidth;
        float bottom = this.startY + this.startHeight;
        this.editX = this.dragMode.left ? Math.min(right - 1.0F, this.startX + dx) : this.startX;
        this.editY = this.dragMode.top ? Math.min(bottom - 1.0F, this.startY + dy) : this.startY;
        float newRight = this.dragMode.right ? Math.max(this.editX + 1.0F, right + dx) : right;
        float newBottom = this.dragMode.bottom ? Math.max(this.editY + 1.0F, bottom + dy) : bottom;
        if (this.dragMode == DragMode.MOVE) {
            this.editX = this.startX + dx;
            this.editY = this.startY + dy;
            this.editWidth = this.startWidth;
            this.editHeight = this.startHeight;
        } else {
            this.editWidth = newRight - this.editX;
            this.editHeight = newBottom - this.editY;
        }
    }

    private DragMode hitHandle(double mouseX, double mouseY) {
        PreviewViewport viewport = this.activePreviewViewport();
        int left = Math.round(this.screenX(this.editX, viewport));
        int top = Math.round(this.screenY(this.editY, viewport));
        int right = Math.round(this.screenX(this.editX + this.editWidth, viewport));
        int bottom = Math.round(this.screenY(this.editY + this.editHeight, viewport));
        int centerX = (left + right) / 2;
        int centerY = (top + bottom) / 2;
        if (handleContains(mouseX, mouseY, left, top)) return DragMode.TOP_LEFT;
        if (handleContains(mouseX, mouseY, right, top)) return DragMode.TOP_RIGHT;
        if (handleContains(mouseX, mouseY, left, bottom)) return DragMode.BOTTOM_LEFT;
        if (handleContains(mouseX, mouseY, right, bottom)) return DragMode.BOTTOM_RIGHT;
        if (handleContains(mouseX, mouseY, centerX, top)) return DragMode.TOP;
        if (handleContains(mouseX, mouseY, centerX, bottom)) return DragMode.BOTTOM;
        if (handleContains(mouseX, mouseY, left, centerY)) return DragMode.LEFT;
        if (handleContains(mouseX, mouseY, right, centerY)) return DragMode.RIGHT;
        return DragMode.NONE;
    }

    private void select(DslEditorPreview.ElementBox box) {
        this.selected = box;
        this.editX = box.x();
        this.editY = box.y();
        this.editWidth = box.width();
        this.editHeight = box.height();
    }

    private float editedRawX() {
        return this.selected == null ? this.editX
            : this.selected.rawX() + (this.editX - this.selected.x());
    }

    private float editedRawY() {
        return this.selected == null ? this.editY
            : this.selected.rawY() + (this.editY - this.selected.y());
    }

    private void rebindSelection() {
        if (this.selected == null || this.dragMode != DragMode.NONE) {
            return;
        }
        for (DslEditorPreview.ElementBox box : this.preview.boxes()) {
            if (box.id().equals(this.selected.id())
                && box.type().equals(this.selected.type())) {
                this.select(box);
                return;
            }
        }
    }

    private Asset selectedAsset() {
        UiDocumentManager manager = UiDocumentManager.getInstance();
        boolean smallPresentation = this.usesSmallPresentation();
        UiResolution resolution = UiPresentation.resolution();
        String current = resolution == UiResolution.HD ? UiPresentation.HD_SUFFIX : "";
        String alternate = resolution == UiResolution.HD ? "" : UiPresentation.HD_SUFFIX;
        List<String> suffixes = smallPresentation
            ? List.of(
                current + UiPresentation.SMALL_SUFFIX,
                alternate + UiPresentation.SMALL_SUFFIX,
                current,
                alternate
            )
            : List.of(current, alternate);
        for (String suffix : suffixes) {
            Asset candidate = Asset.NamespaceAndPatch(
                this.file.modId(), this.file.type().resourcePath(this.file.path() + suffix)
            );
            if (manager.getCompiled(candidate).isPresent()) {
                return candidate;
            }
        }
        return this.file.asset();
    }

    private Asset editableAsset() {
        // Editing a blueprint instance always changes the file that invokes it,
        // never the registered style where the shared blueprint is declared.
        return this.selectedAsset();
    }

    private List<UiFile> visibleFiles() {
        List<UiFile> result = new ArrayList<>();
        for (UiFile file : this.files) {
            if (file.type() == this.category) {
                result.add(file);
            }
        }
        return result;
    }

    private boolean usesSmallPresentation() {
        return this.small || this.previewMode != 0;
    }

    private float screenX(float canvasX, PreviewViewport viewport) {
        return this.panX + (viewport.canvasX() + canvasX * viewport.scale()) * this.zoom;
    }

    private float screenY(float canvasY, PreviewViewport viewport) {
        return this.panY + (viewport.canvasY() + canvasY * viewport.scale()) * this.zoom;
    }

    private double canvasX(double screenX, PreviewViewport viewport) {
        double layoutX = (screenX - this.panX) / this.zoom;
        return (layoutX - viewport.canvasX()) / viewport.scale();
    }

    private double canvasY(double screenY, PreviewViewport viewport) {
        double layoutY = (screenY - this.panY) / this.zoom;
        return (layoutY - viewport.canvasY()) / viewport.scale();
    }

    private int viewportAt(double screenX, double screenY) {
        double layoutX = (screenX - this.panX) / this.zoom;
        double layoutY = (screenY - this.panY) / this.zoom;
        List<PreviewViewport> viewports = this.previewViewports();
        for (int index = viewports.size() - 1; index >= 0; index--) {
            PreviewViewport viewport = viewports.get(index);
            if (layoutX >= viewport.cellX()
                && layoutX < viewport.cellX() + viewport.cellWidth()
                && layoutY >= viewport.cellY()
                && layoutY < viewport.cellY() + viewport.cellHeight()) {
                return index;
            }
        }
        return -1;
    }

    private PreviewViewport activePreviewViewport() {
        List<PreviewViewport> viewports = this.previewViewports();
        return viewports.get(Math.min(this.activeViewport, viewports.size() - 1));
    }

    private List<PreviewViewport> previewViewports() {
        return switch (this.previewMode) {
            case 1 -> List.of(
                PreviewViewport.fit(0, 0, this.width / 2.0F, this.height),
                PreviewViewport.fit(this.width / 2.0F, 0, this.width / 2.0F, this.height)
            );
            case 2 -> List.of(
                PreviewViewport.fit(0, 0, this.width, this.height / 2.0F),
                PreviewViewport.fit(0, this.height / 2.0F, this.width, this.height / 2.0F)
            );
            case 3 -> List.of(
                PreviewViewport.fit(0, 0, this.width / 2.0F, this.height / 2.0F),
                PreviewViewport.fit(this.width / 2.0F, 0, this.width / 2.0F, this.height / 2.0F),
                PreviewViewport.fit(0, this.height / 2.0F, this.width / 2.0F, this.height / 2.0F),
                PreviewViewport.fit(
                    this.width / 2.0F, this.height / 2.0F,
                    this.width / 2.0F, this.height / 2.0F
                )
            );
            default -> List.of(PreviewViewport.fit(0, 0, this.width, this.height));
        };
    }

    private int button(
        GuiGraphicsExtractor graphics,
        int x,
        int y,
        int width,
        int height,
        String label,
        int mouseX,
        int mouseY
    ) {
        graphics.fill(x, y, x + width, y + height,
            inside(mouseX, mouseY, x, y, width, height) ? 0xFF3D4651 : 0xFF292F36);
        this.text(graphics, label, x + 7, y + 8, TEXT);
        return x + width;
    }

    private void text(GuiGraphicsExtractor graphics, String value, int x, int y, int color) {
        String clipped = value.length() > 42 ? value.substring(0, 39) + "..." : value;
        graphics.text(this.font, Component.literal(clipped), x, y, color, false);
    }

    private static boolean handleContains(double x, double y, int centerX, int centerY) {
        int half = HANDLE_SIZE / 2 + 2;
        return x >= centerX - half && x <= centerX + half
            && y >= centerY - half && y <= centerY + half;
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private static String format(float value) {
        int rounded = Math.round(value);
        return Math.abs(value - rounded) < 0.001F
            ? Integer.toString(rounded)
            : String.format(Locale.ROOT, "%.2f", value);
    }

    private enum DragMode {
        NONE(false, false, false, false),
        MOVE(false, false, false, false),
        PAN(false, false, false, false),
        LEFT(true, false, false, false),
        RIGHT(false, true, false, false),
        TOP(false, false, true, false),
        BOTTOM(false, false, false, true),
        TOP_LEFT(true, false, true, false),
        TOP_RIGHT(false, true, true, false),
        BOTTOM_LEFT(true, false, false, true),
        BOTTOM_RIGHT(false, true, false, true);

        private final boolean left;
        private final boolean right;
        private final boolean top;
        private final boolean bottom;

        DragMode(boolean left, boolean right, boolean top, boolean bottom) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }
    }

    private record PreviewViewport(
        float cellX,
        float cellY,
        float cellWidth,
        float cellHeight,
        float canvasX,
        float canvasY,
        float scale
    ) {
        private static PreviewViewport fit(float x, float y, float width, float height) {
            float scale = Math.min(width / CANVAS_WIDTH, height / CANVAS_HEIGHT);
            float canvasX = x + (width - CANVAS_WIDTH * scale) / 2.0F;
            float canvasY = y + (height - CANVAS_HEIGHT * scale) / 2.0F;
            return new PreviewViewport(x, y, width, height, canvasX, canvasY, scale);
        }
    }
}
