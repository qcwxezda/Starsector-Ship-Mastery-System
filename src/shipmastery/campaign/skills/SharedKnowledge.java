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
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.CampaignUtils;
import shipmastery.util.MasteryUtils;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;
import shipmastery.util.VariantLookup;

import java.awt.Color;

public class SharedKnowledge {

    public static boolean hasSharedKnowledge(PersonAPI person) {
        if (person == null || person.getStats() == null) return false;
        return person.getStats().getSkillLevel("sms_shared_knowledge") >= 1f
                || person.getStats().getSkillLevel("sms_amorphous_knowledge") >= 1f;
    }

    public static boolean hasEliteSharedKnowledge(PersonAPI person) {
        if (person == null || person.getStats() == null) return false;
        return person.getStats().getSkillLevel("sms_shared_knowledge") >= 2f
                || person.getStats().getSkillLevel("sms_amorphous_knowledge") >= 2f;
    }

    public static class Standard extends SkillEffectDescriptionWIthNegativeHighlight implements ShipSkillEffect {

        public static final float DP_REDUCTION = 0.08f;
        public static final float DAMAGE_REDUCTION = 0.05f;

        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            PersonAPI commander = CampaignUtils.getFleetCommanderForStats(stats);
            if (commander == null || commander.isDefault() || stats.getVariant() == null) return;
            var lookup = VariantLookup.getVariantInfo(stats.getVariant());
            var hullSpec = lookup == null || lookup.root == null ? stats.getVariant().getHullSpec() : lookup.root.getHullSpec();

            float reduction = MasteryUtils.getModifiedMasteryEffectStrength(commander, hullSpec, DP_REDUCTION);
            if (reduction > 0f) {
                stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(id, 1f - reduction);
            }
            stats.getEnergyWeaponDamageMult().modifyMult(id, 1f - DAMAGE_REDUCTION);
            stats.getBallisticWeaponDamageMult().modifyMult(id, 1f - DAMAGE_REDUCTION);
            stats.getMissileWeaponDamageMult().modifyMult(id, 1f - DAMAGE_REDUCTION);
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).unmodify(id);
            stats.getEnergyWeaponDamageMult().unmodify(id);
            stats.getBallisticWeaponDamageMult().unmodify(id);
            stats.getMissileWeaponDamageMult().unmodify(id);
        }

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, TooltipMakerAPI info, float width) {
            init(stats, skill);
            // Needed because codex doesn't like \n character
            info.addPara(Strings.Skills.sharedKnowledgeStandardEffect1, 0f, hc, hc, Utils.asPercent(DP_REDUCTION));
            info.addPara(Strings.Skills.sharedKnowledgeStandardEffect2, tc, 0f);
            info.addPara(Strings.Skills.sharedKnowledgeStandardEffect3, 0f, nhc, nhc, Utils.asPercent(DAMAGE_REDUCTION));
        }
    }

    public static class Elite extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

        public static final int MAX_FIRE_RATE_STACKS = 5;
        public static final float MAX_FIRE_RATE = 0.08f;
        public static final float FIRE_RATE_RANGE = 2500f;
        public static final String FIRE_RATE_STACKS_MOD = "$sms_SharedKnowledgeStacksModifier";

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
                    var commander = CampaignUtils.getFleetCommanderForStats(ship.getMutableStats());
                    bonus = MasteryUtils.getModifiedMasteryEffectStrength(commander, ship.getHullSpec(), bonus);
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
            info.addPara(Strings.Skills.sharedKnowledgeEliteEffect3, tc, 0f);
            info.addPara(Strings.Skills.sharedKnowledgeEliteEffect4, tc, 0f);
        }
    }

    public static class Hidden extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
        public static class EffectScript extends HiddenEffectScript {
            public EffectScript(ShipAPI ship, String id, Provider plugin) {
                super(ship, id, new Color(100, 200, 150), plugin);
            }

            @Override
            protected void applyEffectsToShip(ShipAPI ship, float effectLevel) {
                ship.getMutableStats().getTimeMult().modifyMult(id, 1f + effectLevel);
            }

            @Override
            protected void unapplyEffectsToShip(ShipAPI ship) {
                ship.getMutableStats().getTimeMult().unmodify(id);
            }
        }

        @Override
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            var p = getHiddenEffectPlugin(ship);
            if (p != null) {
                ship.addListener(new EffectScript(ship, id, p));
            }
        }

        @Override
        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.removeListenerOfClass(EffectScript.class);
        }

        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {}

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {}
    }

    public static HiddenEffectScript.Provider getHiddenEffectPlugin(ShipAPI ship) {
        PersonAPI commander = ship.getFleetCommander();
        if (commander == null || (commander.isPlayer() && !Global.getSector().getMemoryWithoutUpdate().getBoolean(Strings.Campaign.PSEUDOCORE_AMP_INTEGRATED))) return null;
        PersonAPI captain = ship.getCaptain();
        if (captain == null || !captain.isAICore()) return null;
        VariantLookup.VariantInfo info = VariantLookup.getVariantInfo(ship.getVariant());
        if (info == null || info.root != info.variant) return null;
        var plugin = Misc.getAICoreOfficerPlugin(ship.getCaptain().getAICoreId());
        return plugin instanceof HiddenEffectScript.Provider p ? p : null;
    }
}
