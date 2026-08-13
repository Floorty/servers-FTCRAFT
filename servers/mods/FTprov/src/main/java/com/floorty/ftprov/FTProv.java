package com.floorty.ftprov;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Mod(FTProv.MOD_ID)
public final class FTProv {
    public static final String MOD_ID = "ftprov";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "ftprov.json";

    public FTProv(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(FTProv::registerCommands);
        LOGGER.info("FTProv initialized");
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        registerRoot(event, "province");
        registerRoot(event, "prov");
    }

    private static void registerRoot(RegisterCommandsEvent event, String name) {
        event.getDispatcher().register(Commands.literal(name)
                .then(Commands.literal("buy")
                        .executes(ctx -> unavailable(ctx.getSource(), "Функция пока недоступна.")))
                .then(Commands.literal("info").executes(ctx -> info(ctx.getSource())))
                .then(Commands.literal("members").executes(ctx -> members(ctx.getSource())))
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> invite(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("join").executes(ctx -> join(ctx.getSource())))
                .then(Commands.literal("leave").executes(ctx -> leave(ctx.getSource())))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> kick(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("transfer")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> transfer(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("delete").executes(ctx -> delete(ctx.getSource())))
                .then(Commands.literal("bank").executes(ctx -> unavailable(ctx.getSource(), "Казна будет подключена через FTcoins.")))
                .then(Commands.literal("deposit").executes(ctx -> unavailable(ctx.getSource(), "Пополнение казны будет подключено через FTcoins.")))
                .then(Commands.literal("withdraw").executes(ctx -> unavailable(ctx.getSource(), "Снятие с казны будет подключено через FTcoins.")))
                .then(Commands.literal("tax").executes(ctx -> unavailable(ctx.getSource(), "Налоговый сбор: дополнительные команды ещё проектируются."))));
    }

    private static int buy(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Data data = load(source.getServer());
        if (findByMember(data, player.getUUID()) != null) return fail(source, "Вы уже состоите в системе: Провинция.");
        Settlement s = new Settlement();
        s.name = name.trim();
        s.leader = player.getUUID();
        s.members.add(player.getUUID());
        data.settlements.add(s);
        save(source.getServer(), data);
        source.sendSuccess(() -> Component.literal("Провинция «" + s.name + "» создан. Ваша роль: Лорд."), false);
        return 1;
    }

    private static int info(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Settlement s = findByMember(load(source.getServer()), p.getUUID());
        if (s == null) return fail(source, "Вы не состоите в системе: Провинция.");
        source.sendSuccess(() -> Component.literal("Провинция: " + s.name + " | участников: " + s.members.size() + " | лидер: " + nameOf(source.getServer(), s.leader)), false);
        return 1;
    }

    private static int members(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Settlement s = findByMember(load(source.getServer()), p.getUUID());
        if (s == null) return fail(source, "Вы не состоите в системе: Провинция.");
        String list = s.members.stream().map(id -> nameOf(source.getServer(), id)).reduce((a,b) -> a + ", " + b).orElse("-");
        source.sendSuccess(() -> Component.literal("Участники: " + list), false);
        return 1;
    }

    private static int invite(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Data data = load(source.getServer());
        Settlement s = findByLeader(data, p.getUUID());
        if (s == null) return fail(source, "Только Лорд может приглашать.");
        if (findByMember(data, target.getUUID()) != null) return fail(source, "Игрок уже состоит в поселении этой системы.");
        s.invites.add(target.getUUID());
        save(source.getServer(), data);
        target.sendSystemMessage(Component.literal("Вас пригласили в Провинция «" + s.name + "». Используйте /prov join."));
        source.sendSuccess(() -> Component.literal("Приглашение отправлено игроку " + target.getName().getString() + "."), false);
        return 1;
    }

    private static int join(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Data data = load(source.getServer());
        if (findByMember(data, p.getUUID()) != null) return fail(source, "Вы уже состоите в поселении этой системы.");
        Settlement target = data.settlements.stream().filter(s -> s.invites.contains(p.getUUID())).findFirst().orElse(null);
        if (target == null) return fail(source, "У вас нет приглашения.");
        target.invites.remove(p.getUUID());
        target.members.add(p.getUUID());
        save(source.getServer(), data);
        source.sendSuccess(() -> Component.literal("Вы вступили в Провинция «" + target.name + "»."), false);
        return 1;
    }

    private static int leave(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Data data = load(source.getServer());
        Settlement s = findByMember(data, p.getUUID());
        if (s == null) return fail(source, "Вы нигде не состоите.");
        if (s.leader.equals(p.getUUID())) return fail(source, "Сначала передайте лидерство или удалите поселение.");
        s.members.remove(p.getUUID());
        save(source.getServer(), data);
        source.sendSuccess(() -> Component.literal("Вы покинули Провинция «" + s.name + "»."), false);
        return 1;
    }

    private static int kick(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Data data = load(source.getServer());
        Settlement s = findByLeader(data, p.getUUID());
        if (s == null) return fail(source, "Только Лорд может исключать.");
        if (!s.members.contains(target.getUUID()) || s.leader.equals(target.getUUID())) return fail(source, "Этого игрока нельзя исключить.");
        s.members.remove(target.getUUID());
        save(source.getServer(), data);
        target.sendSystemMessage(Component.literal("Вас исключили из системы: Провинция."));
        return success(source, "Игрок исключён.");
    }

    private static int transfer(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Data data = load(source.getServer());
        Settlement s = findByLeader(data, p.getUUID());
        if (s == null) return fail(source, "Только Лорд может передать лидерство.");
        if (!s.members.contains(target.getUUID())) return fail(source, "Игрок должен быть участником.");
        s.leader = target.getUUID();
        save(source.getServer(), data);
        return success(source, "Лидерство передано игроку " + target.getName().getString() + ".");
    }

    private static int delete(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Data data = load(source.getServer());
        Settlement s = findByLeader(data, p.getUUID());
        if (s == null) return fail(source, "Только Лорд может удалить поселение.");
        data.settlements.remove(s);
        save(source.getServer(), data);
        return success(source, "Провинция удалён.");
    }

    private static int unavailable(CommandSourceStack source, String text) { return success(source, text); }
    private static int success(CommandSourceStack source, String text) { source.sendSuccess(() -> Component.literal(text), false); return 1; }
    private static int fail(CommandSourceStack source, String text) { source.sendFailure(Component.literal(text)); return 0; }
    private static Settlement findByMember(Data d, UUID id) { return d.settlements.stream().filter(s -> s.members.contains(id)).findFirst().orElse(null); }
    private static Settlement findByLeader(Data d, UUID id) { return d.settlements.stream().filter(s -> s.leader.equals(id)).findFirst().orElse(null); }
    private static String nameOf(MinecraftServer server, UUID id) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        return online != null ? online.getName().getString() : id.toString();
    }
    private static Path path(MinecraftServer server) { return server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME); }
    private static Data load(MinecraftServer server) {
        Path p = path(server);
        if (!Files.exists(p)) return new Data();
        try { Data d = GSON.fromJson(Files.readString(p), Data.class); return d == null ? new Data() : d; }
        catch (Exception e) { LOGGER.error("Failed to load " + FILE_NAME, e); return new Data(); }
    }
    private static void save(MinecraftServer server, Data data) {
        try { Files.writeString(path(server), GSON.toJson(data)); }
        catch (IOException e) { LOGGER.error("Failed to save " + FILE_NAME, e); }
    }

    private static final class Data { List<Settlement> settlements = new ArrayList<>(); }
    private static final class Settlement {
        String name = "";
        UUID leader;
        Set<UUID> members = new LinkedHashSet<>();
        Set<UUID> invites = new LinkedHashSet<>();
    }
}
