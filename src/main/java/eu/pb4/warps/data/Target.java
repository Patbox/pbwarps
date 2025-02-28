package eu.pb4.warps.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record Target(RegistryKey<World> world, Vec3d pos, Optional<Float> pitch, Optional<Float> yaw) {
    public static final MapCodec<Target> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RegistryKey.createCodec(RegistryKeys.WORLD).fieldOf("world").forGetter(Target::world),
            Vec3d.CODEC.fieldOf("pos").forGetter(Target::pos),
            Codec.FLOAT.optionalFieldOf("pitch").forGetter(Target::pitch),
            Codec.FLOAT.optionalFieldOf("yaw").forGetter(Target::yaw)
    ).apply(instance, Target::new));

    @Nullable
    public TeleportTarget asTeleportTarget(MinecraftServer server, Entity entity, TeleportTarget.PostDimensionTransition transition) {
        var world = server.getWorld(this.world);
        if (world == null) {
            return null;
        }
        return new TeleportTarget(world, this.pos, Vec3d.ZERO, yaw.orElse(entity.getYaw()), pitch.orElse(entity.getPitch()), transition);
    }
}
