package net.jr.client.render;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.jr.api.client.render.Draw;
import net.jr.api.client.render.glyph.GlyphDefinition;
import net.jr.api.client.render.glyph.Glyphs;
import net.jr.api.client.resource.Asset;
import net.jr.client.render.glyph.GlyphRegistryStore;
import net.jr.client.render.glyph.GlyphTextureBounds;
import net.jr.client.render.glyph.ResolvedGlyph;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

final class InlineTextRenderer {
    private static final String MARKER_OPEN = "${";
    private static final String MARKER_CLOSE = "}";
    private static final String UNKNOWN_MARKER_FALLBACK = "?";

    private InlineTextRenderer() {
    }

    static boolean containsMarker(Component component) {
        return component.getString().contains(MARKER_OPEN);
    }

    static Layout layout(Component component, TextStyle style) {
        return layout(
                component,
                style,
                Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY,
                0,
                0.0F
        );
    }

    static Layout layout(Component component, TextStyle style, float maxWidth) {
        return layout(
                component,
                style,
                maxWidth,
                Float.POSITIVE_INFINITY,
                0,
                0.0F
        );
    }

    static Layout layout(
            Component component,
            TextStyle style,
            float maxWidth,
            float maxHeight,
            int maxLines,
            float lineSpacing
    ) {
        float safeMaxWidth = Float.isFinite(maxWidth)
                ? Math.max(0.0F, maxWidth)
                : Float.POSITIVE_INFINITY;
        float safeMaxHeight = Float.isFinite(maxHeight)
                ? Math.max(0.0F, maxHeight)
                : Float.POSITIVE_INFINITY;
        float safeLineSpacing = Math.max(0.0F, lineSpacing);
        List<Atom> atoms = splitOversizedWords(
                parse(component.getString()),
                style,
                safeMaxWidth
        );
        float baseLineHeight = text(style, "").lineHeight();

        List<Line> lines = new ArrayList<>();
        LineBuilder current = new LineBuilder(baseLineHeight);
        TextAtom pendingWhitespace = null;

        for (Atom atom : atoms) {
            if (atom instanceof LineBreakAtom) {
                lines.add(current.finish());
                current = new LineBuilder(baseLineHeight);
                pendingWhitespace = null;
                continue;
            }

            if (atom instanceof TextAtom textAtom && textAtom.whitespace()) {
                if (!current.isEmpty()) {
                    pendingWhitespace = pendingWhitespace == null
                            ? textAtom
                            : new TextAtom(
                            pendingWhitespace.text() + textAtom.text(),
                            true
                    );
                }
                continue;
            }

            Run run = run(atom, style);
            Run whitespace = pendingWhitespace == null
                    ? null
                    : run(pendingWhitespace, style);

            if (!current.isEmpty()
                    && current.widthWith(whitespace, run) > safeMaxWidth) {
                lines.add(current.finish());
                current = new LineBuilder(baseLineHeight);
                whitespace = null;
            }

            if (whitespace != null) {
                current.add(whitespace);
            }
            current.add(run);
            pendingWhitespace = null;
        }

        lines.add(current.finish());
        return new Layout(
                List.copyOf(lines),
                safeMaxHeight,
                Math.max(0, maxLines),
                safeLineSpacing
        );
    }

    private static List<Atom> splitOversizedWords(
            List<Atom> atoms,
            TextStyle style,
            float maxWidth
    ) {
        if (!Float.isFinite(maxWidth)) {
            return atoms;
        }

        List<Atom> result = new ArrayList<>();
        for (Atom atom : atoms) {
            if (!(atom instanceof TextAtom textAtom)
                    || textAtom.whitespace()
                    || run(textAtom, style).width() <= maxWidth) {
                result.add(atom);
                continue;
            }
            splitOversizedWord(result, textAtom.text(), style, maxWidth);
        }
        return result;
    }

    private static void splitOversizedWord(
            List<Atom> result,
            String value,
            TextStyle style,
            float maxWidth
    ) {
        int start = 0;
        while (start < value.length()) {
            int cursor = start;
            int bestEnd = start;

            while (cursor < value.length()) {
                int next = cursor + Character.charCount(value.codePointAt(cursor));
                float width = run(
                        new TextAtom(value.substring(start, next), false),
                        style
                ).width();
                if (width > maxWidth && bestEnd > start) {
                    break;
                }
                bestEnd = next;
                cursor = next;
                if (width > maxWidth) {
                    break;
                }
            }

            result.add(new TextAtom(value.substring(start, bestEnd), false));
            start = bestEnd;
        }
    }

