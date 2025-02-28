package eu.pb4.warps.ui;

import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class PagedGui extends SimpleGui {
    public static final int PAGE_SIZE = 9 * 4;
    protected final Runnable closeCallback;
    public boolean ignoreCloseCallback;
    protected int page = 0;

    public PagedGui(ServerPlayerEntity player, @Nullable Runnable closeCallback) {
        super(ScreenHandlerType.GENERIC_9X5, player, false);
        this.closeCallback = closeCallback;
    }

    public static void playClickSound(ServerPlayerEntity player) {
        player.playSoundToPlayer(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 1, 1);
    }

    public void refreshOpen() {
        this.updateDisplay();
        this.open();
    }

    @Override
    public void onClose() {
        if (this.closeCallback != null && !ignoreCloseCallback) {
            this.closeCallback.run();
        }
    }

    protected void nextPage() {
        this.page = Math.min(this.getPageAmount() - 1, this.page + 1);
        this.updateDisplay();
    }

    protected boolean canNextPage() {
        return this.getPageAmount() > this.page + 1;
    }

    protected void previousPage() {
        this.page = Math.max(0, this.page - 1);
        this.updateDisplay();
    }

    protected boolean canPreviousPage() {
        return this.page - 1 >= 0;
    }

    protected void updateDisplay() {
        var offset = this.page * PAGE_SIZE;

        for (int i = 0; i < PAGE_SIZE; i++) {
            var element = this.getElement(offset + i);

            if (element == null) {
                element = DisplayElement.empty();
            }

            this.setSlot(i, element);
        }

        for (int i = 0; i < 9; i++) {
            var navElement = this.getNavElement(i);

            if (navElement == null) {
                navElement = DisplayElement.EMPTY;
            }

            this.setSlot(i + PAGE_SIZE, navElement);
        }
    }

    protected int getPage() {
        return this.page;
    }

    protected abstract int getPageAmount();

    protected abstract GuiElementInterface getElement(int id);

    protected GuiElementInterface getNavElement(int id) {
        return switch (id) {
            case 1 -> DisplayElement.previousPage(this);
            case 3 -> DisplayElement.nextPage(this);
            case 7 -> new GuiElementBuilder(Items.STRUCTURE_VOID)
                    .setName(ScreenTexts.BACK.copy().formatted(Formatting.RED))
                    .hideDefaultTooltip()
                    .setCallback((x, y, z) -> {
                                playClickSound(this.player);
                                this.close(this.closeCallback != null);
                            }
                    ).build();
            default -> DisplayElement.filler();
        };
    }

    public interface DisplayElement {
        GuiElementInterface EMPTY = new GuiElement(ItemStack.EMPTY, GuiElementInterface.EMPTY_CALLBACK);
        GuiElementInterface FILLER =
                new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE)
                        .setName(Text.empty())
                        .hideTooltip().build();

        static GuiElementInterface nextPage(PagedGui gui) {
            if (gui.canNextPage()) {
                return
                        new GuiElementBuilder(Items.PLAYER_HEAD)
                                .setName(Text.translatable("spectatorMenu.next_page").formatted(Formatting.WHITE))
                                .hideDefaultTooltip()
                                .setSkullOwner(GuiTextures.GUI_NEXT_PAGE)
                                .setCallback((x, y, z) -> {
                                    playClickSound(gui.player);
                                    gui.nextPage();
                                }).build();
            } else {
                return
                        new GuiElementBuilder(Items.PLAYER_HEAD)
                                .setName(Text.translatable("spectatorMenu.next_page").formatted(Formatting.DARK_GRAY))
                                .hideDefaultTooltip()
                                .setSkullOwner(GuiTextures.GUI_NEXT_PAGE_BLOCKED).build();
            }
        }

        static GuiElementInterface previousPage(PagedGui gui) {
            if (gui.canPreviousPage()) {
                return
                        new GuiElementBuilder(Items.PLAYER_HEAD)
                                .setName(Text.translatable("spectatorMenu.previous_page").formatted(Formatting.WHITE))
                                .hideDefaultTooltip()
                                .setSkullOwner(GuiTextures.GUI_PREVIOUS_PAGE)
                                .setCallback((x, y, z) -> {
                                    playClickSound(gui.player);
                                    gui.previousPage();
                                }).build();
            } else {
                return
                        new GuiElementBuilder(Items.PLAYER_HEAD)
                                .setName(Text.translatable("spectatorMenu.previous_page").formatted(Formatting.DARK_GRAY))
                                .hideDefaultTooltip()
                                .setSkullOwner(GuiTextures.GUI_PREVIOUS_PAGE_BLOCKED).build();
            }
        }

        static GuiElementInterface filler() {
            return FILLER;
        }

        static GuiElementInterface empty() {
            return EMPTY;
        }
    }
}
