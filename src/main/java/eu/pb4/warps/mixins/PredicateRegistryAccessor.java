package eu.pb4.warps.mixins;

import com.mojang.serialization.MapCodec;
import eu.pb4.predicate.api.MinecraftPredicate;
import eu.pb4.predicate.api.PredicateRegistry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(PredicateRegistry.class)
public interface PredicateRegistryAccessor {
    @Accessor
    static Map<Identifier, MapCodec<MinecraftPredicate>> getCODECS() {
        throw new UnsupportedOperationException();
    }
}
