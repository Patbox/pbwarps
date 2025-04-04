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
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class PagedGui extends SimpleGui implements PageAware {
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

    @Override
    public void nextPage() {
        this.page = Math.min(this.getPageAmount() - 1, this.page + 1);
        this.updateDisplay();
    }

    protected boolean canNextPage() {
        return this.getPageAmount() > this.page + 1;
    }

    @Override
    public void previousPage() {
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
                element = GuiUtils.EMPTY;
            }

            this.setSlot(i, element);
        }

        for (int i = 0; i < 9; i++) {
            var navElement = this.getNavElement(i);

            if (navElement == null) {
                navElement = GuiUtils.EMPTY;
            }

            this.setSlot(i + PAGE_SIZE, navElement);
        }
    }

    @Override
    public int getPage() {
        return this.page;
    }

    @Override
    public void setPage(int page) {
        this.page = MathHelper.clamp(page, 0, getPageAmount());
    }

    @Override
    public abstract int getPageAmount();

    protected abstract GuiElementInterface getElement(int id);

    protected GuiElementInterface getNavElement(int id) {
        return switch (id) {
            case 3 -> this.canPreviousPage() ? GuiUtils.previousPage(this.player,this) : GuiUtils.fillerStack(player);
            case 5 -> this.canNextPage() ? GuiUtils.nextPage(this.player,this) : GuiUtils.fillerStack(player);
            case 8 ->  GuiUtils.backButton(this.player, () -> {
                this.close(false);
            }, true);
            default -> GuiUtils.fillerStack(this.player);
        };
    }
}
