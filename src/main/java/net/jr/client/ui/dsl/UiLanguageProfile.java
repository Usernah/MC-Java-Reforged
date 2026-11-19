package net.jr.client.ui.dsl;

import java.util.EnumSet;
import java.util.Set;
import net.jr.api.client.ui.UiFileType;

public final class UiLanguageProfile {
    private static final Set<UiLanguageCapability> LAYOUT_CAPABILITIES = Set.copyOf(
        EnumSet.allOf(UiLanguageCapability.class)
    );
    private static final Set<UiLanguageCapability> STYLE_CAPABILITIES = Set.of(
        UiLanguageCapability.ASSET_IMPORTS,
        UiLanguageCapability.DOCUMENT_IMPORTS,
        UiLanguageCapability.STYLE_REFERENCES,
        UiLanguageCapability.BLUEPRINT_DECLARATIONS,
        UiLanguageCapability.BLUEPRINT_USAGE,
        UiLanguageCapability.CONTENT_INSERTION,
        UiLanguageCapability.VARIABLES,
        UiLanguageCapability.EXPOSED_DATA,
        UiLanguageCapability.ELEMENT_REFERENCES,
        UiLanguageCapability.VISUAL_ELEMENTS
    );
    private static final Set<UiLanguageCapability> DATA_CAPABILITIES = Set.of(
        UiLanguageCapability.ASSET_IMPORTS,
        UiLanguageCapability.STYLE_REFERENCES,
        UiLanguageCapability.EXPOSED_DATA
    );

    private final Set<UiLanguageCapability> capabilities;

    private UiLanguageProfile(Set<UiLanguageCapability> capabilities) {
        this.capabilities = Set.copyOf(capabilities);
    }

    public static UiLanguageProfile forType(UiFileType type) {
        return new UiLanguageProfile(switch (type) {
            case STYLE -> STYLE_CAPABILITIES;
            case DATA -> DATA_CAPABILITIES;
            default -> LAYOUT_CAPABILITIES;
        });
    }

    public boolean supports(UiLanguageCapability capability) {
        return this.capabilities.contains(capability);
    }

    public void require(
        UiLanguageCapability capability,
        UiDocument document,
        UiDocument.Position position
    ) throws UiCompileException {
        if (!this.supports(capability)) {
            throw new UiCompileException(
                document.source(),
                position,
                "The " + document.type().name().toLowerCase(java.util.Locale.ROOT)
                    + " document type does not support "
                    + capability.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ')
            );
        }
    }
}
