package eu.pb4.warps;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import eu.pb4.warps.data.WarpData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

public class WarpManager {
    private static final Codec<List<WarpData>> SAVE_CODEC = WarpData.CODEC.listOf().fieldOf("warps").codec();
    private static WarpManager manager = null;
    private final TreeMap<String, WarpData> warps = new TreeMap<>();
    private List<WarpData> warpArr = null;
    private final Path savePath;
    private final MinecraftServer server;

    public WarpManager(MinecraftServer server, Path path) {
        this.server = server;
        this.savePath = path;
    }

    public static WarpManager get() {
        return manager;
    }

    public static void setup(MinecraftServer server) {
        var path = server.getSavePath(WorldSavePath.ROOT).resolve("warps.json");
        manager = new WarpManager(server, path);
        if (Files.exists(path)) {
            try {
                var data = SAVE_CODEC.decode(server.getRegistryManager().getOps(JsonOps.INSTANCE), JsonParser.parseString(Files.readString(path)));
                data.result().get().getFirst().forEach(x -> manager.warps.put(x.id(), x));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static void destroy() {
        manager.save();
        manager = null;
    }

    public boolean addWarp(WarpData data) {
        if (this.warps.containsKey(data.id())) {
            return false;
        }

        this.warps.put(data.id(), data);
        warpArr = null;
        return true;
    }
    public boolean updateWarp(WarpData data) {
        if (!this.warps.containsKey(data.id())) {
            return false;
        }
        this.warps.put(data.id(), data);
        warpArr = null;
        return true;
    }

    public boolean updateWarp(String id, Function<WarpData, WarpData> modifier) {
        if (!this.warps.containsKey(id)) {
            return false;
        }
        var warp = modifier.apply(this.warps.get(id));
        if (!warp.id().equals(id)) {
            if (this.warps.containsKey(warp.id())) {
                return false;
            }
            this.warps.remove(id);
        }
        this.warps.put(warp.id(), warp);
        warpArr = null;
        return true;
    }

    public boolean removeWarp(String warp) {
        var removed = this.warps.remove(warp) != null;
        if (removed) {
            warpArr = null;
        }
        return removed;
    }

    @Nullable
    public WarpData get(String id) {
        return this.warps.get(id);
    }

    public List<WarpData> warps() {
        if (warpArr == null) {
            warpArr = new ArrayList<>(this.warps.values());
            warpArr.sort(Comparator.comparingInt(WarpData::priority).reversed().thenComparing(WarpData::id));
        }
        return warpArr;
    }

    public void save() {
        var data = SAVE_CODEC.encodeStart(server.getRegistryManager().getOps(JsonOps.INSTANCE), List.copyOf(this.warps.values()));
        if (data.isSuccess()) {
            try {
                Files.writeString(this.savePath, data.result().get().toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
