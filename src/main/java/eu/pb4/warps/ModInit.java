package eu.pb4.warps;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.WorldSavePath;

public class ModInit implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(WarpCommands::init);
        ServerLifecycleEvents.SERVER_STARTING.register(WarpManager::setup);
        ServerLifecycleEvents.SERVER_STOPPED.register((x) -> WarpManager.destroy());
        ServerLifecycleEvents.BEFORE_SAVE.register((server, a, b) -> WarpManager.get().save());
    }
}
