package shipmastery.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import shipmastery.ShipMastery;
import shipmastery.campaign.FleetHandler;
import shipmastery.util.CampaignUtils;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;
import shipmastery.util.VariantLookup;

// NPCs don't have enhances, so treat every NPC fleet as having 0 enhances.
public class SharedKnowledge {
    public static final float MAX_DP_REDUCTION = 0.075f;
    public static final int MAX_FIRE_RATE_STACKS = 5;
    public static final float MAX_FIRE_RATE = 0.075f;
    public static final float FIRE_RATE_RANGE = 2500f;
    public static final String FIRE_RATE_STACKS_MOD = "$sms_SharedKnowledgeStacksModifier";

    public static boolean hasSharedKnowledge(PersonAPI person) {
        if (person == null || person.getStats() == null) return false;
        return person.getStats().getSkillLevel("sms_shared_knowledge") >= 1f;
    }

    public static class Standard extends BaseSkillEffectDescription implements ShipSkillEffect {
        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            PersonAPI commander = CampaignUtils.getFleetCommanderForStats(stats);
            if (commander == null || commander.isDefault() || stats.getVariant() == null) return;
            var lookup = VariantLookup.getVariantInfo(stats.getVariant());
            var hullSpec = lookup == null || lookup.root == null ? stats.getVariant().getHullSpec() : lookup.root.getHullSpec();
            int maxLevel = ShipMastery.getMaxMasteryLevel(hullSpec);
            int curLevel;
            if (commander.isPlayer()) {
                curLevel = ShipMastery.getPlayerMasteryLevel(hullSpec);
            } else {
                var masteries = FleetHandler.getCachedNPCMasteries(commander, hullSpec);
                if (masteries == null || masteries.isEmpty()) return;
                curLevel =  masteries.lastKey();
            }

            float reduction = (float) curLevel / Math.max(maxLevel, 1) * MAX_DP_REDUCTION;
            if (reduction <= 0f) return;
            stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(id, 1f - reduction);
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).unmodify(id);
        }

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, TooltipMakerAPI info, float width) {
            init(stats, skill);
            // Needed because codex doesn't like \n character
            info.addPara(Strings.Skills.sharedKnowledgeStandardEffect1, 0f, hc, hc, Utils.asPercent(MAX_DP_REDUCTION));
            info.addPara(Strings.Skills.sharedKnowledgeStandardEffect2, 0f, tc);
        }
    }

    public static class Elite extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

        public static class SharedKnowledgeEliteScript implements AdvanceableListener {
            IntervalUtil checkerInterval = new IntervalUtil(0.5f, 1.5f);
            ShipAPI ship;
            String id;

            public SharedKnowledgeEliteScript(ShipAPI ship, String id) {
                this.ship = ship;
                this.id = id;
            }

            @Override
            public void advance(float amount) {
                if (!ship.isAlive() || ship.getHitpoints() < 0f) {
                    ship.removeListener(this);
                    return;
                }
                checkerInterval.advance(amount);
                if (checkerInterval.intervalElapsed()) {
                    float count = 1f; // Includes this ship itself, which has the skill
                    var it = Global.getCombatEngine().getShipGrid().getCheckIterator(
                            ship.getLocation(),
                            2f*(FIRE_RATE_RANGE+ship.getCollisionRadius()),
                            2f*(FIRE_RATE_RANGE+ship.getCollisionRadius()));
                    while (it.hasNext()) {
                        ShipAPI other = (ShipAPI) it.next();
                        if (ship == other) continue;
                        if (other.getOwner() != ship.getOwner()) continue;
                        if (!hasSharedKnowledge(other.getCaptain())) continue;
                        if (MathUtils.dist(other.getLocation(), ship.getLocation()) > FIRE_RATE_RANGE+ship.getCollisionRadius()+other.getCollisionRadius()) continue;
                        count++;
                    }

                    count = ship.getMutableStats().getDynamic().getMod(FIRE_RATE_STACKS_MOD).computeEffective(count);
                    float bonus = MAX_FIRE_RATE * Math.min(1f, count / MAX_FIRE_RATE_STACKS);
                    applyEffectsToStats(ship.getMutableStats(), bonus, id);
                }
            }
        }

        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {}

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {}

        public static void applyEffectsToStats(MutableShipStatsAPI stats, float amount, String modifierId) {
            stats.getBallisticRoFMult().modifyPercent(modifierId, 100f * amount);
            stats.getEnergyRoFMult().modifyPercent(modifierId, 100f * amount);
            stats.getMissileRoFMult().modifyPercent(modifierId, 100f * amount);
            stats.getBallisticWeaponFluxCostMod().modifyMult(modifierId, 1f / (1f + amount));
            stats.getEnergyWeaponFluxCostMod().modifyMult(modifierId, 1f / (1f + amount));
            stats.getMissileWeaponFluxCostMod().modifyMult(modifierId, 1f / (1f + amount));
        }

        @Override
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.addListener(new SharedKnowledgeEliteScript(ship, id));
        }

        @Override
        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.removeListenerOfClass(SharedKnowledgeEliteScript.class);
        }

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, TooltipMakerAPI info, float width) {
            initElite(stats, skill);
            // Need custom description due to elite effect taking multiple lines
            info.addPara(Strings.Skills.sharedKnowledgeEliteEffect1, 0f, hc, hc,
                    Utils.asPercent(MAX_FIRE_RATE),
                    skill.getName(),
                    Utils.asInt(FIRE_RATE_RANGE));
            info.addPara(Strings.Skills.sharedKnowledgeEliteEffect2, 0f, tc, hc, Utils.asInt(MAX_FIRE_RATE_STACKS));
            info.addPara(Strings.Skills.sharedKnowledgeEliteEffect3, 0f, tc);
            info.addPara(Strings.Skills.sharedKnowledgeEliteEffect4, 0f, tc);
        }
    }
}
