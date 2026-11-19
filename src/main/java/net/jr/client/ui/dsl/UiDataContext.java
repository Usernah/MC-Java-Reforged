package net.jr.client.ui.dsl;

import net.jr.api.client.local.Client;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.render.font.FontHolder;
import net.jr.api.client.render.font.FontRegister;
import net.jr.api.client.ui.Viewport;
import net.jr.client.runtime.options.OptionsStateResolver;
import net.jr.client.runtime.state.CraftingMode;
import net.jr.client.ui.presentation.UiPresentation;
import net.jr.client.ui.presentation.UiResolution;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class UiDataContext implements UiCompiledDocument.DataContext {
    private final Map<List<String>, Supplier<?>> values = new LinkedHashMap<>();
    private ElementResolver elementResolver;

    public static UiDataContext withGlobals() {
        UiDataContext context = new UiDataContext();
        context.exposeAsset("global.font", () -> UiPresentation.currentFont().location());
        context.exposeBoolean(
            "global.full_hd",
            () -> UiPresentation.framebufferResolution() == UiResolution.FULL_HD
        );
        context.exposeBoolean("is.small", UiPresentation::isSmall);
        context.exposeBoolean(
            "is.crafting.classic",
            () -> OptionsStateResolver.current().craftingMode().get() == CraftingMode.CLASSIC
        );
        context.exposeBoolean("is.client.primary", () -> Client.current().isPrimary());
        context.exposeNumber("client.id", Client::slotId);
        context.exposeNumber("viewport.width", () -> Viewport.current().uiWidth());
        context.exposeNumber("viewport.height", () -> Viewport.current().uiHeight());
        context.exposeNumber("viewport.full_width", () -> Viewport.window().guiWidth());
        context.exposeNumber("viewport.full_height", () -> Viewport.window().guiHeight());
        exposeRegisteredFonts(context);
        return context;
    }

    private static void exposeRegisteredFonts(UiDataContext context) {
        for (FontHolder font : FontRegister.allRegisteredFonts()) {
            String dslPath = "fonts."
                + font.modId()
                + "."
                + font.path().replace('/', '.');
            context.exposeAsset(dslPath, font::location);
        }
    }

    public void exposeNumber(String path, Supplier<? extends Number> value) {
        this.expose(path, value);
    }

    public void exposeBoolean(String path, Supplier<Boolean> value) {
        this.expose(path, value);
    }

    public void exposeText(String path, Supplier<String> value) {
        this.expose(path, value);
    }

    public void exposeAsset(String path, Supplier<Asset> value) {
        this.expose(path, value);
    }

    public void remove(String path) {
        this.values.remove(parsePath(path));
    }

    @Override
    public Object resolve(List<String> path) {
        Supplier<?> supplier = this.values.get(List.copyOf(path));
        if (supplier == null) {
            throw new IllegalArgumentException("UI data is not exposed: ${" + String.join(".", path) + "}");
        }
        return supplier.get();
    }

    @Override
    public boolean contains(List<String> path) {
        return this.values.containsKey(List.copyOf(path));
    }

    @Override
    public Object resolveElement(String id, String property) {
        if (this.elementResolver == null) {
            throw new IllegalStateException("UI element references are not bound to a DSL context");
        }
        return this.elementResolver.resolve(id, property);
    }

    void bindElementResolver(ElementResolver resolver) {
        this.elementResolver = Objects.requireNonNull(resolver, "resolver");
    }

    private void expose(String path, Supplier<?> value) {
        this.values.put(parsePath(path), Objects.requireNonNull(value, "value"));
    }

    private static List<String> parsePath(String path) {
        Objects.requireNonNull(path, "path");
        List<String> parts = Arrays.stream(path.trim().split("\\."))
            .map(String::trim)
            .toList();
        if (parts.isEmpty() || parts.stream().anyMatch(String::isEmpty) || parts.getFirst().equals("this")) {
            throw new IllegalArgumentException("Invalid exposed UI data path: " + path);
        }
        return parts;
    }

    @FunctionalInterface
    interface ElementResolver {
        Object resolve(String id, String property);
    }
}
