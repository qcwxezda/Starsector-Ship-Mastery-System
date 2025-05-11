package shipmastery.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.HullModFleetEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.IntervalUtil;
import shipmastery.deferred.CombatDeferredActionPlugin;
import shipmastery.fx.OverlayEmitter;
import shipmastery.util.Strings;

import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class CuratorNPCHullmod extends BaseHullMod implements HullModFleetEffect {

    public static final String CUSTOM_DATA_KEY = "sms_FightersList";

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getMinCrewMod().modifyMult(id, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new CuratorNPCHullmodScript(ship, id));
    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
        //noinspection unchecked
        List<ShipAPI> fighterList = ship.getCustomData() == null ? null : (List<ShipAPI>) ship.getCustomData().get(CUSTOM_DATA_KEY);
        if (fighterList == null) {
            fighterList = new LinkedList<>();
            fighterList.add(fighter);
            ship.setCustomData(CUSTOM_DATA_KEY, fighterList);
        }
        fighterList.add(fighter);
    }

    public static class CuratorNPCHullmodScript implements AdvanceableListener {

        public static final float DURATION = 3f; // In normal time, not accelerated time
        private final IntervalUtil interval;
        private final ShipAPI ship;
        private final String id;
        private final boolean enabled;
        private final OverlayEmitter emitter;
        private final FaderUtil effectFader = new FaderUtil(0f, 1f);

        private OverlayEmitter makeEmitter(ShipAPI ship) {
            var emitter = new OverlayEmitter(ship, ship.getSpriteAPI(), DURATION / 2f);
            emitter.randomOffset = Math.min(ship.getSpriteAPI().getHeight(), ship.getSpriteAPI().getWidth()) / 15f;
            emitter.randomAngle = 10f;
            emitter.color = new Color(100, 200, 150);
            emitter.alphaMult = 0.3f;
            emitter.fadeInFrac = 0.1f;
            emitter.fadeOutFrac = 0.1f;
            emitter.enableDynamicAnchoring();
            return emitter;
        }

        public CuratorNPCHullmodScript(ShipAPI ship, String id) {
            this.ship = ship;
            this.id = id;
            if (ship.getSpriteAPI() == null) {
                emitter = null;
            } else {
                emitter = makeEmitter(ship);
            }

            float minIntervalTime;
            if (ship.getCaptain() != null && ship.getCaptain().getAICoreId() != null) {
                minIntervalTime = switch (ship.getCaptain().getAICoreId()) {
                    case "sms_fractured_gamma_core" -> 60f;
                    case "sms_gamma_k_core" -> 45f;
                    case "sms_beta_k_core" -> 35f;
                    case "sms_alpha_k_core" -> 25f;
                    case "sms_amorphous_core" -> 15f;
                    default -> 999999999f;
                };
            } else {
                minIntervalTime = 999999999f;
            }
            enabled = minIntervalTime < 100f;
            if (enabled) {
                ship.setExplosionFlashColorOverride(new Color(150, 250, 200));
            }
            interval = new IntervalUtil(minIntervalTime, 1.2f*minIntervalTime);
        }

        @Override
        public void advance(float amount) {
            var engine = Global.getCombatEngine();
            if (!engine.isShipAlive(ship)) {
                ship.removeListener(this);
                return;
            }

            if (!enabled) return;
            effectFader.advance(amount);
            //noinspection unchecked
            List<ShipAPI> fighterList = ship.getCustomData() == null ? null : (List<ShipAPI>) ship.getCustomData().get(CUSTOM_DATA_KEY);
            if (fighterList == null) fighterList = new LinkedList<>();

            float effectLevel = effectFader.getBrightness();
            if (effectLevel > 0f) {
                ship.getMutableStats().getTimeMult().modifyMult(id, 1f + effectLevel);
            } else {
                ship.getMutableStats().getTimeMult().unmodify(id);
            }
            for (Iterator<ShipAPI> iterator = fighterList.iterator(); iterator.hasNext(); ) {
                ShipAPI fighter = iterator.next();
                if (!engine.isShipAlive(fighter)) {
                    iterator.remove();
                    continue;
                }
                if (effectLevel > 0f) {
                    fighter.getMutableStats().getTimeMult().modifyMult(id, 1f + effectLevel);
                }
                else {
                    fighter.getMutableStats().getTimeMult().unmodify(id);
                }
            }

            interval.advance(amount);
            if (interval.intervalElapsed()) {
                effectFader.fadeIn();
                emitter.stream(1, 4f, DURATION, e -> engine.isShipAlive(ship));
                CombatDeferredActionPlugin.performLater(effectFader::fadeOut, DURATION);

                for (ShipAPI fighter : fighterList) {
                    OverlayEmitter emitter = makeEmitter(fighter);
                    emitter.stream(1, 4f, DURATION, e -> engine.isShipAlive(ship));
                }
            }
        }
    }

    @Override
    public void advanceInCampaign(CampaignFleetAPI fleet) {
    }

    @Override
    public boolean withAdvanceInCampaign() {
        return false;
    }

    @Override
    public boolean withOnFleetSync() {
        return true;
    }

    @Override
    public void onFleetSync(CampaignFleetAPI fleet) {
        if (!fleet.isPlayerFleet()) return;
        boolean active = Global.getSector().getMemoryWithoutUpdate().getBoolean(Strings.Campaign.K_CORE_AMP_INTEGRATED);
        for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
            boolean remove = !active;
            remove |= fm.getCaptain() == null || !fm.getCaptain().isAICore() || fm.getCaptain().getAICoreId() == null;

            if (!remove) {
                var spec = Global.getSettings().getCommoditySpec(fm.getCaptain().getAICoreId());
                remove = spec == null || !spec.hasTag("sms_k_core");
            }

            if (remove) {
                if (fm.getVariant().hasHullMod("sms_curator_npc_hullmod")) {
                    fm.getVariant().removePermaMod("sms_curator_npc_hullmod");
                }
            } else {
                if (!fm.getVariant().hasHullMod("sms_curator_npc_hullmod")) {
                    fm.getVariant().addPermaMod("sms_curator_npc_hullmod");
                }
            }
        }
    }
}
