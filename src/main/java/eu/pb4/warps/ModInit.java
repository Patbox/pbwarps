package eu.pb4.warps;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.warps.ui.GuiUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;

public class ModInit implements ModInitializer {
    public static Identifier id(String s) {
        return Identifier.of("pbwarps", s);
    }

    @Override
    public void onInitialize() {
        PolymerResourcePackUtils.addModAssets("pbwarps");
        GuiUtils.register();
        CommandRegistrationCallback.EVENT.register(WarpCommands::init);
        ServerLifecycleEvents.SERVER_STARTING.register(WarpManager::setup);
        ServerLifecycleEvents.SERVER_STOPPED.register((x) -> WarpManager.destroy());
        ServerLifecycleEvents.BEFORE_SAVE.register((server, a, b) -> WarpManager.get().save());
    }
}
