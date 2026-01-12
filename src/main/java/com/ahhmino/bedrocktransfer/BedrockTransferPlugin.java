package com.ahhmino.bedrocktransfer;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.geyser.api.GeyserApi;

import java.util.Objects;

public final class BedrockTransferPlugin extends JavaPlugin implements Listener {

    private static final String TARGET_IP = "mc.flyingpineapples.com"; // static IP for all transfers
    private static final int SIGN_OFFSET = 2; // blocks behind the plate

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("BedrockTransfer enabled");
    }

    @EventHandler
    public void onPlayerStep(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Only trigger when physically stepping on a block (pressure plate)
        if (event.getAction() != Action.PHYSICAL) return;

        Block plate = event.getClickedBlock();
        if (plate == null) return;

        // Only trigger on pressure plates
        if (!plate.getType().name().endsWith("_PRESSURE_PLATE")) return;

        // Determine the block 2 behind the plate (opposite of the plate's facing)
        // Simple approximation: use the player’s facing relative to plate
        Block signBlock = plate.getRelative(Objects.requireNonNull(player.getLocation().getBlock().getFace(plate)), SIGN_OFFSET);
        getLogger().info(signBlock.toString());

        // Must be a sign
        if (!(signBlock.getState() instanceof Sign sign)) return;

        // Read port from the first line
        String portText = PlainTextComponentSerializer.plainText().serialize(sign.getSide(Side.FRONT).line(0));
        getLogger().info(sign.getSide(Side.FRONT).line(0).toString());
        getLogger().info(portText);
        if (portText.isEmpty()) return;

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            player.sendMessage("Invalid port number on the sign.");
            return;
        }

        // Transfer the player
        transfer(player, port);
    }

    private void transfer(Player player, int port) {
        if (GeyserApi.api().isBedrockPlayer(player.getUniqueId())) {
            GeyserApi.api().transfer(
                    player.getUniqueId(),
                    TARGET_IP,
                    port
            );
        } else {
            ProtocolManager manager = ProtocolLibrary.getProtocolManager();

            PacketContainer packet = manager.createPacket(PacketType.Play.Server.TRANSFER);
            packet.getStrings().write(0, TARGET_IP);        // host
            packet.getIntegers().write(0, port + 1);     // port

            try {
                manager.sendServerPacket(player, packet);
            } catch (Exception e) {
                getLogger().severe(String.format("Failed to send Java player %s due to error: %s", player.getName(), e));
            }
        }
    }
}
