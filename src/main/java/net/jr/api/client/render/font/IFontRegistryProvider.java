package net.jr.api.client.render.font;

import java.util.Collection;

public interface IFontRegistryProvider {
    FontRegister getFontRegister();

    default Collection<FontHolder> getRegisteredFonts() {
        return getFontRegister().entries();
    }
}
