package eu.pb4.warps.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.WrappedText;
import eu.pb4.predicate.api.MinecraftPredicate;
import eu.pb4.predicate.api.PredicateContext;
import eu.pb4.predicate.api.PredicateRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public record WarpData(String id, WrappedText name, ItemStack icon, Target target,
                       Optional<MinecraftPredicate> predicate) {
    public static final NodeParser NAME_PARSER = NodeParser.builder().requireSafe().legacyAll().quickText().build();

    public static final Codec<WarpData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(WarpData::id),
            NAME_PARSER.codec().fieldOf("name").forGetter(WarpData::name),
            ItemStack.UNCOUNTED_CODEC.fieldOf("icon").forGetter(WarpData::icon),
            Target.CODEC.forGetter(WarpData::target),
            PredicateRegistry.CODEC.optionalFieldOf("predicate").forGetter(WarpData::predicate)
    ).apply(instance, WarpData::new));

    public WarpData(String id, Target target) {
        this(id, WrappedText.from(NAME_PARSER, id), Items.GRASS_BLOCK.getDefaultStack(), target, Optional.empty());
    }

    public WarpData withId(String id) {
        return new WarpData(id, name, icon, target, predicate);
    }

    public WarpData withName(String name) {
        return new WarpData(id, WrappedText.from(NAME_PARSER, name), icon, target, predicate);
    }

    public WarpData withIcon(ItemStack icon) {
        return new WarpData(id, name, icon, target, predicate);
    }

    public WarpData withTarget(Target target) {
        return new WarpData(id, name, icon, target, predicate);
    }

    public WarpData withPredicate(@Nullable MinecraftPredicate predicate) {
        return new WarpData(id, name, icon, target, Optional.ofNullable(predicate));
    }

    public void handleTeleport(Entity entity) {
        var target = this.target().asTeleportTarget(Objects.requireNonNull(entity.getWorld().getServer()), entity, this::teleportEffects);

        if (target != null) {
            if (!entity.isInvisible()) {
                entity.getWorld().sendEntityStatus(entity, (byte) 46);
                entity.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENTITY_PLAYER_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
            entity.teleportTo(target);
        }
    }

    private void teleportEffects(Entity entity) {
        if (!entity.isInvisible()) {
            entity.getWorld().emitGameEvent(GameEvent.TELEPORT, entity.getPos(), GameEvent.Emitter.of(entity));
            entity.getWorld().sendEntityStatus(entity, (byte) 46);
            entity.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENTITY_PLAYER_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }

    public boolean canUse(ServerCommandSource source) {
        return this.predicate.isEmpty() || this.predicate.get().test(PredicateContext.of(source)).success();
    }
}