    private static List<Atom> parse(String source) {
        List<Atom> atoms = new ArrayList<>();
        int cursor = 0;

        while (cursor < source.length()) {
            int markerStart = source.indexOf(MARKER_OPEN, cursor);
            if (markerStart < 0) {
                appendText(atoms, source.substring(cursor));
                break;
            }

            appendText(atoms, source.substring(cursor, markerStart));
            int markerEnd = source.indexOf(
                    MARKER_CLOSE,
                    markerStart + MARKER_OPEN.length()
            );
            if (markerEnd < 0) {
                appendText(atoms, source.substring(markerStart));
                break;
            }

            String marker = source.substring(
                    markerStart + MARKER_OPEN.length(),
                    markerEnd
            );
            Float spacing = Glyphs.horizontalSpaceWidth(marker);
            if (spacing != null) {
                atoms.add(new SpacingAtom(spacing));
            } else {
                GlyphDefinition glyph = GlyphRegistryStore.resolve(marker);
                if (glyph == null) {
                    appendText(atoms, UNKNOWN_MARKER_FALLBACK);
                } else {
                    atoms.add(new GlyphAtom(glyph));
                }
            }
            cursor = markerEnd + MARKER_CLOSE.length();
        }

        if (atoms.isEmpty()) {
            atoms.add(new TextAtom("", false));
        }
        return atoms;
    }

    private static void appendText(List<Atom> atoms, String value) {
        int cursor = 0;
        while (cursor < value.length()) {
            char character = value.charAt(cursor);
            if (character == '\r' || character == '\n') {
                if (character == '\r'
                        && cursor + 1 < value.length()
                        && value.charAt(cursor + 1) == '\n') {
                    cursor++;
                }
                atoms.add(LineBreakAtom.INSTANCE);
                cursor++;
                continue;
            }

            boolean whitespace = Character.isWhitespace(character);
            int end = cursor + 1;
            while (end < value.length()) {
                char next = value.charAt(end);
                if (next == '\r'
                        || next == '\n'
                        || Character.isWhitespace(next) != whitespace) {
                    break;
                }
                end++;
            }
            atoms.add(new TextAtom(value.substring(cursor, end), whitespace));
            cursor = end;
        }
    }

    private static Run run(Atom atom, TextStyle style) {
        if (atom instanceof TextAtom textAtom) {
            Draw.TextBuilder builder = text(style, textAtom.text());
            return new TextRun(builder, builder.width(), builder.lineHeight());
        }
        if (atom instanceof GlyphAtom glyphAtom) {
            return new GlyphRun(ResolvedGlyph.from(glyphAtom.definition()));
        }
        if (atom instanceof SpacingAtom spacingAtom) {
            return new SpacingRun(spacingAtom.width());
        }
        throw new IllegalArgumentException("Unsupported inline text atom " + atom);
    }

    private static Draw.TextBuilder text(TextStyle style, String value) {
        Draw.TextBuilder builder = Draw.text(
                        Component.literal(value).setStyle(style.componentStyle())
                )
                .scale(style.scale())
                .color(style.color())
                .shadow(style.shadow())
                .legacyShadow(style.legacyShadow());
        if (style.shadowColor() != null) {
            builder.shadowColor(style.shadowColor());
        }
        if (style.outlineColor() != null) {
            builder.outline(style.outlineColor());
        }
        if (style.font() != null) {
            builder.font(style.font());
        }
        return builder;
    }

    record TextStyle(
            float scale,
            int color,
            boolean shadow,
            boolean legacyShadow,
            @Nullable Integer shadowColor,
            @Nullable Integer outlineColor,
            @Nullable Asset font,
            Style componentStyle
    ) {
    }

    static final class Layout {
        private final List<Line> lines;
        private final List<Float> lineTops;
        private final float width;
        private final float height;
        private final float lineHeight;

