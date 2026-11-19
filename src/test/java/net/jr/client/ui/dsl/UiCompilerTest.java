package net.jr.client.ui.dsl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.Map;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.UiFileType;
import org.junit.jupiter.api.Test;

final class UiCompilerTest {
    @Test
    void compilesRegisteredVisualAndLayoutElements() throws Exception {
        String source = """
            screen {
                background {
                    bounds {
                        x: 0
                        y: 0
                        w: 320
                        h: 180
                    }

                    canvas {
                        x: 0
                        y: 0
                        w: 320
                        h: 180

                        content {
                            rectangle {
                                x: 0
                                y: 0
                                w: 320
                                h: 180
                                color: #ff202020
                            }

                            text {
                                literal: "Test"
                                x: 8
                                y: 8
                            }
                        }
                    }
                }
            }
            """;
        Asset asset = Asset.NamespaceAndPatch("test", "ui/screens/compiler_test.screen");
        UiParser.Result parsed = UiParser.parse(
            new StringReader(source), asset, UiFileType.SCREEN
        );
        UiCompilationResult compiled = new UiCompiler(
            Map.of(asset, parsed.document()), Map.of()
        ).compile(parsed.document());

        assertTrue(parsed.diagnostics().isEmpty(), parsed.diagnostics().toString());
        assertTrue(compiled.diagnostics().isEmpty(), compiled.diagnostics().toString());
        assertTrue(compiled.document().isUsable());
        assertTrue(compiled.document().propertyLocation("$bounds", "x").editable());
        assertTrue(compiled.document().propertyLocation("$bounds", "y").editable());
        assertTrue(compiled.document().propertyLocation("$bounds", "w").editable());
        assertTrue(compiled.document().propertyLocation("$bounds", "h").editable());
    }

    @Test
    void expandsBlueprintsBeforeCompilingElements() throws Exception {
        String source = """
            @@blueprint panel(x) {
                rectangle {
                    x: &x
                    y: 4
                    w: 24
                    h: 12
                    color: #ff203040
                }
            }

            screen {
                background {
                    @panel(8)
                }
            }
            """;
        Asset asset = Asset.NamespaceAndPatch("test", "ui/screens/blueprint_test.screen");
        UiParser.Result parsed = UiParser.parse(
            new StringReader(source), asset, UiFileType.SCREEN
        );
        UiCompilationResult compiled = new UiCompiler(
            Map.of(asset, parsed.document()), Map.of()
        ).compile(parsed.document());

        assertTrue(parsed.diagnostics().isEmpty(), parsed.diagnostics().toString());
        assertTrue(compiled.diagnostics().isEmpty(), compiled.diagnostics().toString());
        assertTrue(compiled.document().isUsable());
    }
}
