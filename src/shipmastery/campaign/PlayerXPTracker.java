package shipmastery.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.fs.starfarer.campaign.CampaignUIPersistentData;
import com.fs.starfarer.campaign.CharacterStats;
import shipmastery.ShipMastery;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.*;

public class PlayerXPTracker extends BaseCampaignEventListener implements EveryFrameScript {

    /** On average, amount of XP required for 50% chance of obtaining 1 MP
     *  Chance is x/(XP_PER_HALF_MP + x) to gain 1 MP, x is then reduced by XP_PER_MP and the chance is rolled again */
    public static final float XP_PER_HALF_MP = 25000f;
    private long prevXP;
    private final Set<FleetMemberAPI> deployedInLastBattle = new HashSet<>();
    private boolean lastXPGainWasBattle = false;

    public PlayerXPTracker(boolean permaRegister) {
        super(permaRegister);
        prevXP = Global.getSector().getPlayerPerson().getStats().getXP();
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        MutableCharacterStatsAPI playerStats = Global.getSector().getPlayerStats();
        CampaignUIPersistentData uiData = (CampaignUIPersistentData) Global.getSector().getUIData();
        long curXP = playerStats.getXP();
        if (curXP > prevXP) {
            long diff = curXP - prevXP;
            if (!deployedInLastBattle.isEmpty()) {
                gainMPFromBattle(diff, deployedInLastBattle);
                deployedInLastBattle.clear();
            }
            // means auto-pursuit, treat as if all combat ships were deployed
            else if (lastXPGainWasBattle) {
                gainMPFromAutoPursuit(diff);
            }
            // not from battle, only consider civilian ships
            else {
                gainMPFromOther(diff);
            }
            lastXPGainWasBattle = false;
        }
        // Technically hijacking this will cause the level-up flashing character tab to stop when max
        // level is reached
        else if (!((CampaignUIPersistentData) Global.getSector().getUIData()).isOpenedCharacterTabSinceLevelUp())
        prevXP = curXP;
    }

    public void gainMPFromBattle(long xpGained, Set<FleetMemberAPI> deployed) {
        WeightedRandomPicker<ShipHullSpecAPI> picker = new WeightedRandomPicker<>();
        for (FleetMemberAPI fm : deployed) {
            picker.add(Utils.getRestoredHullSpec(fm.getHullSpec()));
        }
        gainMP(xpGained, picker);
    }

    public void gainMPFromAutoPursuit(long xpGained) {
        List<FleetMemberAPI> members = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        WeightedRandomPicker<ShipHullSpecAPI> picker = new WeightedRandomPicker<>();
        for (FleetMemberAPI fm : members) {
            if (fm.isCivilian()) continue;
            picker.add(Utils.getRestoredHullSpec(fm.getHullSpec()));
        }
        gainMP(xpGained, picker);
    }

    public void gainMPFromOther(long xpGained) {
        List<FleetMemberAPI> members = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        WeightedRandomPicker<ShipHullSpecAPI> picker = new WeightedRandomPicker<>();
        for (FleetMemberAPI fm : members) {
            if (!fm.isCivilian()) continue;
            picker.add(Utils.getRestoredHullSpec(fm.getHullSpec()));
        }
        gainMP(xpGained, picker);
    }

    private void gainMP(float xp, WeightedRandomPicker<ShipHullSpecAPI> picker) {
        Map<ShipHullSpecAPI, Integer> amounts = new HashMap<>();
        while (xp > 0 && Misc.random.nextFloat() < xp / (XP_PER_HALF_MP + xp)) {
            ShipHullSpecAPI spec = picker.pick();
            Integer amount = amounts.get(spec);
            amounts.put(spec, amount == null ? 1 : 1 + amount);
            xp -= XP_PER_HALF_MP;
        }
        for (Map.Entry<ShipHullSpecAPI, Integer> entry : amounts.entrySet()) {
            ShipMastery.addPlayerMasteryPoints(entry.getKey(), entry.getValue());
        }
        showMasteryPointGainMessage(amounts);
    }

    public void showMasteryPointGainMessage(Map<ShipHullSpecAPI, Integer> amounts) {
        if (amounts.isEmpty()) return;
        if (amounts.size() == 1) {
            ShipHullSpecAPI spec = amounts.keySet().iterator().next();
            int amount = amounts.get(spec);
            Global.getSector().getCampaignUI().addMessage(
                String.format(Strings.gainedMPSingle, amount, spec.getHullNameWithDashClass()), Settings.MASTERY_COLOR);
        }
        else {
            int sum = 0;
            for (int amount : amounts.values()) {
                sum += amount;
            }
            Global.getSector().getCampaignUI().addMessage(
                    String.format(Strings.gainedMPMultiple, sum, amounts.size()), Settings.MASTERY_COLOR);
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