        private Layout(
                List<Line> lines,
                float maxHeight,
                int maxLines,
                float lineSpacing
        ) {
            List<Line> visibleLines = new ArrayList<>();
            List<Float> positions = new ArrayList<>();
            float minY = 0.0F;
            float maxY = 0.0F;
            float measuredWidth = 0.0F;
            float measuredLineHeight = 0.0F;

            for (Line line : lines) {
                if (maxLines > 0 && visibleLines.size() >= maxLines) {
                    break;
                }

                float top = 0.0F;
                if (!visibleLines.isEmpty()) {
                    Line previous = visibleLines.get(visibleLines.size() - 1);
                    float previousTop = positions.get(positions.size() - 1);
                    float normalTop = previousTop
                            + previous.baseLineHeight()
                            + lineSpacing;
                    float previousBottom = previousTop
                            + previous.baseLineHeight() / 2.0F
                            + previous.maxY();
                    float currentTopOffset = line.baseLineHeight() / 2.0F
                            + line.minY();
                    float collisionTop = previousBottom
                            + lineSpacing
                            - currentTopOffset;
                    top = Math.max(normalTop, collisionTop);
                }

                float actualTop = top
                        + line.baseLineHeight() / 2.0F
                        + line.minY();
                float actualBottom = top
                        + line.baseLineHeight() / 2.0F
                        + line.maxY();
                float nextMinY = Math.min(minY, actualTop);
                float nextMaxY = Math.max(maxY, actualBottom);
                if (nextMaxY - nextMinY > maxHeight) {
                    break;
                }

                visibleLines.add(line);
                positions.add(top);
                minY = nextMinY;
                maxY = nextMaxY;
                measuredWidth = Math.max(measuredWidth, line.width());
                measuredLineHeight = Math.max(measuredLineHeight, line.height());
            }

            this.lines = List.copyOf(visibleLines);
            this.lineTops = List.copyOf(positions);
            this.width = measuredWidth;
            this.height = maxY - minY;
            this.lineHeight = measuredLineHeight;
        }

        float width() {
            return this.width;
        }

        float height() {
            return this.height;
        }

        float lineHeight() {
            return this.lineHeight;
        }

        int lineCount() {
            return this.lines.size();
        }

        void draw(
                GuiGraphicsExtractor graphics,
                float anchorX,
                float y,
                UiRenderEngine.TextAlign align
        ) {
            for (int i = 0; i < this.lines.size(); i++) {
                Line line = this.lines.get(i);
                float lineTop = y + this.lineTops.get(i);
                line.draw(graphics, anchorX, lineTop, align);
            }
        }
    }

    private interface Atom {
    }

    private record TextAtom(String text, boolean whitespace) implements Atom {
    }

    private record GlyphAtom(GlyphDefinition definition) implements Atom {
    }

    private record SpacingAtom(float width) implements Atom {
    }

    private enum LineBreakAtom implements Atom {
        INSTANCE
    }

    private interface Run {
        float advance();

        float left();

        float right();

        float top();

        float bottom();

        default float width() {
            return Math.max(this.advance(), this.right())
                    - Math.min(0.0F, this.left());
        }

        void draw(GuiGraphicsExtractor graphics, float x, float centerY);
    }

    private record TextRun(
            Draw.TextBuilder builder,
            float advance,
            float height
    ) implements Run {
        @Override
        public float left() {
            return 0.0F;
        }

        @Override
        public float right() {
            return this.advance;
        }

        @Override
        public float top() {
            return -this.height / 2.0F;
        }

        @Override
        public float bottom() {
            return this.height / 2.0F;
        }

        @Override
        public void draw(GuiGraphicsExtractor graphics, float x, float centerY) {
            this.builder.position(
                    Math.round(x),
                    Math.round(centerY - this.height / 2.0F)
            ).draw(graphics);
        }
    }

