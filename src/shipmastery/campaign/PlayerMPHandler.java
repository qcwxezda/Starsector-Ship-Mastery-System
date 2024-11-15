package shipmastery.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
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
import shipmastery.ShipMastery;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.*;

public class PlayerMPHandler extends BaseCampaignEventListener implements EveryFrameScript {

    /** On average, amount of XP required for 50% chance of obtaining 1 MP
     *  Chance is x/(XP_PER_HALF_MP + x) to gain 1 MP, x is then reduced by XP_PER_MP and the chance is rolled again */
    public static final float XP_PER_HALF_MP = 3400f;
    public static final float XP_PER_HALF_MP_CIV = 4200f;
    /** Minimum XP required for a single action to be eligible to give MP to civilian ships. */
    public static final float MIN_XP_CIV = 750f;
    public static final float MULT_PER_MP = 1.175f;
    /** Ship hulls types at max mastery level have less probability of being picked for each MP they have over the max. */
    public static final float WEIGHT_MULT_PER_EXTRA_MP = 0.97f;
    private long prevXP;
    private long prevBonusXP;
    private int prevSP;
    private final Set<FleetMemberAPI> deployedInLastBattle = new HashSet<>();
    private boolean lastXPGainWasBattle = false;
    private final Random random = new Random(90706904117206L);

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
//        Parsing XP gain messages is technically more robust but doesn't handle translation packs
//        checkInterval.advance(amount);
//        if (checkInterval.intervalElapsed()) {
//            CampaignUIAPI ui = Global.getSector().getCampaignUI();
//            Object messageList = ReflectionUtils.invokeMethodNoCatch(ui, "getMessageList");
//            List<?> messages = (List<?>) ReflectionUtils.invokeMethodNoCatch(messageList, "getMessages");
//            for (Object message : messages) {
//                Object intel = ReflectionUtils.invokeMethodNoCatch(message, "getIntel");
//                List<?> lines = (List<?>) ReflectionUtils.getFieldWithClassNoCatch(MessageIntel.class, intel, "lines");
//                if (!lines.isEmpty()) {
//                    String text = (String) ReflectionUtils.getFieldWithClassNoCatch(MessageIntel.MessageLineData.class, lines.get(0), "text");
//                    String[] words = text.split("\\s+");
//                    if (words.length >= 3 && "Gained")
//                }
//            }

//        }
        MutableCharacterStatsAPI playerStats = Global.getSector().getPlayerStats();
        long curXP = playerStats.getXP();
        int curSP = playerStats.getStoryPoints();
        long curBonusXP = playerStats.getBonusXp();
        long xpGained = getXpGained(curXP, curBonusXP, curSP, playerStats);

