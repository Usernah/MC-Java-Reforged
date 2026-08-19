package net.jr.screens.controller;

import com.google.common.collect.ImmutableList;
import net.jr.api.client.render.Draw;
import net.jr.client.input.binding.GamepadBindingRegistry;
import net.jr.client.input.binding.GamepadInputChord;
import net.jr.client.input.binding.KeyboardMouseInputBindings;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.ui.hint.glyph.ControllerGlyph;
import net.jr.client.ui.hint.glyph.ControllerGlyphTheme;
import net.jr.client.ui.hint.glyph.GamepadGlyphComponents;
import net.jr.client.ui.hint.glyph.GamepadGlyphThemeState;
import net.jr.client.ui.hint.glyph.KeyboardMouseGlyph;
import net.jr.client.ui.hint.glyph.KeyboardMouseGlyphResolver;
import net.jr.client.ui.hint.glyph.KeyboardMouseGlyphTheme;
import net.jr.client.ui.hint.render.GlyphTextureBounds;
import net.jr.client.ui.hint.render.GlyphTextureBoundsCache;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ControllerBindingsList extends ContainerObjectSelectionList<ControllerBindingsList.Entry> {
    private static final int ITEM_HEIGHT = 20;
    final ControllerBindingsScreen keyBindsScreen;
    private int maxNameWidth;

    public ControllerBindingsList(ControllerBindingsScreen keyBindsScreen, Minecraft minecraft) {
        super(
            minecraft,
            keyBindsScreen.width,
            keyBindsScreen.layout.getContentHeight(),
            keyBindsScreen.layout.getHeaderHeight(),
            ITEM_HEIGHT
        );
        this.keyBindsScreen = keyBindsScreen;
        rebuildEntries();
    }

    public void resetMappingAndUpdateButtons() {
        if (!this.keyBindsScreen.isGamepadMode()) {
            KeyMapping.resetMapping();
        }
        refreshEntries();
    }

    public void refreshEntries() {
        children().forEach(Entry::refreshEntry);
    }

    private void rebuildEntries() {
        clearEntries();
        maxNameWidth = 0;
        List<KeyMapping> keyMappings = keyBindsScreen.isGamepadMode()
            ? GamepadBindingRegistry.get().sortedGamepadKeyMappings(minecraft.options)
            : GamepadBindingRegistry.get().sortedKeyboardKeyMappings(minecraft.options);

        KeyMapping.Category currentCategory = null;
        for (KeyMapping keyMapping : keyMappings) {
            KeyMapping.Category category = keyMapping.getCategory();
            if (!category.equals(currentCategory)) {
                currentCategory = category;
                addEntry(new CategoryEntry(category.label()));
            }

            Component displayName = keyMapping.getDisplayName();
            maxNameWidth = Math.max(maxNameWidth, minecraft.font.width(displayName));
            addEntry(new KeyEntry(keyMapping, displayName));
        }
    }

    @Override
    public int getRowWidth() {
        return 340;
    }

    public class CategoryEntry extends Entry {
        final Component name;
        private final int nameWidth;

        public CategoryEntry(Component name) {
            this.name = name;
            this.nameWidth = ControllerBindingsList.this.minecraft.font.width(name);
        }

        @Override
        public void extractContent(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            boolean hovered,
            float partialTick
        ) {
            graphics.text(
                ControllerBindingsList.this.minecraft.font,
                name,
                ControllerBindingsList.this.width / 2 - nameWidth / 2,
                getY() + getHeight() - 10,
                -1,
                false
            );
        }

        @Override
        public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
            return null;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.emptyList();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return ImmutableList.of(new NarratableEntry() {
                @Override
                public NarrationPriority narrationPriority() {
                    return NarrationPriority.HOVERED;
                }

                @Override
                public void updateNarration(NarrationElementOutput narration) {
                    narration.add(NarratedElementType.TITLE, CategoryEntry.this.name);
                }
            });
        }

        @Override
        protected void refreshEntry() {
        }
    }

    public abstract static class Entry extends ContainerObjectSelectionList.Entry<ControllerBindingsList.Entry> {
        abstract void refreshEntry();
    }

    public class KeyEntry extends Entry {
        private static final Component RESET_BUTTON_TITLE = Component.translatable("controls.reset");
        private final KeyMapping key;
        private final Component name;
        private final Button changeButton;
        private final Button resetButton;
        private boolean hasCollision;

        KeyEntry(KeyMapping key, Component name) {
            this.key = key;
            this.name = name;
            this.changeButton = Button.builder(name, button -> {
                if (ControllerBindingsList.this.keyBindsScreen.isGamepadMode()) {
                    ControllerBindingsList.this.keyBindsScreen.beginGamepadSelection(key);
                } else {
                    ControllerBindingsList.this.keyBindsScreen.beginKeyboardSelection(key);
                }
                ControllerBindingsList.this.resetMappingAndUpdateButtons();
            }).bounds(0, 0, 75, 20)
                .createNarration(messageSupplier -> createNarration(messageSupplier.get()))
                .build();

            this.resetButton = Button.builder(RESET_BUTTON_TITLE, button -> {
                if (ControllerBindingsList.this.keyBindsScreen.isGamepadMode()) {
                    GamepadBindingRegistry.get().resetBindingToDefault(key);
                } else {
                    key.setToDefault();
                    key.setKey(key.getDefaultKey());
                }
                ControllerBindingsList.this.resetMappingAndUpdateButtons();
            }).bounds(0, 0, 50, 20)
                .createNarration(messageSupplier -> Component.translatable("narrator.controls.reset", name))
                .build();
            refreshEntry();
        }

        @Override
        public void extractContent(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            boolean hovered,
            float partialTick
        ) {
            int resetX = ControllerBindingsList.this.scrollBarX() - resetButton.getWidth() - 10;
            int buttonY = getY() - 2;
            resetButton.setPosition(resetX, buttonY);
            resetButton.extractRenderState(graphics, mouseX, mouseY, partialTick);

            int changeX = resetX - 5 - changeButton.getWidth();
            changeButton.setPosition(changeX, buttonY);
            changeButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
            if (shouldRenderKeyboardGlyphs()) {
                renderKeyboardGlyphs(graphics);
            } else if (shouldRenderGamepadGlyphs()) {
                renderGamepadGlyphs(graphics);
            }

            graphics.text(
                ControllerBindingsList.this.minecraft.font,
                name,
                getX(),
                getY() + getHeight() / 2 - 9 / 2,
                -1,
                false
            );
            if (hasCollision) {
                int markerX = changeButton.getX() - 6;
                graphics.fill(markerX, getY() - 1, markerX + 3, getY() + getHeight(), -65536);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return ImmutableList.of(changeButton, resetButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return ImmutableList.of(changeButton, resetButton);
        }

        @Override
        protected void refreshEntry() {
            if (ControllerBindingsList.this.keyBindsScreen.isGamepadMode()) {
                refreshGamepadEntry();
            } else {
                refreshKeyboardEntry();
            }
        }

        private void refreshKeyboardEntry() {
            changeButton.setMessage(shouldRenderKeyboardGlyphs() ? Component.empty() : key.getTranslatedKeyMessage());
            resetButton.active = !key.isDefault();
            hasCollision = false;
            MutableComponent collisionNames = Component.empty();
            if (!key.isUnbound()) {
                for (KeyMapping keyMapping : ControllerBindingsList.this.minecraft.options.keyMappings) {
                    if ((keyMapping != key && key.same(keyMapping)) || keyMapping.hasKeyModifierConflict(key)) {
                        if (hasCollision) {
                            collisionNames.append(", ");
                        }
                        hasCollision = true;
                        collisionNames.append(keyMapping.getDisplayName());
                    }
                }
            }

            if (hasCollision && !shouldRenderKeyboardGlyphs()) {
                changeButton.setMessage(
                    Component.literal("[ ")
                        .append(changeButton.getMessage().copy().withStyle(ChatFormatting.WHITE))
                        .append(" ]")
                        .withStyle(ChatFormatting.RED)
                );
                changeButton.setTooltip(Tooltip.create(
                    Component.translatable("controls.keybinds.duplicateKeybinds", collisionNames)
                ));
            } else {
                changeButton.setTooltip(null);
            }

            if (ControllerBindingsList.this.keyBindsScreen.selectedKey == key) {
                changeButton.setMessage(
                    Component.literal("> ")
                        .append(Component.translatable("controls.java_reforged.waiting_short")
                            .withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
                        .append(" <")
                        .withStyle(ChatFormatting.YELLOW)
                );
            }
        }

        private boolean shouldRenderKeyboardGlyphs() {
            return !ControllerBindingsList.this.keyBindsScreen.isGamepadMode()
                && ControllerBindingsList.this.keyBindsScreen.selectedKey != key
                && !key.isUnbound()
                && !KeyboardMouseGlyphResolver.glyphs(key).isEmpty();
        }

        private void renderKeyboardGlyphs(GuiGraphicsExtractor graphics) {
            List<KeyboardMouseGlyph> glyphs = KeyboardMouseGlyphResolver.glyphs(key);
            if (glyphs.isEmpty()) {
                return;
            }

            KeyboardMouseGlyphTheme theme = KeyboardMouseGlyphResolver.currentTheme();
            float totalWidth = keyboardGlyphsWidth(theme, glyphs);
            float currentX = changeButton.getX() + (changeButton.getWidth() - totalWidth) / 2.0F;
            float centerY = changeButton.getY() + changeButton.getHeight() / 2.0F;
            int joinerWidth = ControllerBindingsList.this.minecraft.font.width("+");

            for (int i = 0; i < glyphs.size(); i++) {
                if (i > 0) {
                    currentX += 2.0F;
                    int joinerY = Math.round(centerY - ControllerBindingsList.this.minecraft.font.lineHeight / 2.0F);
                    graphics.text(
                        ControllerBindingsList.this.minecraft.font,
                        "+",
                        Math.round(currentX),
                        joinerY,
                        -1,
                        false
                    );
                    currentX += joinerWidth + 2.0F;
                }

                KeyboardMouseGlyph glyph = glyphs.get(i);
                GlyphTextureBounds metrics = GlyphTextureBoundsCache.get(theme.texture(glyph));
                float iconHeight = theme.hintHeight(glyph);
                float iconWidth = metrics.drawWidthForHeight(iconHeight);
                Draw.image(theme.texture(glyph), currentX, centerY - iconHeight / 2.0F, iconWidth, iconHeight)
                    .uvSize(metrics.sourceWidth(), metrics.sourceHeight())
                    .atlasSize(metrics.sourceWidth(), metrics.sourceHeight())
                    .draw(graphics);
                currentX += iconWidth;
            }
        }

        private float keyboardGlyphsWidth(KeyboardMouseGlyphTheme theme, List<KeyboardMouseGlyph> glyphs) {
            float width = 0.0F;
            int joinerWidth = ControllerBindingsList.this.minecraft.font.width("+");
            for (int i = 0; i < glyphs.size(); i++) {
                if (i > 0) {
                    width += 4.0F + joinerWidth;
                }
                KeyboardMouseGlyph glyph = glyphs.get(i);
                GlyphTextureBounds metrics = GlyphTextureBoundsCache.get(theme.texture(glyph));
                width += metrics.drawWidthForHeight(theme.hintHeight(glyph));
            }
            return width;
        }

        private void refreshGamepadEntry() {
            GamepadBindingRegistry registry = GamepadBindingRegistry.get();
            GamepadInputChord binding = registry.getBinding(key);
            resetButton.active = !registry.isDefault(key);
            hasCollision = registry.hasConflict(key);

            Component message;
            if (ControllerBindingsList.this.keyBindsScreen.getSelectedControllerKey() == key) {
                message = Component.translatable("controls.java_reforged.waiting_short");
            } else if (binding == null) {
                message = Component.translatable("controls.java_reforged.unbound_short");
            } else if (shouldRenderGamepadGlyphs()) {
                message = Component.empty();
            } else {
                message = GamepadGlyphComponents.current().chord(binding);
            }

            if (hasCollision) {
                message = wrapConflict(message);
                changeButton.setTooltip(Tooltip.create(Component.translatable(
                    "controls.keybinds.duplicateKeybinds",
                    registry.describeConflictNames(key)
                )));
            } else {
                changeButton.setTooltip(null);
            }

            if (ControllerBindingsList.this.keyBindsScreen.getSelectedControllerKey() == key) {
                message = wrapSelected(message);
            }
            changeButton.setMessage(message);
        }

        private MutableComponent createNarration(Component currentMessage) {
            if (ControllerBindingsList.this.keyBindsScreen.isGamepadMode()) {
                GamepadInputChord binding = GamepadBindingRegistry.get().getBinding(key);
                return binding == null
                    ? Component.translatable("narrator.controls.unbound", name)
                    : Component.translatable(
                        "narrator.controls.bound",
                        name,
                        GamepadGlyphComponents.current().chord(binding)
                    );
            }
            return key.isUnbound()
                ? Component.translatable("narrator.controls.unbound", name)
                : Component.translatable("narrator.controls.bound", name, currentMessage);
        }

        private Component wrapConflict(Component inner) {
            return Component.empty()
                .append(Component.literal("[ ").withStyle(ChatFormatting.RED))
                .append(inner)
                .append(Component.literal(" ]").withStyle(ChatFormatting.RED));
        }

        private Component wrapSelected(Component inner) {
            return Component.empty()
                .append(Component.literal("> ").withStyle(ChatFormatting.YELLOW))
                .append(inner)
                .append(Component.literal(" <").withStyle(ChatFormatting.YELLOW));
        }

        private boolean shouldRenderGamepadGlyphs() {
            return ControllerBindingsList.this.keyBindsScreen.isGamepadMode()
                && ControllerBindingsList.this.keyBindsScreen.getSelectedControllerKey() != key
                && GamepadBindingRegistry.get().getBinding(key) != null;
        }

        private void renderGamepadGlyphs(GuiGraphicsExtractor graphics) {
            GamepadInputChord chord = GamepadBindingRegistry.get().getBinding(key);
            if (chord == null) {
                return;
            }

            ControllerGlyphTheme theme = GamepadGlyphThemeState.currentTheme();
            if (!hasCompleteGlyphSet(theme, chord)) {
                String fallback = chord.inputs().stream()
                    .map(GamepadDigitalInput::serializedName)
                    .collect(java.util.stream.Collectors.joining(" + "));
                int textX = changeButton.getX()
                    + (changeButton.getWidth() - ControllerBindingsList.this.minecraft.font.width(fallback)) / 2;
                int textY = changeButton.getY()
                    + (changeButton.getHeight() - ControllerBindingsList.this.minecraft.font.lineHeight) / 2;
                graphics.text(ControllerBindingsList.this.minecraft.font, fallback, textX, textY, -1, false);
                return;
            }

            float totalWidth = gamepadGlyphsWidth(theme, chord);
            float currentX = changeButton.getX() + (changeButton.getWidth() - totalWidth) / 2.0F;
            float centerY = changeButton.getY() + changeButton.getHeight() / 2.0F;
            int joinerWidth = ControllerBindingsList.this.minecraft.font.width("+");
            for (int i = 0; i < chord.inputs().size(); i++) {
                if (i > 0) {
                    currentX += 2.0F;
                    int joinerY = Math.round(centerY - ControllerBindingsList.this.minecraft.font.lineHeight / 2.0F);
                    graphics.text(
                        ControllerBindingsList.this.minecraft.font,
                        "+",
                        Math.round(currentX),
                        joinerY,
                        -1,
                        false
                    );
                    currentX += joinerWidth + 2.0F;
                }

                ControllerGlyph glyph = ControllerGlyph.forInput(chord.inputs().get(i));
                GlyphTextureBounds metrics = GlyphTextureBoundsCache.get(theme.texture(glyph));
                float iconHeight = theme.hintHeight(glyph);
                float iconWidth = metrics.drawWidthForHeight(iconHeight);
                Draw.image(theme.texture(glyph), currentX, centerY - iconHeight / 2.0F, iconWidth, iconHeight)
                    .uvSize(metrics.sourceWidth(), metrics.sourceHeight())
                    .atlasSize(metrics.sourceWidth(), metrics.sourceHeight())
                    .draw(graphics);
                currentX += iconWidth;
            }
        }

        private float gamepadGlyphsWidth(ControllerGlyphTheme theme, GamepadInputChord chord) {
            float width = 0.0F;
            int joinerWidth = ControllerBindingsList.this.minecraft.font.width("+");
            for (int i = 0; i < chord.inputs().size(); i++) {
                if (i > 0) {
                    width += 4.0F + joinerWidth;
                }
                ControllerGlyph glyph = ControllerGlyph.forInput(chord.inputs().get(i));
                GlyphTextureBounds metrics = GlyphTextureBoundsCache.get(theme.texture(glyph));
                width += metrics.drawWidthForHeight(theme.hintHeight(glyph));
            }
            return width;
        }

        private boolean hasCompleteGlyphSet(ControllerGlyphTheme theme, GamepadInputChord chord) {
            for (GamepadDigitalInput input : chord.inputs()) {
                ControllerGlyph glyph = ControllerGlyph.forInput(input);
                if (glyph == null || !theme.supports(glyph) || theme.texture(glyph) == null) {
                    return false;
                }
            }
            return true;
        }
    }
}
