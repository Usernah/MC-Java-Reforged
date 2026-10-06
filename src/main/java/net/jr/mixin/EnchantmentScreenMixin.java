package net.jr.mixin;


import net.jr.api.client.local.Client;
import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.Cursor;
import net.jr.client.components.widgets.Button;
import net.jr.client.ui.container.actions.ContainerSlotFocusController;
import net.jr.mixin.accessors.ScreenAccesor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.EnchantmentNames;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin {

    @Unique
    private static final Asset[] ENABLED_LEVEL_SPRITES = new Asset[]{
            Asset.MC("textures/gui/sprites/container/enchanting_table/level_1.png"),
            Asset.MC("textures/gui/sprites/container/enchanting_table/level_2.png"),
            Asset.MC("textures/gui/sprites/container/enchanting_table/level_3.png")
    };
    @Unique
    private static final Asset[] DISABLED_LEVEL_SPRITES = new Asset[]{
            Asset.MC("textures/gui/sprites/container/enchanting_table/level_1_disabled.png"),
            Asset.MC("textures/gui/sprites/container/enchanting_table/level_2_disabled.png"),
            Asset.MC("textures/gui/sprites/container/enchanting_table/level_3_disabled.png")
    };

    @Unique private static final Asset ENCHANTMENT_SLOT_DISABLED_SPRITE = Asset.MC("textures/gui/sprites/container/enchanting_table/enchantment_slot_disabled.png");
    @Unique private static final Asset ENCHANTMENT_SLOT_SPRITE = Asset.MC("textures/gui/sprites/container/enchanting_table/enchantment_slot.png");
    @Unique private static final Asset ENCHANTMENT_SLOT_HIGHLIGHTED_SPRITE = Asset.MC("textures/gui/sprites/container/enchanting_table/enchantment_slot_highlighted.png");

    @Unique private static final int LEVEL_SIZE = 16;
    @Unique private static final int ENCHANTMENT_SLOT_WIDHT = 108;
    @Unique private static final int ENCHANTMENT_SLOT_HEIGTH = 19;

    EnchantmentScreen screen = (EnchantmentScreen) (Object) this;
    @Unique
    private void javaReforged$createOfferList() {
        int xo = (screen.width - screen.getImageWidth()) / 2;
        int yo = (screen.height - screen.getImageHeight()) / 2;
        EnchantmentNames.getInstance().initSeed(screen.getMenu().getEnchantmentSeed());
        for (int i = 0; i < 3; i++) {
            final int index = i;
            int leftPos = xo + 60;
            int leftPosText = 20;
            Button enchantmentButton = new Button.Builder(leftPos, yo + 14 + ENCHANTMENT_SLOT_HEIGTH * index,ENCHANTMENT_SLOT_WIDHT,ENCHANTMENT_SLOT_HEIGTH)
                    .onPress(button -> {
                        if (screen.getMenu().clickMenuButton(Client.current().player(), index)) {
                            screen.getMinecraft().gameMode.handleInventoryButtonClick(screen.getMenu().containerId, index);
                            ContainerSlotFocusController.focusSlot(screen, screen.getMenu().getSlot(0));
                        }
                    })
                    .withRenderer((gui, button, mouseX, mouseY, partialTick) -> {
                        var menu = screen.getMenu();
                        var player = Client.current().player();
                        int cost = menu.costs[index];
                        boolean active = cost != 0
                                && !(((menu.getGoldCount() < index + 1 || player.experienceLevel < cost)
                                && !player.getAbilities().instabuild)
                                || menu.enchantClue[index] == -1);

                        if (button.isActive() != active) {
                            button.setActive(active);
                        }

                        if (cost == 0) {
                            Draw.image(ENCHANTMENT_SLOT_DISABLED_SPRITE, button.getX(), button.getY(), button.getWidth(), button.getHeight()).draw(gui);
                            return;
                        }

                        String costText = cost + "";
                        int textWidth = 86 - screen.getFont().width(costText);
                        FormattedText message = EnchantmentNames.getInstance().getRandomName(screen.getFont(), textWidth);
                        FormattedCharSequence drawableText =
                                screen.getFont().split(message, textWidth).getFirst();
                        int costColor;

                        if (!active) {
                            Draw.image(ENCHANTMENT_SLOT_DISABLED_SPRITE, button.getX(), button.getY(), button.getWidth(), button.getHeight()).draw(gui);
                            Draw.image(DISABLED_LEVEL_SPRITES[index], button.getX() + 1, button.getY() + 1, LEVEL_SIZE, LEVEL_SIZE).draw(gui);
                            Draw.text(drawableText, button.getX() + leftPosText, button.getY() + 2).color(0xFF342F25).shadow(false).draw(gui);
                            costColor = 0xFF407F10;
                        } else {
                            if (button.isHoveredOrFocused()) {
                                Draw.image(ENCHANTMENT_SLOT_HIGHLIGHTED_SPRITE, button.getX(), button.getY(), button.getWidth(), button.getHeight()).draw(gui);
                                Draw.text(drawableText, button.getX() + leftPosText, button.getY() + 2).color(0xFFFFFF80).shadow(false).draw(gui);
                            } else {
                                Draw.image(ENCHANTMENT_SLOT_SPRITE, button.getX(), button.getY(), button.getWidth(), button.getHeight()).draw(gui);
                                Draw.text(drawableText, button.getX() + leftPosText, button.getY() + 2).color(0xFF685E4A).shadow(false).draw(gui);
                            }
                            Draw.image(ENABLED_LEVEL_SPRITES[index], button.getX() + 1, button.getY() + 1, LEVEL_SIZE, LEVEL_SIZE).draw(gui);
                            costColor = 0xFF80FF20;
                        }

                        Draw.text(Component.literal(costText), button.getX() + leftPosText + 86 - screen.getFont().width(costText), button.getY() + 2 + 7).color(costColor).shadow(false).draw(gui);
                    })
                    .build();

            ((ScreenAccesor) (Object) screen)
                    .javaReforged$addRenderableWidget(enchantmentButton);

        }
    }

    @Inject(method = "init",at = @At(value = "TAIL"))
    protected void javaReforged$init(CallbackInfo ci) {
        this.javaReforged$createOfferList();
    }

    @Inject(method = "extractBackground",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/EnchantmentNames;initSeed(J)V"),cancellable = true)
    public void javaReforged$extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        ci.cancel();
    }

    @Redirect(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/EnchantmentMenu;clickMenuButton(Lnet/minecraft/world/entity/player/Player;I)Z"))
    public boolean javaReforged$clickMenuButton(EnchantmentMenu instance, Player player, int buttonId) {
        return false;
    }
}
