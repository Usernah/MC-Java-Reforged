package net.jr.client.ui.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.UiDataFile;
import net.jr.api.client.ui.UiDataId;
import net.jr.api.client.ui.UiDataValues;
import net.jr.api.client.ui.UiFileType;
import net.jr.client.ui.dsl.UiCompiledDocument.DataContext;
import org.junit.jupiter.api.Test;

final class UiDataCompilerTest {
    @Test
    void compilesTypedValuesAssetsAndRegisteredStyles() throws Exception {
        Asset styleAsset = Asset.NamespaceAndPatch("test", "ui/styles/theme.style");
        UiParser.Result style = UiParser.parse(new StringReader("""
            @@def palette = {
                enabled: true
                label: "Imported"
                color: #ff336699
            }
            """), styleAsset, UiFileType.STYLE);

        Asset dataAsset = Asset.NamespaceAndPatch("test", "ui/data/settings.data");
        UiParser.Result data = UiParser.parse(new StringReader("""
            @@import asset.local("textures/icon.png") as icon

            "enabled": ::theme.palette.enabled
            "label": ::theme.palette.label
            "color": ::theme.palette.color
            "lives": 3
            "scale": 1.5
            "icon": @icon
            "modes": ["vanilla", "battle"]
            "layout": { width: 320, height: 180 }
            """), dataAsset, UiFileType.DATA);

        UiDataFile file = new UiDataFile(new UiDataId("test", "settings"));
        UiDataCompilationResult result = new UiDataCompiler(
            Map.of(dataAsset, data.document(), styleAsset, style.document()),
            Map.of("theme", style.document())
        ).compile(data.document());
        UiDataValues values = UiDataValues.loaded(file, result.data().resolve(path -> {
            throw new IllegalArgumentException("Unexpected runtime value: " + path);
        }));

        assertTrue(style.diagnostics().isEmpty(), style.diagnostics().toString());
        assertTrue(data.diagnostics().isEmpty(), data.diagnostics().toString());
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
        assertTrue(values.isLoaded());
        assertTrue(values.bool("enabled"));
        assertEquals("Imported", values.string("label"));
        assertEquals(0xff336699, values.color("color"));
        assertEquals(3, values.integer("lives"));
        assertEquals(1.5F, values.decimal("scale"));
        assertEquals("test:textures/icon.png", values.asset("icon").toString());
        assertEquals(2, values.list("modes").size());
        assertEquals(320.0, values.object("layout").get("width"));
    }

    @Test
    void rejectsLayoutAndBlueprintDeclarations() throws Exception {
        Asset asset = Asset.NamespaceAndPatch("test", "ui/data/invalid.data");
        UiParser.Result parsed = UiParser.parse(new StringReader("""
            @@blueprint invalid() {
            }

            screen {
            }
            """), asset, UiFileType.DATA);
        UiDataCompilationResult result = new UiDataCompiler(
            Map.of(asset, parsed.document()), Map.of()
        ).compile(parsed.document());

        assertFalse(result.diagnostics().isEmpty());
        assertFalse(result.data().isUsable());
    }

    @Test
    void resolvesRuntimeValuesEveryTimeDataIsRead() throws Exception {
        Asset styleAsset = Asset.NamespaceAndPatch("test", "ui/styles/text.style");
        UiParser.Result style = UiParser.parse(new StringReader("""
            @@def font = if (${is.small})
                ${fonts.test.minecraft_min}
                else ${fonts.test.minecraft_sevenv4}
            """), styleAsset, UiFileType.STYLE);
        Asset dataAsset = Asset.NamespaceAndPatch("test", "ui/data/text.data");
        UiParser.Result data = UiParser.parse(new StringReader("""
            "font": ::text.font
            "small": ${is.small}
            "small_size": if (${is.small}) 1 else 2
            """), dataAsset, UiFileType.DATA);
        UiDataFile file = new UiDataFile(new UiDataId("test", "text"));
        UiDataCompilationResult result = new UiDataCompiler(
            Map.of(dataAsset, data.document(), styleAsset, style.document()),
            Map.of("text", style.document())
        ).compile(data.document());

        AtomicBoolean smallPresentation = new AtomicBoolean(false);
        UiDataValues values = UiDataValues.live(
            file,
            () -> true,
            () -> result.data().resolve(context(smallPresentation.get()))
        );

        assertTrue(style.diagnostics().isEmpty(), style.diagnostics().toString());
        assertTrue(data.diagnostics().isEmpty(), data.diagnostics().toString());
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
        assertEquals(styleAsset, result.data().location("font").source());
        assertTrue(result.data().location("small_size").branchesEditable());
        assertFalse(values.bool("small"));
        assertEquals("test:font/minecraft_sevenv4", values.asset("font").toString());
        smallPresentation.set(true);
        assertTrue(values.bool("small"));
        assertEquals("test:font/minecraft_min", values.asset("font").toString());
    }

    @Test
    void resolvesNestedPresentationAndCraftingModeStyleValues() throws Exception {
        Asset styleAsset = Asset.NamespaceAndPatch("test", "ui/styles/inventory_preview.style");
        UiParser.Result style = UiParser.parse(new StringReader("""
            @@def left = if (${is.crafting.classic})
                if (not ${is.small}) 140 else 58
                else if (not ${is.small}) 140 else 105
            """), styleAsset, UiFileType.STYLE);
        Asset dataAsset = Asset.NamespaceAndPatch("test", "ui/data/inventory.data");
        UiParser.Result data = UiParser.parse(new StringReader("""
            "left": ::inventory_preview.left
            """), dataAsset, UiFileType.DATA);
        UiDataCompilationResult result = new UiDataCompiler(
            Map.of(dataAsset, data.document(), styleAsset, style.document()),
            Map.of("inventory_preview", style.document())
        ).compile(data.document());

        AtomicBoolean small = new AtomicBoolean(false);
        AtomicBoolean classic = new AtomicBoolean(false);
        UiDataValues values = UiDataValues.live(
            new UiDataFile(new UiDataId("test", "inventory")),
            () -> true,
            () -> result.data().resolve(path -> switch (String.join(".", path)) {
                case "is.small" -> small.get();
                case "is.crafting.classic" -> classic.get();
                default -> throw new IllegalArgumentException("Unexpected runtime value: " + path);
            })
        );

        assertTrue(style.diagnostics().isEmpty(), style.diagnostics().toString());
        assertTrue(data.diagnostics().isEmpty(), data.diagnostics().toString());
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
        assertEquals(140, values.integer("left"));
        small.set(true);
        assertEquals(105, values.integer("left"));
        classic.set(true);
        assertEquals(58, values.integer("left"));
        small.set(false);
        assertEquals(140, values.integer("left"));
    }

    private static DataContext context(boolean small) {
        return path -> switch (String.join(".", path)) {
            case "is.small" -> small;
            case "fonts.test.minecraft_min" ->
                Asset.NamespaceAndPatch("test", "font/minecraft_min");
            case "fonts.test.minecraft_sevenv4" ->
                Asset.NamespaceAndPatch("test", "font/minecraft_sevenv4");
            default -> throw new IllegalArgumentException("Unexpected runtime value: " + path);
        };
    }
}
