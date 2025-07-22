package shipmastery.hullmods;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillsChangeRemoveExcessOPEffect;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.aicoreinterface.AICoreInterfacePlugin;
import shipmastery.backgrounds.BackgroundUtils;
import shipmastery.backgrounds.RejectHumanity;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.MasteryTags;
import shipmastery.util.CampaignUtils;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Strings;
import shipmastery.util.VariantLookup;

import java.util.Objects;
import java.util.function.BiConsumer;

public class MasteryHullmod extends BaseHullMod {
    @Override
    public boolean affectsOPCosts() {
        return true;
    }

    private void applyAICoreInterfaceEffect(ShipVariantAPI variant, BiConsumer<String, AICoreInterfacePlugin> effect) {
        var id = AICoreInterfacePlugin.getIntegratedPseudocore(variant);
        var aiInterface = ShipMastery.getAICoreInterfacePlugin(id);
        if (aiInterface != null) {
            var modifyId = id + AICoreInterfacePlugin.INTEGRATED_SUFFIX;
            effect.accept(modifyId, aiInterface);
        }
    }

    public void applyPostEffectsBeforeShipCreation(MutableShipStatsAPI stats, String id) {
        ShipVariantAPI variant = stats.getVariant();
        // Add an S-mod slot if the logistics enhance bonus is active and the ship has at least one logistics hullmod
        // Deprecated
//        if (shouldApplyEffects(variant)) {
//            if (HullmodUtils.hasBonusLogisticSlot(variant) && HullmodUtils.hasLogisticSMod(variant)) {
//                stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, 1);
//            } else {
//                stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).unmodify(id);
//            }
//        }

        if (!BackgroundUtils.isTinkererStart()) {
            stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, -Misc.MAX_PERMA_MODS);
        }

        VariantLookup.VariantInfo info = VariantLookup.getVariantInfo(variant);
        if (info != null && info.commander != null && info.commander.isPlayer()) {
            // Penalize CR if the ship's OP is above the limit, for player ships only
            int maxOp = SkillsChangeRemoveExcessOPEffect.getMaxOP(variant.getHullSpec(), info.commander.getStats());
            int op = variant.computeOPCost(info.commander.getStats());
            if (op > maxOp) {
                float frac = (float) (op - maxOp) / maxOp;
                float penalty = Math.min(1f, frac * 100f * Settings.CR_PENALTY_PER_EXCESS_OP_PERCENT);
                if (penalty > 0f) {
                    stats.getMaxCombatReadiness().modifyFlat(id, -penalty, Strings.Misc.excessOP);
                }
            }
            // Penalize CR for reject humanity background if officered by human
            if (BackgroundUtils.isRejectHumanityStart()) {
                var captain = CampaignUtils.getCaptain(stats);
                if (captain != null && !captain.isPlayer() && !captain.isDefault() && !captain.isAICore()) {
                    stats.getMaxCombatReadiness().modifyFlat(
                            RejectHumanity.MODIFIER_ID,
                            -RejectHumanity.CREWED_CR_REDUCTION,
                            Strings.Backgrounds.rejectHumanityCRPenaltyDesc);
                }
            }
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        ShipVariantAPI variant = stats.getVariant();

        applyEffects(variant, (effect, commander, isModule) -> {
            if (!isModule || !effect.hasTag(MasteryTags.DOESNT_AFFECT_MODULES)) {
                effect.applyEffectsBeforeShipCreation(hullSize, stats);
                if (effect.hasTag(MasteryTags.TRIGGERS_AUTOFIT) && variant.getStatsForOpCosts() != null) {
                    effect.applyEffectsBeforeShipCreation(hullSize, variant.getStatsForOpCosts());
                }
                // For display purposes only
                FleetMemberAPI fm = stats.getFleetMember();
                PersonAPI captain = fm == null ? null : fm.getCaptain();
                if (commander != null && Objects.equals(commander, captain)) {
                    effect.onFlagshipStatusGained(commander, stats, null);
                }
            }
        }, (commander, isModule) -> {
            if (!isModule) {
                applyAICoreInterfaceEffect(variant,
                        (modifyId, plugin) -> plugin.applyEffectsBeforeShipCreation(hullSize, stats, modifyId));
            }
        });

        applyPostEffectsBeforeShipCreation(stats, id);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        applyEffects(ship.getVariant(), (effect, commander, isModule) -> {
            if (!isModule || !effect.hasTag(MasteryTags.DOESNT_AFFECT_MODULES)) {
                effect.applyEffectsAfterShipCreation(ship);
                // For display purposes only
                PersonAPI captain = ship.getCaptain();
                if (commander != null && Objects.equals(commander, captain)) {
                    effect.onFlagshipStatusGained(commander, ship.getMutableStats(), null);
                }
            }
        }, (commander, isModule) -> {
            if (!isModule) {
                applyAICoreInterfaceEffect(ship.getVariant(),
                        (modifyId, plugin) -> plugin.applyEffectsAfterShipCreation(ship, modifyId));
            }
        });

    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
        applyEffects(ship.getVariant(), (effect, commander, isModule) -> {
            if (!isModule || !effect.hasTag(MasteryTags.DOESNT_AFFECT_MODULES)) {
                effect.applyEffectsToFighterSpawnedByShip(fighter, ship);
            }
        }, (commander, isModule) -> {
            if (!isModule) {
                applyAICoreInterfaceEffect(ship.getVariant(),
                        (modifyId, plugin) -> plugin.applyEffectsToFighterSpawnedByShip(fighter, ship, modifyId));
            }
        });
    }

    // Extra safety against recursive calls not handled by forcing no-sync for fleet, i.e. in variant
    // .updateStatsForOpCosts, etc.
    boolean noRecurse = false;

    private void applyEffects(ShipVariantAPI variant, HullmodAction perEffectAction,
                              BiConsumer<PersonAPI, Boolean> afterEffectsAction) {
        if (variant == null || noRecurse) {
            return;
        }

        VariantLookup.VariantInfo info = VariantLookup.getVariantInfo(variant);
        ShipHullSpecAPI rootSpec = info == null ? variant.getHullSpec() : info.root.getHullSpec();
        boolean isModule = info != null && !Objects.equals(info.uid, info.rootUid);
        PersonAPI commander = info == null ? null : info.commander;
        CampaignFleetAPI fleet = info == null ? null : info.fleet;
        noRecurse = true;
        // Needed because getting masteries calls getFlagship, which updates stats, which calls
        // applyEffectsBeforeShipCreation, etc.
        boolean wasNoSync = false;
        if (fleet != null) {
            wasNoSync = fleet.getFleetData().isForceNoSync();
            fleet.getFleetData().setForceNoSync(true);
        }
        if (shouldApplyEffects(variant)) {
            MasteryUtils.applyAllActiveMasteryEffects(
                    commander, rootSpec, effect -> perEffectAction.perform(effect, commander, isModule));
        }
        afterEffectsAction.accept(commander, isModule);
        if (fleet != null) {
            fleet.getFleetData().setForceNoSync(wasNoSync);
        }
        noRecurse = false;
    }

    private boolean shouldApplyEffects(ShipVariantAPI variant) {
        return !Settings.DISABLE_MAIN_FEATURES && variant != null && !variant.hasHullMod(Strings.Hullmods.ENGINEERING_OVERRIDE);
    }

    private interface HullmodAction {
        void perform(MasteryEffect effect, PersonAPI commander, boolean isModule);
    }
}
