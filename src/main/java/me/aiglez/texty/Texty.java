package me.aiglez.texty;

import com.google.common.collect.Maps;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.util.Codec;
import net.minecraft.server.v1_13_R2.MojangsonParser;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public final class Texty extends JavaPlugin implements Listener {

    private static final Codec<NBTTagCompound, String, IOException, IOException> NBT_CODEC = new Codec<NBTTagCompound, String, IOException, IOException>() {
        @Override
        public @NonNull NBTTagCompound decode(final @NonNull String encoded) throws IOException {
            try {
                return MojangsonParser.parse(encoded);
            } catch (final CommandSyntaxException e) {
                throw new IOException(e);
            }
        }

        @Override
        public @NonNull String encode(final @NonNull NBTTagCompound decoded) {
            return decoded.toString();
        }
    };
    private static final String PLACEHOLDER = "[item]";
    private static final Component ERROR_NOT_HOLDING_ITEM = Component.text()
            .append(Component.text("(", NamedTextColor.DARK_GRAY, TextDecoration.BOLD))
            .append(Component.text("!", NamedTextColor.RED, TextDecoration.BOLD))
            .append(Component.text(")", NamedTextColor.DARK_GRAY, TextDecoration.BOLD))
            .append(Component.text(" You aren't holding an item.", NamedTextColor.RED))
            .build();

    private Map<UUID, Long> cooldown;
    private BukkitAudiences adventure;

    @Override
    public void onEnable() {
        this.cooldown = Maps.newHashMap();
        this.adventure = BukkitAudiences.create(this);

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        if(this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
    }

    @SuppressWarnings("PatternValidation")
    @EventHandler
    public void onAsyncChat(final AsyncPlayerChatEvent e) {
        final Player player = e.getPlayer();
        final String message = e.getMessage();

        if(StringUtils.containsIgnoreCase(message, PLACEHOLDER)) {
            final ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR) {
                adventure.player(player).sendMessage(ERROR_NOT_HOLDING_ITEM);
                return;
            }

            Component itemDisplayName;
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                itemDisplayName = LegacyComponentSerializer.legacyAmpersand().deserialize(item.getItemMeta().getDisplayName());
            } else {
                itemDisplayName = Component.text(StringUtils.capitalize(item.getType().name().replaceAll("_", " ")),
                        NamedTextColor.WHITE);
            }

            NBTTagCompound tag = CraftItemStack.asNMSCopy(item).getOrCreateTag();
            final HoverEvent<HoverEvent.ShowItem> hoverEvent = HoverEvent.showItem(
                    Key.key(item.getType().getKey().getNamespace(), item.getType().getKey().getKey()), item.getAmount(), asBinaryTag(tag)
            );

            final TextComponent component = Component.text("»", NamedTextColor.DARK_GRAY)
                    .append(Component.space())
                    .append(
                            Component.text(player.getName()).color(NamedTextColor.AQUA)
                    )
                    .append(Component.space())
                    .append(
                            Component.text(message.replace(PLACEHOLDER, "")).color(NamedTextColor.AQUA)
                    )
                    .append(Component.space())
                    .append(
                            Component.text("»", NamedTextColor.DARK_GRAY)
                    )
                    .append(Component.space())
                    .append(
                            itemDisplayName.hoverEvent(hoverEvent)
                    )
                    .append(Component.space())
                    .append(
                            Component.text("«", NamedTextColor.DARK_GRAY)
                    )
                    .append(Component.space())
                    .append(
                            Component.text(message.replace(PLACEHOLDER, "")).color(NamedTextColor.AQUA)
                    );

            adventure.all().sendMessage(component);
            e.setCancelled(true);
        }
    }

    private BinaryTagHolder asBinaryTag(final NBTTagCompound tag) {
        try {
            return BinaryTagHolder.encode(tag, NBT_CODEC);
        } catch (final IOException e) {
            return null;
        }
    }
}
