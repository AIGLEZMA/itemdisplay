package me.aiglez.texty;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.util.Codec;
import net.minecraft.server.v1_13_R2.MojangsonParser;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import org.apache.commons.lang.StringUtils;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ItemDisplayHelper {

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



    public static Component getDisplayName(final ItemStack itemStack) {
        Preconditions.checkNotNull(itemStack, "itemstack may not be null");
        Component itemDisplayName;
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
            itemDisplayName = LegacyComponentSerializer.legacyAmpersand().deserialize(itemStack.getItemMeta().getDisplayName())
                    .colorIfAbsent(NamedTextColor.WHITE);
        } else {
            itemDisplayName = Component.text(StringUtils.capitalize(itemStack.getType().name().replaceAll("_", " ")))
                    .color(NamedTextColor.WHITE);
        }
        return itemDisplayName;
    }

    public static String formatTimeGeneric(final long millis) {
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        final long second = seconds % 60;
        long minute = seconds / 60;
        String hourMsg = "";

        if (minute >= 60) {
            final long hour = seconds / 60 / 60;
            minute %= 60;

            hourMsg = hour + (hour == 1 ? " hour" : " hours") + " ";
        }

        return hourMsg + (minute != 0 ? minute : "") + (minute > 0 ? (minute == 1 ? " minute" : " minutes") + " " : "") + Long.parseLong(String.valueOf(second)) + (Long.parseLong(String.valueOf(second)) == 1 ? " second" : " seconds");

    }

    public static BinaryTagHolder asBinaryTag(final NBTTagCompound tag) {
        try {
            return BinaryTagHolder.encode(tag, NBT_CODEC);
        } catch (final IOException e) {
            return null;
        }
    }
}
