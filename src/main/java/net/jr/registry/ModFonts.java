package net.jr.registry;

import net.jr.api.client.render.font.FontHolder;
import net.jr.api.client.render.font.FontRegister;
import net.jr.api.client.render.font.IFontRegistryProvider;

import static net.jr.Java_reforged.MODID;

public class ModFonts implements IFontRegistryProvider {
    public static final ModFonts INSTANCE = new ModFonts();
    public static final FontRegister REGISTRY = FontRegister.create(MODID);

    public static final FontHolder MINECRAFT = REGISTRY.register("minecraft");
    public static final FontHolder MINECRAFT_MIN = REGISTRY.register("minecraft_min");
    public static final FontHolder MC_FIVE = REGISTRY.register("minecraft_five");
    public static final FontHolder MC_FIVE_V3 = REGISTRY.register("minecraft_fivev3");
    public static final FontHolder MC_FIVE_BOLD = REGISTRY.register("minecraft_five_bold");
    public static final FontHolder MC_SEVEN = REGISTRY.register("minecraft_seven");
    public static final FontHolder MC_SEVEN_V4 = REGISTRY.register("minecraft_sevenv4");
    public static final FontHolder MC_TEN = REGISTRY.register("minecraft_ten");

    private ModFonts() {
    }

    @Override
    public FontRegister getFontRegister() {
        return REGISTRY;
    }
}