        if (xpGained > 0) {
            if (!deployedInLastBattle.isEmpty()) {
                gainMPFromBattle(xpGained, deployedInLastBattle);
                deployedInLastBattle.clear();
            }
            // means auto-pursuit, treat as if all combat ships were deployed
            else if (lastXPGainWasBattle) {
                gainMPFromAutoPursuit(xpGained);
            }
            // not from battle, only consider civilian ships
            else if (xpGained >= MIN_XP_CIV) {
                gainMPFromOther(xpGained);
            }
            lastXPGainWasBattle = false;
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
                    * ((curXP > prevXP ? 0 : 1) + (curSP - prevSP) / plugin.getStoryPointsPerLevel());
        }
        else {
            xpGained = curXP - prevXP;
        }
        // Don't count bonus XP
        xpGained -= Math.max(0L, prevBonusXP - curBonusXP);
        return xpGained;
    }
    private WeightedRandomPicker<ShipHullSpecAPI> makePicker(
            Collection<FleetMemberAPI> toConsider,
            boolean allowCivilian,
            boolean allowCombat,
            boolean allowDuplicates) {
        WeightedRandomPicker<ShipHullSpecAPI> picker = new WeightedRandomPicker<>();
        picker.setRandom(random);
        Map<ShipHullSpecAPI, Integer> counts = new HashMap<>();
        for (FleetMemberAPI fm : toConsider) {
            ShipHullSpecAPI spec = Utils.getRestoredHullSpec(fm.getHullSpec());
            if (spec.isCivilianNonCarrier() && !allowCivilian) continue;
            if (!spec.isCivilianNonCarrier() && !allowCombat) continue;
            if (picker.getWeight(spec) > 0f && !allowDuplicates) continue;
            Integer count = counts.get(spec);
            if (count == null) count = 0;
            float weight = (float) Math.pow(2, -count);
            if (ShipMastery.getPlayerMasteryLevel(spec) >= ShipMastery.getMaxMasteryLevel(spec)) {
                weight *= (float) Math.pow(WEIGHT_MULT_PER_EXTRA_MP, ShipMastery.getPlayerMasteryPoints(spec));
            }
            picker.add(spec, weight);
            counts.put(spec, count + 1);
        }
        return picker;
    }

    public void gainMPFromBattle(long xpGained, Set<FleetMemberAPI> deployed) {
        WeightedRandomPicker<ShipHullSpecAPI> picker =
                makePicker(deployed, false, true, true);
        gainMP(xpGained, picker, false, false);
    }

    public void gainMPFromAutoPursuit(long xpGained) {
        List<FleetMemberAPI> members = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        WeightedRandomPicker<ShipHullSpecAPI> picker =
                makePicker(members, false, true, false);
        // 65% XP penalty for auto pursuits
        gainMP(0.35f * xpGained, picker, false, true);
    }

    public void gainMPFromOther(long xpGained) {
        List<FleetMemberAPI> members = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        WeightedRandomPicker<ShipHullSpecAPI> picker =
                makePicker(members, true, false, false);
        gainMP(xpGained, picker, true, false);
    }

    private void gainMP(float xp, WeightedRandomPicker<ShipHullSpecAPI> picker, boolean isCivilian, boolean isPursuit) {
        if (picker.isEmpty()) return;
        Map<ShipHullSpecAPI, Integer> amounts = new HashMap<>();
        float xpPer = isCivilian ? XP_PER_HALF_MP_CIV : XP_PER_HALF_MP;
        Set<ShipHullSpecAPI> uniques = new HashSet<>(picker.getItems());
        // Scale MP gains to number of ships deployed
        if (!isCivilian && !isPursuit) {
            if (uniques.size() == 1) xpPer *= 1.5f;
            else if (uniques.size() == 2) xpPer *= 1.3f;
            else if (uniques.size() == 3) xpPer *= 1.1f;
            else if (uniques.size() == 5) xpPer *= 0.9f;
            else if (uniques.size() == 6) xpPer *= 0.7f;
            else if (uniques.size() == 7) xpPer *= 0.6f;
            else if (uniques.size() >= 8) xpPer *= 0.5f;
        }
        float totalMPGained = 0f;
        while (xp > 0 && random.nextFloat() < xp / (xpPer + xp)) {
            totalMPGained++;
            xp -= xpPer;
            xpPer *= MULT_PER_MP;
        }
        totalMPGained *= Settings.MP_GAIN_MULTIPLIER;
        float fractionalPart = totalMPGained - (int) totalMPGained;
        if (random.nextFloat() < fractionalPart) {
            totalMPGained++;
        }
        for (int i = 0; i < totalMPGained; i++) {
            ShipHullSpecAPI spec = picker.pick();
            Integer amount = amounts.get(spec);
            amounts.put(spec, amount == null ? 1 : 1 + amount);
        }
        for (Map.Entry<ShipHullSpecAPI, Integer> entry : amounts.entrySet()) {
            ShipMastery.addPlayerMasteryPoints(entry.getKey(), entry.getValue());
        }
        showMasteryPointGainMessage(amounts);
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

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        EngagementResultForFleetAPI playerResult = result.getLoserResult().isPlayer() ? result.getLoserResult() : result.getWinnerResult();
        EngagementResultForFleetAPI enemyResult = result.getLoserResult().isPlayer() ? result.getWinnerResult() : result.getLoserResult();

        if (enemyResult.getDestroyed().isEmpty() && enemyResult.getDisabled().isEmpty()) return;

        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        if (dialog != null) {
            InteractionDialogPlugin plugin = dialog.getPlugin();
            if (plugin != null && plugin.getContext() instanceof FleetEncounterContext) {
                if (((FleetEncounterContext) plugin.getContext()).computePlayerContribFraction() <= 0f) return;
            }
        }

        lastXPGainWasBattle = true;
        // pursuit, no deployed data
        if (playerResult.getAllEverDeployedCopy() == null) return;
        Set<FleetMemberAPI> playerFleetMembers = new HashSet<>(Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy());
        for (DeployedFleetMemberAPI dfm : playerResult.getAllEverDeployedCopy()) {
            if (dfm.isFighterWing() || dfm.getMember() == null || !playerFleetMembers.contains(dfm.getMember())) continue;
            deployedInLastBattle.add(dfm.getMember());
        }
    }
}
