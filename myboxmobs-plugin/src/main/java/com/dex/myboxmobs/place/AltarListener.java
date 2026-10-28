package com.dex.myboxmobs.place;

import com.dex.myboxmobs.config.Messages;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Right-click to summon; altar blocks cannot be broken, blown up or pushed (spec 8 step 6). */
public final class AltarListener implements Listener {

    private final AltarService altars;
    private final Messages messages;

    public AltarListener(AltarService altars, Messages messages) {
        this.altars = altars;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND || e.getClickedBlock() == null) {
            return;
        }
        AltarDef altar = altars.at(e.getClickedBlock());
        if (altar == null) return;
        e.setCancelled(true);
        altars.use(e.getPlayer(), altar);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (altars.at(e.getBlock()) == null) return;
        e.setCancelled(true);
        messages.send(e.getPlayer(), "shrine.protected");
    }

    /** While the altar block is hidden (boss alive) nothing may be placed or flow into its place. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(org.bukkit.event.block.BlockPlaceEvent e) {
        if (altars.at(e.getBlockPlaced()) != null) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFlow(org.bukkit.event.block.BlockFromToEvent e) {
        if (altars.at(e.getToBlock()) != null) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> altars.at(b) != null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> altars.at(b) != null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        for (Block b : e.getBlocks()) {
            if (altars.at(b) != null) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        for (Block b : e.getBlocks()) {
            if (altars.at(b) != null) {
                e.setCancelled(true);
                return;
            }
        }
    }
}
