package eu.pb4.warps.ui;

import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.warps.ModInit;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

public class GuiUtils {
    public static final Style TEXTURE_STYLE = Style.EMPTY.withFont(Identifier.of("pbwarps:gui")).withColor(Formatting.WHITE);

    public static final GuiElement EMPTY = GuiElement.EMPTY;
    public static final GuiElement EMPTY_STACK = new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE)
            .setName(Text.empty())
            .setCustomModelData(requestModel(Items.WHITE_STAINED_GLASS_PANE, "empty").value())
            .hideTooltip().build();
    public static final GuiElement FILLER = Util.make(() -> new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE)
                .setName(Text.empty())
                .hideTooltip().build());
    private static final PolymerModelData BACK_TEXTURE = requestModel(Items.STRUCTURE_VOID, "back");
    private static final PolymerModelData NEXT_PAGE_TEXTURE = requestModel(Items.STRUCTURE_VOID, "next_page");
    private static final PolymerModelData PREVIOUS_PAGE_TEXTURE = requestModel(Items.STRUCTURE_VOID, "previous_page");
    private static final PolymerModelData FLAME_TEXTURE = requestModel(Items.STRUCTURE_VOID, "flame");
    private static final PolymerModelData XP_TEXTURE = requestModel(Items.STRUCTURE_VOID, "xp");
    private static final PolymerModelData PAGE_TEXTURE = requestModel(Items.STRUCTURE_VOID, "page");

    private static PolymerModelData requestModel(Item item, String back) {
        return PolymerResourcePackUtils.requestModel(item, ModInit.id("sgui/elements/" + back));
    }

    public static void register() {
    }

    public static GuiElementBuilder page(ServerPlayerEntity player, int current, int max) {
        return (new GuiElementBuilder(Items.BOOK)).noDefaults().setName(
                Text.translatable("text.polydex.view.pages",
                        Text.literal("" + current).formatted(Formatting.WHITE),
                        Text.literal("" + max).formatted(Formatting.WHITE)
                ).formatted(Formatting.AQUA)
        );
    }

    public static GuiElement backButton(ServerPlayerEntity player, Runnable callback, boolean back) {
        return backBase(player)
                .setName(Text.translatable(back ? "gui.back" : "text.pbwarps.close").formatted(Formatting.RED))
                .noDefaults()
                .hideDefaultTooltip()
                .setCallback((x, y, z) -> {
                    playClickSound(player);
                    callback.run();
                }).build();
    }

    private static GuiElementBuilder backBase(ServerPlayerEntity player) {
        return hasTexture(player) ?
                new GuiElementBuilder(BACK_TEXTURE.item()).noDefaults().setCustomModelData(BACK_TEXTURE.value())
                : new GuiElementBuilder(Items.STRUCTURE_VOID).noDefaults();
    }

    public static final void playClickSound(ServerPlayerEntity player) {
        player.playSoundToPlayer(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 0.5f, 1);
    }

    public static GuiElement nextPage(ServerPlayerEntity player, PageAware gui) {
        return nextPageBase(player)
                .setName(Text.translatable("spectatorMenu.next_page").formatted(Formatting.WHITE))
                .noDefaults()
                .hideDefaultTooltip()
                .setCallback((x, y, z) -> {
                    playClickSound(player);
                    gui.nextPage();
                }).build();
    }

    private static GuiElementBuilder nextPageBase(ServerPlayerEntity player) {
        return hasTexture(player)
                ? new GuiElementBuilder(NEXT_PAGE_TEXTURE.item()).noDefaults().setCustomModelData(NEXT_PAGE_TEXTURE.value())
                : new GuiElementBuilder(Items.PLAYER_HEAD).noDefaults().setSkullOwner(GuiHeadTextures.GUI_NEXT_PAGE);
    }

    private static GuiElementBuilder previousPageBase(ServerPlayerEntity player) {
        return hasTexture(player)
                ? new GuiElementBuilder(PREVIOUS_PAGE_TEXTURE.item()).noDefaults().setCustomModelData(PREVIOUS_PAGE_TEXTURE.value())
                : new GuiElementBuilder(Items.PLAYER_HEAD).noDefaults().setSkullOwner(GuiHeadTextures.GUI_PREVIOUS_PAGE);
    }

    public static GuiElement previousPage(ServerPlayerEntity player, PageAware gui) {
        return previousPageBase(player)
                .setName(Text.translatable("spectatorMenu.previous_page").formatted(Formatting.WHITE))
                .noDefaults()
                .hideDefaultTooltip()
                .setCallback((x, y, z) -> {
                    playClickSound(player);
                    gui.previousPage();
                }).build();
    }

    public static boolean hasTexture(ServerPlayerEntity player) {
        return PolymerResourcePackUtils.hasMainPack(player);
    }

    public static GuiElementInterface fillerStack(ServerPlayerEntity player) {
        return hasTexture(player) ? EMPTY_STACK : FILLER;
    }

    public static Text formatTexturedText(ServerPlayerEntity player, @Nullable Text texture, @Nullable Text input) {
        if (PolymerResourcePackUtils.hasMainPack(player)) {
            var text = Text.empty();
            var textTexture = Text.empty().setStyle(TEXTURE_STYLE);

            if (texture != null) {
                textTexture.append("a").append(texture).append("b");
            }

            if (!textTexture.getSiblings().isEmpty()) {
                text.append(textTexture);
            }

            if (input != null) {
                text.append(input);
            }
            return text;
        } else {
            return input != null ? input : Text.empty();
        }
    }
}
