package xyz.destiall.addons.managers;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.event.CancelReason;
import net.citizensnpcs.api.event.NPCDamageByEntityEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.api.trait.trait.Inventory;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.util.Vector;
import xyz.destiall.addons.valorant.Yoru;

import java.util.HashSet;
import java.util.Set;

public class CitizensManager {
    public static final Set<YoruClone> clones = new HashSet<>();

    public static void init() {
        CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(YoruClone.class));
    }

    public static void destroy() {
        for (YoruClone clone : clones) {
            clone.getNPC().destroy();
        }
    }

    public static void sendYoruClone(Yoru yoru, Location source, Vector direction) {
        NPC cloned = CitizensAPI.getTemporaryNPCRegistry().createNPC(EntityType.PLAYER, yoru.getSelf().getName());
        YoruClone trait = cloned.getOrAddTrait(YoruClone.class);
        trait.setYoru(yoru);
        trait.setDirection(direction);
        trait.setSourceLocation(source);
        clones.add(trait);

        Inventory inventory = cloned.getOrAddTrait(Inventory.class);
        inventory.setContents(yoru.getSelf().getInventory().getContents());

        cloned.spawn(source);
        cloned.setProtected(true);
    }

    public static class YoruClone extends Trait {
        private Yoru yoru;
        private boolean hit = false;
        private boolean set = false;
        private Vector direction;
        private Location source;

        protected YoruClone() {
            super("yoruclone");
        }

        private int hitTicks = 0;
        private int moveTicks = 0;

        @Override
        public void run() {
            if (!getNPC().isSpawned())
                return;

            if (hit) {
                if (hitTicks++ > 10) {
                    Location loc = getNPC().getStoredLocation();
                    yoru.flashOut(yoru.getSelf(), loc.add(0, 1, 0), getNPC().getEntity());

                    getNPC().destroy();
                    clones.remove(this);
                }
                return;
            }

            if (getNPC().getEntity() != null && !set) {
                getNPC().getNavigator().setStraightLineTarget(source.add(direction));
                set = true;
            }

            if (moveTicks++ > 20 * 10) {
                getNPC().destroy();
                clones.remove(this);
            }
        }

        public void setYoru(Yoru yoru) {
            this.yoru = yoru;
        }

        public void setDirection(Vector direction) {
            this.direction = direction.setY(0).normalize().multiply(50);
        }

        public void setSourceLocation(Location location) {
            this.source = location;
        }

        @EventHandler
        public void onHit(NPCDamageByEntityEvent e) {
            if (e.getNPC() != getNPC())
                return;

            e.setCancelled(true);
            Entity damager = e.getDamager();
            Location loc = damager.getLocation();
            getNPC().getNavigator().cancelNavigation(CancelReason.PLUGIN);
            getNPC().faceLocation(loc);
            hit = true;
        }
    }
}
