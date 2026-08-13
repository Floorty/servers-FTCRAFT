package com.floorty.ftcoins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Mod(FTCoins.MOD_ID)
public final class FTCoins {
    public static final String MOD_ID = "ftcoins";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "ftcoins.json";

    public FTCoins(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(FTCoins::registerCommands);
        NeoForge.EVENT_BUS.addListener(FTCoins::onLogin);
        LOGGER.info("FTcoins initialized");
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("money")
                .executes(ctx -> money(ctx.getSource()))
                .then(Commands.literal("add").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> addCurrency(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "amount"), false))))));
        event.getDispatcher().register(Commands.literal("pay")
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> pay(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"),
                                        IntegerArgumentType.getInteger(ctx, "amount"))))));
        event.getDispatcher().register(Commands.literal("donate")
                .executes(ctx -> donateMenu(ctx.getSource()))
                .then(Commands.literal("balance").executes(ctx -> donateBalance(ctx.getSource())))
                .then(Commands.literal("add").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> addCurrency(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "amount"), true))))));
    }

    private static int money(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Account account = account(load(source.getServer()), player.getUUID(), player.getName().getString());
        source.sendSuccess(() -> Component.literal("Баланс: " + account.coins + " монет."), false);
        return 1;
    }

    private static int donateBalance(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Account account = account(load(source.getServer()), player.getUUID(), player.getName().getString());
        source.sendSuccess(() -> Component.literal("Донат баланс: " + account.ftcoins + " FTcoins"), false);
        return 1;
    }

    private static int donateMenu(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Магазин FTcoins пока пуст: товары и цены ещё не добавлены."), false);
        return 1;
    }

    private static int addCurrency(CommandSourceStack source, String targetName, int amount, boolean donate) {
        GameProfile profile = resolveProfile(source.getServer(), targetName);
        if (profile == null) return fail(source, "Игрок «" + targetName + "» не найден.");
        Data data = load(source.getServer());
        Account target = account(data, profile.getId(), profile.getName());
        if (donate) target.ftcoins += amount;
        else target.coins += amount;
        save(source.getServer(), data);
        String currency = donate ? " FTcoins" : " монет";
        source.sendSuccess(() -> Component.literal("Игроку " + profile.getName() + " начислено " + amount + currency + "."), true);
        return 1;
    }

    private static int pay(CommandSourceStack source, String targetName, int amount) throws CommandSyntaxException {
        ServerPlayer sender = source.getPlayerOrException();
        MinecraftServer server = source.getServer();
        GameProfile profile = resolveProfile(server, targetName);
        if (profile == null) return fail(source, "Игрок «" + targetName + "» не найден. Он должен хотя бы один раз зайти на сервер.");
        if (profile.getId().equals(sender.getUUID())) return fail(source, "Нельзя переводить монеты самому себе.");

        Data data = load(server);
        Account from = account(data, sender.getUUID(), sender.getName().getString());
        if (from.coins < amount) return fail(source, "Недостаточно монет. Ваш баланс: " + from.coins + ".");
        Account to = account(data, profile.getId(), profile.getName());
        from.coins -= amount;
        to.coins += amount;

        ServerPlayer online = server.getPlayerList().getPlayer(profile.getId());
        if (online != null) {
            online.sendSystemMessage(Component.literal("Игрок " + sender.getName().getString() + " перевел вам " + amount + " монет."));
        } else {
            to.pending.add(new Pending(sender.getName().getString(), amount));
        }
        save(server, data);
        source.sendSuccess(() -> Component.literal("Вы перевели Игроку " + profile.getName() + " " + amount + " монет."), false);
        return 1;
    }

    private static GameProfile resolveProfile(MinecraftServer server, String name) {
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) return online.getGameProfile();
        return server.getProfileCache().get(name).orElse(null);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Data data = load(player.getServer());
        Account account = account(data, player.getUUID(), player.getName().getString());
        for (Pending pending : account.pending) {
            player.sendSystemMessage(Component.literal("Во время вашего отсутствия вам перевели "
                    + pending.amount + " монет. Игрок " + pending.sender + "."));
        }
        if (!account.pending.isEmpty()) {
            account.pending.clear();
            save(player.getServer(), data);
        }
    }

    private static int fail(CommandSourceStack source, String text) {
        source.sendFailure(Component.literal(text));
        return 0;
    }

    private static Account account(Data data, UUID id, String name) {
        Account value = data.accounts.computeIfAbsent(id.toString(), ignored -> new Account());
        value.lastName = name;
        return value;
    }

    private static Path path(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
    }

    private static Data load(MinecraftServer server) {
        Path file = path(server);
        if (!Files.exists(file)) return new Data();
        try {
            Data data = GSON.fromJson(Files.readString(file), Data.class);
            return data == null ? new Data() : data;
        } catch (Exception e) {
            LOGGER.error("Failed to load " + FILE_NAME, e);
            return new Data();
        }
    }

    private static void save(MinecraftServer server, Data data) {
        try {
            Files.writeString(path(server), GSON.toJson(data));
        } catch (IOException e) {
            LOGGER.error("Failed to save " + FILE_NAME, e);
        }
    }

    private static final class Data {
        Map<String, Account> accounts = new LinkedHashMap<>();
    }

    private static final class Account {
        String lastName = "";
        long coins = 0;
        long ftcoins = 0;
        List<Pending> pending = new ArrayList<>();
    }

    private static final class Pending {
        String sender;
        int amount;

        Pending(String sender, int amount) {
            this.sender = sender;
            this.amount = amount;
        }
    }
}
