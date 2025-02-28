package eu.pb4.warps;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import eu.pb4.predicate.api.PredicateContext;
import eu.pb4.predicate.api.PredicateRegistry;
import eu.pb4.warps.data.Target;
import eu.pb4.warps.data.WarpData;
import eu.pb4.warps.mixins.PredicateRegistryAccessor;
import eu.pb4.warps.ui.WarpSelectGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Objects;
import java.util.Optional;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class WarpCommands {
    private static final SuggestionProvider<ServerCommandSource> WARP_ID_SUGGESTION_WITH_PREDICATE = (context, builder) -> {
        var ctx = PredicateContext.of(context.getSource());
        for (var warp : WarpManager.get().warps()) {
            if (warp.id().startsWith(builder.getRemainingLowerCase()) && (warp.predicate().isEmpty() || warp.predicate().get().test(ctx).success())) {
                builder.suggest(warp.id(), warp.name().text());
            }
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<ServerCommandSource> WARP_ID_SUGGESTION = (context, builder) -> {
        for (var warp : WarpManager.get().warps()) {
            if (warp.id().startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(warp.id(), warp.name().text());
            }
        }
        return builder.buildFuture();
    };

    public static void init(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
        dispatcher.register(literal("warp")
                .requires(Permissions.require("pbwarps.command", true))
                .executes(WarpCommands::openWarpUi)
                .then(argument("id", StringArgumentType.word()).suggests(WARP_ID_SUGGESTION_WITH_PREDICATE)
                        .executes(WarpCommands::warpTeleportSelf)
                )
        );

        dispatcher.register(literal("warps")
                .requires(Permissions.require("pbwarps.warps_command", true))
                .then(literal("create")
                        .requires(Permissions.require("pbwarps.create", 2))
                        .then(argument("id", StringArgumentType.word())
                                .executes(WarpCommands::createWarp)
                                .then(argument("position", Vec3ArgumentType.vec3(true))
                                        .executes(WarpCommands::createWarp)
                                        .then(argument("rotation", RotationArgumentType.rotation())
                                                .executes(WarpCommands::createWarp)
                                                .then(argument("world", DimensionArgumentType.dimension())
                                                        .executes(WarpCommands::createWarp)
                                                )
                                        )
                                )
                        )
                )
                .then(literal("modify")
                        .requires(Permissions.require("pbwarps.modify", 2))
                        .then(argument("id", StringArgumentType.word())
                                .suggests(WARP_ID_SUGGESTION)
                                .then(literal("name")
                                        .requires(Permissions.require("pbwarps.modify.name", 2))
                                        .then(argument("name", StringArgumentType.greedyString()).executes(WarpCommands::setName))
                                )
                                .then(literal("position")
                                        .requires(Permissions.require("pbwarps.modify.position", 2))
                                        .executes(WarpCommands::setPosition)
                                        .then(argument("position", Vec3ArgumentType.vec3(true))
                                                .executes(WarpCommands::setPosition)
                                                .then(argument("rotation", RotationArgumentType.rotation())
                                                        .executes(WarpCommands::setPosition)
                                                        .then(argument("world", DimensionArgumentType.dimension())
                                                                .executes(WarpCommands::setPosition)
                                                        )
                                                )
                                        )
                                )
                                .then(literal("icon")
                                        .requires(Permissions.require("pbwarps.modify.icon", 2))
                                        .then(argument("icon", ItemStackArgumentType.itemStack(access)).executes(WarpCommands::setIcon))
                                )
                                .then(literal("predicate")
                                        .requires(Permissions.require("pbwarps.modify.predicate", 2))
                                        .then(literal("clear").executes(WarpCommands::clearPredicate))
                                        .then(argument("predicate_type", IdentifierArgumentType.identifier())
                                                .suggests((context, builder) -> CommandSource.suggestIdentifiers(PredicateRegistryAccessor.getCODECS().keySet(), builder))
                                                .executes(WarpCommands::setPredicate)
                                                .then(argument("data", NbtCompoundArgumentType.nbtCompound())
                                                        .executes(WarpCommands::setPredicate)
                                                )
                                        )
                                )

                        )
                )
                .then(literal("remove")
                        .requires(Permissions.require("pbwarps.remove", 2))
                        .then(argument("id", StringArgumentType.word())
                                .suggests(WARP_ID_SUGGESTION)
                                .executes(WarpCommands::removeWarp)
                        )
                )
                .then(literal("teleport")
                        .requires(Permissions.require("pbwarps.teleport", 2))
                        .then(argument("id", StringArgumentType.word())
                                .suggests(WARP_ID_SUGGESTION)
                                .executes(WarpCommands::warpTeleportSelfUnrestricted)
                                .then(argument("entity", EntityArgumentType.entities())
                                        .requires(Permissions.require("pbwarps.teleport.others", 2))
                                        .executes(WarpCommands::warpTeleportOthers)
                                )
                        )
                )
                .then(literal("info")
                        .requires(Permissions.require("pbwarps.info", 2))
                        .then(argument("id", StringArgumentType.word())
                                .suggests(WARP_ID_SUGGESTION)
                                .executes(WarpCommands::showInfo)
                        )

                )
        );
    }

    private static int showInfo(CommandContext<ServerCommandSource> context) {
        var id = StringArgumentType.getString(context, "id");
        var warp = WarpManager.get().get(id);
        if (warp == null) {
            context.getSource().sendMessage(Text.translatable("command.pbwarps.invalid_warp", id).formatted(Formatting.RED));
            return 0;
        }
        context.getSource().sendMessage(Text.translatable("command.pbwarps.info.id", warp.id()));
        context.getSource().sendMessage(Text.translatable("command.pbwarps.info.name", warp.name().text()));
        context.getSource().sendMessage(Text.translatable("command.pbwarps.info.unformatted_name", warp.name().input()));
        context.getSource().sendMessage(Text.translatable("command.pbwarps.info.icon", warp.icon().toHoverableText()));
        context.getSource().sendMessage(Text.translatable("command.pbwarps.info.position", warp.target().pos().toString(), warp.target().yaw(), warp.target().pitch(), warp.target().world().getValue().toString()));
        if (warp.predicate().isPresent()) {
            context.getSource().sendMessage(Text.translatable("command.pbwarps.info.predicate_type", warp.predicate().get().identifier().toString()));
            context.getSource().sendMessage(Text.translatable("command.pbwarps.info.predicate_data", NbtHelper.toPrettyPrintedText(
                    warp.predicate().get().codec().codec().encodeStart(context.getSource().getRegistryManager().getOps(NbtOps.INSTANCE), warp.predicate().get()).getOrThrow()
            )));
        }
        return 1;
    }

    private static int clearPredicate(CommandContext<ServerCommandSource> context) {
        var id = StringArgumentType.getString(context, "id");
        if (WarpManager.get().updateWarp(id, x -> x.withPredicate(null))) {
            context.getSource().sendMessage(Text.translatable("command.pbwarps.modify.predicate.clear", id));
            return 1;
        }

        context.getSource().sendMessage(Text.translatable("command.pbwarps.invalid_warp").formatted(Formatting.RED));
        return 0;
    }

    private static int setPredicate(CommandContext<ServerCommandSource> context) {
        var id = StringArgumentType.getString(context, "id");
        var type = IdentifierArgumentType.getIdentifier(context, "predicate_type");
        var codec = PredicateRegistry.getCodec(type);
        if (codec == null) {
            context.getSource().sendMessage(Text.translatable("command.pbwarps.modify.predicate.invalid_predicate", type.toString()).formatted(Formatting.RED));
            return 0;
        }
        NbtCompound data = new NbtCompound();

        try {
            data = NbtCompoundArgumentType.getNbtCompound(context, "data");
        } catch (Throwable ignored) {
        }

        var predicate = codec.codec().decode(context.getSource().getRegistryManager().getOps(NbtOps.INSTANCE), data);
        if (predicate.isError()) {
            var alt = new NbtCompound();
            alt.put("value", data);
            var maybe = codec.codec().decode(context.getSource().getRegistryManager().getOps(NbtOps.INSTANCE), alt);
            if (maybe.isSuccess()) {
                predicate = maybe;
            }
        }

        if (predicate.error().isPresent()) {
            context.getSource().sendMessage(Text.translatable("command.pbwarps.modify.predicate.invalid_predicate_data", type.toString()).formatted(Formatting.RED));
            context.getSource().sendMessage(Text.literal(predicate.error().get().message()).formatted(Formatting.RED));
            return 0;
        }

        var finalPredicate = predicate;
        if (WarpManager.get().updateWarp(id, x -> x.withPredicate(finalPredicate.result().get().getFirst()))) {
            context.getSource().sendMessage(Text.translatable("command.pbwarps.modify.predicate", id, type.toString(), NbtHelper.toPrettyPrintedText(data)));
            return 1;
        }

        context.getSource().sendMessage(Text.translatable("command.pbwarps.invalid_warp").formatted(Formatting.RED));
        return 0;
    }

    private static int removeWarp(CommandContext<ServerCommandSource> context) {
        var id = StringArgumentType.getString(context, "id");

        if (WarpManager.get().removeWarp(id)) {
            context.getSource().sendMessage(Text.translatable("command.pbwarps.remove", id));
            return 1;
        }

        context.getSource().sendMessage(Text.translatable("command.pbwarps.invalid_warp").formatted(Formatting.RED));
        return 0;
    }

    private static int setIcon(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var id = StringArgumentType.getString(context, "id");
        var icon = ItemStackArgumentType.getItemStackArgument(context, "icon").createStack(1, false);

        if (WarpManager.get().updateWarp(id, x -> x.withIcon(icon))) {
            context.getSource().sendMessage(Text.translatable("command.pbwarps.modify.icon", id, icon.toHoverableText()));
            return 1;
        }

        context.getSource().sendMessage(Text.translatable("command.pbwarps.invalid_warp").formatted(Formatting.RED));
        return 0;
    }

    private static int setPosition(CommandContext<ServerCommandSource> context) {
        var id = StringArgumentType.getString(context, "id");
        var target = getTarget(context);

        if (WarpManager.get().updateWarp(id, x -> x.withTarget(target))) {
            context.getSource().sendMessage(Text.translatable("command.pbwarps.modify.position", id, target.pos().toString(), target.yaw(), target.pitch(), target.world().getValue().toString()));
            return 1;
        }

        context.getSource().sendMessage(Text.translatable("command.pbwarps.invalid_warp").formatted(Formatting.RED));
        return 0;
    }

    private static int setName(CommandContext<ServerCommandSource> context) {
        var id = StringArgumentType.getString(context, "id");
        var name = StringArgumentType.getString(context, "name");

        if (WarpManager.get().updateWarp(id, x -> x.withName(name))) {
            context.getSource().sendMessage(Text.translatable("command.pbwarps.modify.name", id, Objects.requireNonNull(WarpManager.get().get(id)).name().text()));
            return 1;
        }

        context.getSource().sendMessage(Text.translatable("command.pbwarps.invalid_warp").formatted(Formatting.RED));
        return 0;
    }

    private static int createWarp(CommandContext<ServerCommandSource> context) {
        var id = StringArgumentType.getString(context, "id");
        var target = getTarget(context);

        if (WarpManager.get().addWarp(new WarpData(id, target))) {
            context.getSource().sendMessage(Text.translatable("command.pbwarps.create.success", id, target.pos().toString(), target.yaw(), target.pitch(), target.world().getValue().toString()));
            return 1;
        }

        context.getSource().sendMessage(Text.translatable("command.pbwarps.create.duplicate", id).formatted(Formatting.RED));
        return 0;
    }

    private static Target getTarget(CommandContext<ServerCommandSource> context) {
        var world = context.getSource().getWorld().getRegistryKey();
        var pos = context.getSource().getPosition();
        float pitch = context.getSource().getRotation().x;
        float yaw = context.getSource().getRotation().y;

        try {
            pos = Vec3ArgumentType.getVec3(context, "position");
        } catch (Throwable ignored) {
        }
        try {
            var vec = RotationArgumentType.getRotation(context, "rotation").toAbsoluteRotation(context.getSource());
            pitch = vec.x;
            yaw = vec.y;
        } catch (Throwable ignored) {
        }
        try {
            world = DimensionArgumentType.getDimensionArgument(context, "world").getRegistryKey();
        } catch (Throwable ignored) {
        }

        return new Target(world, pos, Optional.of(pitch), Optional.of(yaw));
    }

    private static int warpTeleportSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var id = StringArgumentType.getString(context, "id");
        var warp = WarpManager.get().get(id);
        if (warp != null && warp.canUse(context.getSource())) {
            warp.handleTeleport(context.getSource().getEntityOrThrow());
            context.getSource().sendMessage(Text.translatable("command.pbwarps.warp", warp.name().text()));
            return 1;
        }
        context.getSource().sendMessage(Text.translatable("command.pbwarps.invalid_warp", id).formatted(Formatting.RED));
        return 0;
    }

    private static int warpTeleportSelfUnrestricted(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var id = StringArgumentType.getString(context, "id");
        var warp = WarpManager.get().get(id);
        if (warp != null) {
            warp.handleTeleport(context.getSource().getEntityOrThrow());
            context.getSource().sendMessage(Text.translatable("command.pbwarps.warp", warp.name().text()));
            return 1;
        }
        context.getSource().sendMessage(Text.translatable("command.pbwarps.invalid_warp", id).formatted(Formatting.RED));
        return 0;
    }

    private static int warpTeleportOthers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var id = StringArgumentType.getString(context, "id");
        var entities = EntityArgumentType.getEntities(context, "entity");
        var warp = WarpManager.get().get(id);
        if (warp != null) {
            for (var entity : entities) {
                warp.handleTeleport(entity);
                context.getSource().sendMessage(Text.translatable("command.pbwarps.teleport.other", entity.getName(), warp.name().text()));
            }
            return 1;
        }
        context.getSource().sendMessage(Text.translatable("command.pbwarps.invalid_warp", id).formatted(Formatting.RED));
        return 0;
    }

    private static int openWarpUi(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        WarpSelectGui.open(context.getSource().getPlayerOrThrow());
        return 0;
    }
}
