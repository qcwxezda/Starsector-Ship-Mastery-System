package shipmastery.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import shipmastery.combat.listeners.AdvanceIfAliveListener;
import shipmastery.fx.OverlayEmitter;
import shipmastery.util.CampaignUtils;
import shipmastery.util.MasteryUtils;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;
import shipmastery.util.VariantLookup;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SharedKnowledge {

    public static boolean hasSharedKnowledge(PersonAPI person) {
        if (person == null || person.getStats() == null) return false;
        return person.getStats().getSkillLevel("sms_shared_knowledge") >= 1f
                || person.getStats().getSkillLevel("sms_amorphous_knowledge") >= 1f;
    }

    public static class Standard extends SkillEffectDescriptionWIthNegativeHighlight implements ShipSkillEffect {

        public static final float DP_REDUCTION = 0.08f;
        public static final float DAMAGE_REDUCTION = 0.08f;

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

    public interface HiddenAICoreEffect {
        default float getCooldownSeconds(ShipAPI ship) {
            return 60f;
        }
        default float getDurationSeconds(ShipAPI ship) {
            return 6f;
        }
        default float getTimeMultBonus(ShipAPI ship) {
            return 0.75f;
        }
    }

    public static class Hidden extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

        public static class HiddenEffectScript extends AdvanceIfAliveListener implements PseudocoreHiddenSkillScript {
            public static final Color color = new Color(100, 200, 150);
            private final String id;
            private final HiddenAICoreEffect plugin;
            private final Map<ShipAPI, OverlayEmitter> emitterMap = new HashMap<>();
            private float cooldownRemaining = 0f;
            private boolean active = false;
            private float activeTime = 0f;
            private final FaderUtil effectFader = new FaderUtil(0f, 0.5f);
            private final IntervalUtil repopulateWingsAndModulesInterval = new IntervalUtil(0.5f, 0.5f);
            private List<ShipAPI> allModules = new ArrayList<>();
            private List<ShipAPI> allWings = new ArrayList<>();

            private OverlayEmitter getEmitterForShip(ShipAPI ship) {
                var existing = emitterMap.get(ship);
                if (existing != null) {
                    return existing;
                }
                var emitter = new OverlayEmitter(ship, ship.getSpriteAPI(), 0.8f);
                emitter.randomOffset = Math.min(ship.getSpriteAPI().getHeight(), ship.getSpriteAPI().getWidth()) / 7f;
                emitter.randomAngle = 20f;
                emitter.color = color;
                emitter.alphaMult = 0.2f;
                emitter.fadeInFrac = 0.2f;
                emitter.fadeOutFrac = 0.2f;
                emitter.enableDynamicAnchoring();
                emitterMap.put(ship, emitter);
                return emitter;
            }

            public HiddenEffectScript(ShipAPI ship, String id, HiddenAICoreEffect plugin) {
                super(ship);
                this.id = id;
                this.plugin = plugin;
                ship.setExplosionFlashColorOverride(new Color(150, 250, 200));
                resetCooldownTime();
            }

            @Override
            public void activate() {
                effectFader.fadeIn();
                active = true;
                activeTime = 0f;
                resetCooldownTime();
            }

            private void resetCooldownTime() {
                float cooldown = plugin.getCooldownSeconds(ship);
                cooldownRemaining = MathUtils.randBetween(0.8f*cooldown, 1.25f*cooldown);
            }

            private void applyEffects(float effectLevel, boolean shouldBurst) {
                allWings.forEach(wing -> {
                    if (!Global.getCombatEngine().isShipAlive(wing)) return;
                    if (effectLevel > 0f) {
                        wing.getMutableStats().getTimeMult().modifyMult(id, 1f + effectLevel);
                        wing.setCircularJitter(true);
                        wing.setJitterShields(true);
                        wing.setJitter(wing, color, effectFader.getBrightness(), 12, 10f);
                    } else {
                        wing.getMutableStats().getTimeMult().unmodify(id);
                    }
                });

                allModules.forEach(module -> {
                    if (!Global.getCombatEngine().isShipAlive(module)) return;
                    if (effectLevel > 0f) {
                        module.getMutableStats().getTimeMult().modifyMult(id, 1f + effectLevel);
                        if (shouldBurst && module.getSpriteAPI() != null) {
                            getEmitterForShip(module).burst(1);
                        }
                    } else {
                        module.getMutableStats().getTimeMult().unmodify(id);
                    }
                });
            }

            private void repopulateWingsAndModules() {
                allModules = Utils.getAllModules(ship);
                var wings = ship.getAllWings();
                allModules.forEach(module -> wings.addAll(module.getAllWings()));
                allWings = wings.stream()
                        .map(FighterWingAPI::getWingMembers)
                        .filter(Objects::nonNull)
                        .flatMap(List::stream)
                        .toList();
            }

            @Override
            public void advanceIfAlive(float amount) {
                float effectLevel = effectFader.getBrightness() * plugin.getTimeMultBonus(ship);
                applyEffects(effectLevel, Misc.random.nextFloat() < amount*9f);
                effectFader.advance(amount);

                repopulateWingsAndModulesInterval.advance(amount);
                if (repopulateWingsAndModulesInterval.intervalElapsed()) {
                    repopulateWingsAndModules();
                }

                if (!active) {
                    cooldownRemaining -= amount;
                    if (cooldownRemaining <= 0f) {
                        activate();
                    }
                } else {
                    activeTime += amount;
                    if (activeTime >= plugin.getDurationSeconds(ship)) {
                        active = false;
                        effectFader.fadeOut();
                    }
                }
            }
        }

        @Override
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            PersonAPI commander = ship.getFleetCommander();
            if (commander == null || (commander.isPlayer() && !Global.getSector().getMemoryWithoutUpdate().getBoolean(Strings.Campaign.PSEUDOCORE_AMP_INTEGRATED))) return;
            PersonAPI captain = ship.getCaptain();
            if (captain == null || !captain.isAICore()) return;
            var plugin = Misc.getAICoreOfficerPlugin(captain.getAICoreId());
            if (!(plugin instanceof HiddenAICoreEffect p)) return;
            VariantLookup.VariantInfo info = VariantLookup.getVariantInfo(ship.getVariant());
            if (info == null || info.root != info.variant) return;
            ship.addListener(new HiddenEffectScript(ship, id, p));
        }

        @Override
        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.removeListenerOfClass(HiddenEffectScript.class);
        }

        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {}

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {}
    }
}