    private record GlyphRun(
            ResolvedGlyph glyph
    ) implements Run {
        @Override
        public float advance() {
            return this.glyph.advance();
        }

        @Override
        public float left() {
            return Math.min(
                    0.0F,
                    this.glyph.sideAdvance() + this.glyph.offsetX()
            );
        }

        @Override
        public float right() {
            return Math.max(
                    this.advance(),
                    this.glyph.sideAdvance()
                            + this.glyph.offsetX()
                            + this.glyph.visibleWidth()
            );
        }

        @Override
        public float top() {
            return this.glyph.offsetY() - this.glyph.visibleHeight() / 2.0F;
        }

        @Override
        public float bottom() {
            return this.glyph.offsetY() + this.glyph.visibleHeight() / 2.0F;
        }

        @Override
        public void draw(GuiGraphicsExtractor graphics, float x, float centerY) {
            GlyphTextureBounds bounds = this.glyph.bounds();
            Draw.image(
                            this.glyph.asset(),
                            x + this.glyph.sideAdvance() + this.glyph.offsetX()
                                    - this.glyph.visibleLeft(),
                            centerY - this.glyph.visibleHeight() / 2.0F
                                    + this.glyph.offsetY() - this.glyph.visibleTop(),
                            this.glyph.drawWidth(),
                            this.glyph.drawHeight()
                    )
                    .uvSize(bounds.sourceWidth(), bounds.sourceHeight())
                    .atlasSize(bounds.sourceWidth(), bounds.sourceHeight())
                    .draw(graphics);
        }
    }

    private record SpacingRun(float advance) implements Run {
        @Override
        public float left() {
            return 0.0F;
        }

        @Override
        public float right() {
            return this.advance;
        }

        @Override
        public float top() {
            return 0.0F;
        }

        @Override
        public float bottom() {
            return 0.0F;
        }

        @Override
        public void draw(GuiGraphicsExtractor graphics, float x, float centerY) {
        }
    }

    private record PlacedRun(Run run, float x) {
    }

    private record Line(
            List<PlacedRun> runs,
            float width,
            float height,
            float baseLineHeight,
            float advanceWidth,
            float minY,
            float maxY
    ) {
        void draw(
                GuiGraphicsExtractor graphics,
                float x,
                float y,
                UiRenderEngine.TextAlign align
        ) {
            float originX = switch (align) {
                case CENTER -> x - this.advanceWidth / 2.0F;
                case RIGHT -> x - this.advanceWidth;
                case LEFT -> x;
            };
            float centerY = y + this.baseLineHeight / 2.0F;
            for (PlacedRun placed : this.runs) {
                placed.run().draw(graphics, originX + placed.x(), centerY);
            }
        }
    }

    private static final class LineBuilder {
        private final List<PlacedRun> runs = new ArrayList<>();
        private final float baseLineHeight;
        private float cursor;
        private float minX;
        private float maxX;
        private float minY;
        private float maxY;

        private LineBuilder(float baseLineHeight) {
            this.baseLineHeight = baseLineHeight;
            this.minX = 0.0F;
            this.maxX = 0.0F;
            this.minY = -baseLineHeight / 2.0F;
            this.maxY = baseLineHeight / 2.0F;
        }

        void add(Run run) {
            this.runs.add(new PlacedRun(run, this.cursor));
            this.include(run, this.cursor);
            this.cursor += run.advance();
            this.maxX = Math.max(this.maxX, this.cursor);
        }

        boolean isEmpty() {
            return this.runs.isEmpty();
        }

        float width() {
            return this.maxX - this.minX;
        }

        float widthWith(@Nullable Run first, Run second) {
            float previewCursor = this.cursor;
            float previewMinX = this.minX;
            float previewMaxX = this.maxX;

            if (first != null) {
                previewMinX = Math.min(previewMinX, previewCursor + first.left());
                previewMaxX = Math.max(previewMaxX, previewCursor + first.right());
                previewCursor += first.advance();
                previewMaxX = Math.max(previewMaxX, previewCursor);
            }

            previewMinX = Math.min(previewMinX, previewCursor + second.left());
            previewMaxX = Math.max(previewMaxX, previewCursor + second.right());
            previewCursor += second.advance();
            previewMaxX = Math.max(previewMaxX, previewCursor);
            return previewMaxX - previewMinX;
        }

        private void include(Run run, float x) {
            this.minX = Math.min(this.minX, x + run.left());
            this.maxX = Math.max(this.maxX, x + run.right());
            this.minY = Math.min(this.minY, run.top());
            this.maxY = Math.max(this.maxY, run.bottom());
        }

        Line finish() {
            return new Line(
                    List.copyOf(this.runs),
                    this.maxX - this.minX,
                    this.maxY - this.minY,
                    this.baseLineHeight,
                    this.cursor,
                    this.minY,
                    this.maxY
            );
        }
    }
}
