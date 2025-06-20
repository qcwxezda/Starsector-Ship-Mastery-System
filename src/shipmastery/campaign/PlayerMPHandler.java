package shipmastery.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.plugins.LevelupPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.apache.log4j.Logger;
import shipmastery.ShipMastery;
import shipmastery.achievements.LotsOfMP;
import shipmastery.achievements.UnlockAchievementAction;
import shipmastery.config.Settings;
import shipmastery.util.MathUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class PlayerMPHandler extends BaseCampaignEventListener implements EveryFrameScript {

    /** On average, amount of XP required for 50% chance of obtaining 1 MP
     *  Chance is x/(XP_PER_HALF_MP + x) to gain 1 MP, x is then reduced by XP_PER_MP and the chance is rolled again */
    public static final float XP_PER_HALF_MP = 3500f;
    public static final float XP_PER_HALF_MP_CIV = 1250f;
    /** Minimum XP required for a single action to be eligible to give MP to civilian ships. */
    public static final float MIN_XP_CIV = 500f;
    public static final float MULT_PER_MP = 1.035f;
    /** Ship hulls types at max mastery level have less probability of being picked for each MP they have over the max. */
    public static final float WEIGHT_MULT_PER_EXTRA_MP = 0.996f;
    public static final float MAX_DEPLOYMENT_TIME_TO_SCALE_MP = 120f;
    public static final float FULL_DEPLOYMENT_TIME_MULT = 0.2f;
    private long prevXP;
    private long prevBonusXP;
    private int prevSP;
    /** Fleet member -> how long they were deployed for in the last battle. */
    private final Map<FleetMemberAPI, Float> deployedTimeInLastBattle = new HashMap<>();
    private long computedLastBattleXPGain = 0;
    private final Random random = new Random(90706904117206L);
    public static final String DIFFICULTY_PROGRESSION_KEY = "$sms_DifficultyProgression";
    // Attached to player person
    public static final String COMBAT_MP_GAIN_STAT_MULT_KEY = "sms_MPGainMultCombat";
    public static final String CIVILIAN_MP_GAIN_STAT_MULT_KEY = "sms_MPGainMultCivilian";

    public PlayerMPHandler() {
        super(false);
        prevXP = Global.getSector().getPlayerPerson().getStats().getXP();
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        MutableCharacterStatsAPI playerStats = Global.getSector().getPlayerStats();
        long curXP = playerStats.getXP();
        int curSP = playerStats.getStoryPoints();
        long curBonusXP = playerStats.getBonusXp();
        long xpGained = getXpGained(curXP, curBonusXP, curSP, playerStats);

        // Battle XP handled in reportPlayerEngagement
        if (xpGained > MIN_XP_CIV) {
            gainMPFromOther(xpGained);
        }
        prevXP = curXP;
        prevBonusXP = curBonusXP;
        prevSP = curSP;
    }

    private long getXpGained(long curXP, long curBonusXP, int curSP, MutableCharacterStatsAPI playerStats) {
        long xpGained;
        // Known issue: if gained an exact multiple of 4,000,000 XP at max level, won't gain any MP.
        if (curXP == prevXP) return 0;
        LevelupPlugin plugin = Global.getSettings().getLevelupPlugin();
        int maxLevel = plugin.getMaxLevel();
        // Known issue: if >= storyPointsPerLevel SP gain is bundled with the XP gain,
        // will treat as if player gained enough XP to loop the max level that many times
        if (playerStats.getLevel() >= maxLevel) {
            xpGained = curXP - prevXP
                    + (plugin.getXPForLevel(maxLevel + 1) - plugin.getXPForLevel(maxLevel))
                    * ((curXP > prevXP ? 0 : 1) + (curSP - prevSP - (curXP > prevXP ? 0 : 1)) / plugin.getStoryPointsPerLevel());
        }
        else {
            xpGained = curXP - prevXP;
        }
        // Don't count bonus XP
        xpGained -= Math.max(0L, prevBonusXP - curBonusXP);
        return xpGained;
    }
    private WeightedRandomPicker<ShipHullSpecAPI> makePicker(
            Map<FleetMemberAPI, Float> deployedTime) {
        WeightedRandomPicker<ShipHullSpecAPI> picker = new WeightedRandomPicker<>();
        picker.setRandom(random);
        Map<ShipHullSpecAPI, Integer> counts = new HashMap<>();
        Map<ShipHullSpecAPI, Float> averageTimes = new HashMap<>();
        for (Map.Entry<FleetMemberAPI, Float> entry : deployedTime.entrySet()) {
            FleetMemberAPI fm = entry.getKey();
            ShipHullSpecAPI spec = Utils.getRestoredHullSpec(fm.getHullSpec());
            counts.compute(spec, (k, v) -> v == null ? 1 : v + 1);
            averageTimes.compute(spec, (k, v) -> v == null ? entry.getValue() : v + entry.getValue());
        }
        for (Map.Entry<ShipHullSpecAPI, Float> time : averageTimes.entrySet()) {
            ShipHullSpecAPI spec = time.getKey();
            int count = counts.get(spec);
            time.setValue(time.getValue() / count);
            float weight = time.getValue();
            weight *= 1f + Math.min(0.5f, 0.1f * (count-1));
            weight *= Math.max(0.5f, (float) Math.pow(WEIGHT_MULT_PER_EXTRA_MP, ShipMastery.getPlayerMasteryPoints(spec)));
            if (weight > 0f) {
                picker.add(spec, weight);
            }
        }
        return picker;
    }

    private WeightedRandomPicker<ShipHullSpecAPI> makePicker(
            Collection<FleetMemberAPI> toConsider,
            boolean allowCivilian,
            boolean allowCombat) {
        WeightedRandomPicker<ShipHullSpecAPI> picker = new WeightedRandomPicker<>();
        Map<ShipHullSpecAPI, Integer> counts = new HashMap<>();
        for (FleetMemberAPI fm : toConsider) {
            if (fm.isMothballed()) continue;
            ShipHullSpecAPI spec = Utils.getRestoredHullSpec(fm.getHullSpec());
            if (spec.isCivilianNonCarrier() && !allowCivilian) continue;
            if (!spec.isCivilianNonCarrier() && !allowCombat) continue;
            Integer count = counts.get(spec);
            if (count == null) count = 0;
            float weight = (float) Math.pow(2, -count);
            weight *= Math.max(0.5f, (float) Math.pow(WEIGHT_MULT_PER_EXTRA_MP, ShipMastery.getPlayerMasteryPoints(spec)));
            picker.add(spec, weight);
            counts.put(spec, count + 1);
        }
        return picker;
    }

    public void gainMPFromBattle(long xpGained, Map<FleetMemberAPI, Float> deployedTime) {
        WeightedRandomPicker<ShipHullSpecAPI> picker =
                makePicker(deployedTime);
        gainMP(xpGained, picker, false, false);
    }

    public void gainMPFromAutoPursuit(long xpGained) {
        List<FleetMemberAPI> members = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        WeightedRandomPicker<ShipHullSpecAPI> picker =
                makePicker(members, false, true);
        gainMP(0.6f*xpGained, picker, false, true);
    }

    public void gainMPFromOther(long xpGained) {
        List<FleetMemberAPI> members = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        WeightedRandomPicker<ShipHullSpecAPI> picker =
                makePicker(members, true, false);
        gainMP(xpGained, picker, true, false);
    }

    private void gainMP(float xp, WeightedRandomPicker<ShipHullSpecAPI> picker, boolean isCivilian, boolean isPursuit) {
        if (picker.isEmpty()) return;
        Map<ShipHullSpecAPI, Integer> amounts = new HashMap<>();
        float xpPer = isCivilian ? XP_PER_HALF_MP_CIV : XP_PER_HALF_MP;
        float xpPerMult = isPursuit ? 1f/3f : isCivilian ? 0.5f : 1f/8f;
        Set<ShipHullSpecAPI> uniques = new HashSet<>(picker.getItems());
        float totalMPGained = 0f;
        int count = 0;
        while (xp > 0 && (random.nextFloat() < xp / (xpPer*xpPerMult + xp) || xp > 5f * xpPer*xpPerMult)) {
            totalMPGained++;
            count++;
            xp -= xpPer*xpPerMult * MathUtils.randBetween(0.8f, 1.25f, random);
            xpPer *= isCivilian && totalMPGained >= 12 ? 2f : MULT_PER_MP;
            if (!isCivilian && !isPursuit) {
                xpPerMult = Math.min(1f/8f * (count+1), 1f);
            } else if (!isCivilian) {
                xpPerMult = count == 1 ? 2f/3f : 1f;
            } else {
                xpPerMult = 1f;
            }
        }
        var stats = Global.getSector().getPlayerStats().getDynamic();
        float additionalCivMult = stats.getStat(CIVILIAN_MP_GAIN_STAT_MULT_KEY).getModifiedValue();
        float additionalCombatMult = stats.getStat(COMBAT_MP_GAIN_STAT_MULT_KEY).getModifiedValue();
        totalMPGained *= (isCivilian ? Settings.CIVILIAN_MP_GAIN_MULTIPLIER * additionalCivMult
                : Settings.COMBAT_MP_GAIN_MULTIPLIER * additionalCombatMult);
        // Scale MP gains to number of ships deployed
        if (!isPursuit) {
            if (!isCivilian) {
                totalMPGained *= switch (uniques.size()) {
                    case 0 -> 0f;
                    case 1 -> 0.24f;
                    case 2 -> 0.45f;
                    case 3 -> 0.63f;
                    case 4 -> 0.79f;
                    case 5 -> 0.93f;
                    case 6 -> 1.04f;
                    case 7 -> 1.15f;
                    case 8 -> 1.23f;
                    case 9 -> 1.32f;
                    case 10 -> 1.39f;
                    case 11 -> 1.45f;
                    default -> 1.5f;
                };
            }
            else {
                totalMPGained *= switch (uniques.size()) {
                    case 1 -> 0.3f;
                    case 2 -> 0.55f;
                    case 3 -> 0.8f;
                    case 4 -> 1f;
                    default -> 1.2f;
                };
            }
        }
        float fractionalPart = totalMPGained - (int) totalMPGained;
        if (random.nextFloat() < fractionalPart) {
            totalMPGained++;
        }
        for (int i = 0; i < totalMPGained; i++) {
            ShipHullSpecAPI spec = picker.pick();
            amounts.compute(spec, (k, amount) -> amount == null ? 1 : 1 + amount);
        }
        for (Map.Entry<ShipHullSpecAPI, Integer> entry : amounts.entrySet()) {
            ShipMastery.addPlayerMasteryPoints(
                    entry.getKey(),
                    entry.getValue(),
                    true,
                    !isCivilian && !entry.getKey().isCivilianNonCarrier());
        }
        showMasteryPointGainMessage(amounts);

        // Check for achievement completion
        if (!isCivilian && totalMPGained >= LotsOfMP.NUM_NEEDED && Settings.COMBAT_MP_GAIN_MULTIPLIER <= 1.001f) {
            UnlockAchievementAction.unlockWhenUnpaused(LotsOfMP.class);
        }
    }

    public void showMasteryPointGainMessage(Map<ShipHullSpecAPI, Integer> amounts) {
        if (amounts.isEmpty()) return;
        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        String message;
        String highlight;
        if (amounts.size() == 1) {
            ShipHullSpecAPI spec = amounts.keySet().iterator().next();
            int amount = amounts.get(spec);
            message = String.format(Strings.Messages.gainedMPSingle, amount + " MP", spec.getHullNameWithDashClass());
            highlight = amount + " MP";
        }
        else {
            int sum = 0;
            for (int amount : amounts.values()) {
                sum += amount;
            }
            message = String.format(Strings.Messages.gainedMPMultiple, sum + " MP", amounts.size());
            highlight = sum + " MP";
        }
        Global.getSector().getCampaignUI().addMessage(message, Settings.MASTERY_COLOR);
        if (dialog != null) {
            dialog.getTextPanel().setFontSmallInsignia();
            dialog.getTextPanel().addPara(message, Misc.getTextColor(), Settings.MASTERY_COLOR, highlight);
            dialog.getTextPanel().setFontInsignia();
        }
    }

    public static void addTotalCombatMP(float amount) {
        Float previous = (Float) Global.getSector().getPersistentData().get(DIFFICULTY_PROGRESSION_KEY);
        float gain = amount / Math.max(1f, Settings.NPC_TOTAL_PROGRESSION_MP);
        float newAmount = previous == null ? gain : previous + gain;
        newAmount = MathUtils.clamp(newAmount, 0f, 1f);
        Global.getSector().getPersistentData().put(DIFFICULTY_PROGRESSION_KEY, newAmount);
    }

    /** Between 0 and 1 */
    public static float getDifficultyProgression() {
        Float progression = (Float) Global.getSector().getPersistentData().get(DIFFICULTY_PROGRESSION_KEY);
        return progression == null ? 0f : progression;
    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (battle == null || !battle.isPlayerInvolved()) return;

        long xpGained;
        try {
            var context = Global.getSector().getCampaignUI().getCurrentInteractionDialog().getPlugin().getContext();
            xpGained = (long) (float) ReflectionUtils.fleetEncounterContextXPGained.invoke(context);
        } catch (Throwable e) {
            Logger.getLogger(PlayerMPHandler.class).warn("Unable to get XP gained from fleet encounter context; falling back to custom computation");
            xpGained = computedLastBattleXPGain;
        }

        if (xpGained > 0) {
            if (deployedTimeInLastBattle.isEmpty()) {
                gainMPFromAutoPursuit(xpGained);
            } else {
                gainMPFromBattle(xpGained, deployedTimeInLastBattle);
            }
        }
        deployedTimeInLastBattle.clear();
        computedLastBattleXPGain = 0;
        updateXPValues();
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        EngagementResultForFleetAPI playerResult = result.getLoserResult().isPlayer() ? result.getLoserResult() : result.getWinnerResult();
        EngagementResultForFleetAPI enemyResult = result.getLoserResult().isPlayer() ? result.getWinnerResult() : result.getLoserResult();
        if (enemyResult.getDestroyed().isEmpty() && enemyResult.getDisabled().isEmpty()) return;

        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        if (dialog != null) {
            InteractionDialogPlugin plugin = dialog.getPlugin();
            if (plugin != null && plugin.getContext() instanceof FleetEncounterContextPlugin context) {
                float xpGained = 0f;
                var destroyed = new ArrayList<>(enemyResult.getDestroyed());
                destroyed.addAll(enemyResult.getDisabled());
                xpGained += (float) destroyed.stream().mapToDouble(fm -> 250f * fm.getFleetPointCost() * (1f + fm.getCaptain().getStats().getLevel() / 5f)).sum();

                float difficulty = (context instanceof FleetEncounterContext fContext) ? fContext.getDifficulty() : 1f;
                xpGained *= 2f * Math.max(1f, difficulty) * context.computePlayerContribFraction();
                computedLastBattleXPGain += (long) xpGained;
                // pursuit, no deployed data
                if (playerResult.getAllEverDeployedCopy() == null) {
                    return;
                }

                Set<FleetMemberAPI> playerFleetMembers = new HashSet<>(Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy());
                for (DeployedFleetMemberAPI dfm : playerResult.getAllEverDeployedCopy()) {
                    FleetMemberAPI fm = dfm.getMember();
                    if (dfm.isFighterWing() || fm == null || !playerFleetMembers.contains(fm) || dfm.getShip() == null) continue;
                    Float existingTime = deployedTimeInLastBattle.get(fm);
                    float newTime = dfm.getShip().getTimeDeployedForCRReduction();
                    // In case a ship doesn't lose CR for some reason
                    newTime += (dfm.getShip().getFullTimeDeployed() - newTime) * FULL_DEPLOYMENT_TIME_MULT;
                    float total = existingTime == null ? newTime : existingTime + newTime;
                    total = Math.min(total, MAX_DEPLOYMENT_TIME_TO_SCALE_MP);
                    deployedTimeInLastBattle.put(fm, total);
                }
            }
            updateXPValues();
        }
    }

    private void updateXPValues() {
        MutableCharacterStatsAPI playerStats = Global.getSector().getPlayerStats();
        long curXP = playerStats.getXP();
        int curSP = playerStats.getStoryPoints();
        long curBonusXP = playerStats.getBonusXp();
        prevXP = curXP;
        prevSP = curSP;
        prevBonusXP = curBonusXP;
    }
}
