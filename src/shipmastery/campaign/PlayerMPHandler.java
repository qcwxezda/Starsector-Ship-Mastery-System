package shipmastery.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
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
    public static final float XP_PER_HALF_MP = 10000f;
    public static final float XP_PER_HALF_MP_CIV = 4000f;
    public static final float MULT_PER_MP = 1.1f;
    /** Ship hulls types at max mastery level start receiving fewer MP for each MP they have over the max. */
    public static final float WEIGHT_MULT_PER_EXTRA_MP = 0.9f;
    private long prevXP;
    private int prevSP;
    private final Set<FleetMemberAPI> deployedInLastBattle = new HashSet<>();
    private boolean lastXPGainWasBattle = false;

    public PlayerMPHandler(boolean permaRegister) {
        super(permaRegister);
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
        long xpGained = getXpGained(curXP, curSP, playerStats);

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
            else {
                gainMPFromOther(xpGained);
            }
            lastXPGainWasBattle = false;
        }
        prevXP = curXP;
        prevSP = curSP;
    }

    private long getXpGained(long curXP, int curSP, MutableCharacterStatsAPI playerStats) {
        long xpGained;
        // Known issue: if gained an exact multiple of 4,000,000 XP at max level, won't gain any SP.
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
        return xpGained;
    }
    private WeightedRandomPicker<ShipHullSpecAPI> makePicker(
            Collection<FleetMemberAPI> toConsider,
            boolean allowCivilian,
            boolean allowCombat,
            boolean allowDuplicates) {
        WeightedRandomPicker<ShipHullSpecAPI> picker = new WeightedRandomPicker<>();
        picker.setRandom(Misc.random);
        Map<ShipHullSpecAPI, Integer> counts = new HashMap<>();
        for (FleetMemberAPI fm : toConsider) {
            if (fm.isCivilian() && !allowCivilian) continue;
            if (!fm.isCivilian() && !allowCombat) continue;
            ShipHullSpecAPI spec = Utils.getRestoredHullSpec(fm.getHullSpec());
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
        gainMP(xpGained, picker, false);
    }

    public void gainMPFromAutoPursuit(long xpGained) {
        List<FleetMemberAPI> members = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        WeightedRandomPicker<ShipHullSpecAPI> picker =
                makePicker(members, false, true, false);
        gainMP(xpGained, picker, false);
    }

    public void gainMPFromOther(long xpGained) {
        List<FleetMemberAPI> members = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        WeightedRandomPicker<ShipHullSpecAPI> picker =
                makePicker(members, true, false, false);
        gainMP(xpGained, picker, true);
    }

    private void gainMP(float xp, WeightedRandomPicker<ShipHullSpecAPI> picker, boolean isCivilian) {
        Map<ShipHullSpecAPI, Integer> amounts = new HashMap<>();
        float xpPer = isCivilian ? XP_PER_HALF_MP_CIV : XP_PER_HALF_MP;
        while (xp > 0 && Misc.random.nextFloat() < xp / (xpPer + xp)) {
            ShipHullSpecAPI spec = picker.pick();
            Integer amount = amounts.get(spec);
            amounts.put(spec, amount == null ? 1 : 1 + amount);
            xp -= xpPer;
            xpPer *= MULT_PER_MP;
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
            message = String.format(Strings.GAINED_MP_SINGLE, amount + " MP", spec.getHullNameWithDashClass());
            highlight = amount + " MP";
        }
        else {
            int sum = 0;
            for (int amount : amounts.values()) {
                sum += amount;
            }
            message = String.format(Strings.GAINED_MP_MULTIPLE, sum + " MP", amounts.size());
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
        lastXPGainWasBattle = true;
        // pursuit, no deployed data
        if (playerResult.getAllEverDeployedCopy() == null) return;
        for (DeployedFleetMemberAPI dfm : playerResult.getAllEverDeployedCopy()) {
            if (!dfm.isFighterWing()) {
                deployedInLastBattle.add(dfm.getMember());
            }
        }
    }
}
