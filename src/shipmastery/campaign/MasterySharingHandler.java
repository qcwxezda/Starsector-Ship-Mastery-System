package shipmastery.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.ShipMastery;
import shipmastery.achievements.BlankConstructMade;
import shipmastery.achievements.UnlockAchievementAction;
import shipmastery.campaign.items.KnowledgeConstructPlugin;
import shipmastery.campaign.listeners.PlayerGainedMPListener;
import shipmastery.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class MasterySharingHandler implements PlayerGainedMPListener {

    public static final float SHARED_MASTERY_MP_GAIN = 100f;
    public static final float SHARED_MASTERY_MP_REQ = 300f;
    public static final float SHARED_MASTERY_MP_MULT = 0.5f;

    public static final String MASTERY_SHARING_DATA_KEY = "$sms_MasterySharingData";

    public static final class MasterySharingData {
        public boolean isActive;
        public float currentMP;

        public MasterySharingData(boolean isActive, float currentMP) {
            this.isActive = isActive;
            this.currentMP = currentMP;
        }
    }

    public static void modifyMasterySharingStatus(ShipHullSpecAPI spec, boolean activate) {
        String id = Utils.getRestoredHullSpecId(spec);
        // noinspection unchecked
        var map = (Map<String, MasterySharingData>) Global.getSector().getPersistentData().computeIfAbsent(MASTERY_SHARING_DATA_KEY, k -> new HashMap<>());
        MasterySharingData data = map.computeIfAbsent(id, k -> new MasterySharingData(false, 0f));
        data.isActive = activate;
    }

    public static boolean isMasterySharingActive(ShipHullSpecAPI spec) {
        String id = Utils.getRestoredHullSpecId(spec);
        //noinspection unchecked
        var map = (Map<String, MasterySharingData>) Global.getSector().getPersistentData().getOrDefault(MASTERY_SHARING_DATA_KEY, new HashMap<>());
        return map.getOrDefault(id, new MasterySharingData(false, 0f)).isActive;
    }

    public static float getCurrentMasterySharingMP(ShipHullSpecAPI spec) {
        String id = Utils.getRestoredHullSpecId(spec);
        //noinspection unchecked
        var map = (Map<String, MasterySharingData>) Global.getSector().getPersistentData().getOrDefault(MASTERY_SHARING_DATA_KEY, new HashMap<>());
        return map.getOrDefault(id, new MasterySharingData(false, 0f)).currentMP;
    }

    @Override
    public float modifyPlayerMPGain(ShipHullSpecAPI spec, float amount, ShipMastery.MasteryGainSource source) {
        if (source == ShipMastery.MasteryGainSource.ITEM) return amount;
        if (!isMasterySharingActive(spec)) return amount;
        return SHARED_MASTERY_MP_MULT * amount;
    }

    @Override
    public void reportPlayerMPGain(ShipHullSpecAPI spec, float amount, ShipMastery.MasteryGainSource source) {
        if (source == ShipMastery.MasteryGainSource.ITEM) return;
        if (!isMasterySharingActive(spec)) return;
        String id = Utils.getRestoredHullSpecId(spec);
        // noinspection unchecked
        var map = (Map<String, MasterySharingData>) Global.getSector().getPersistentData().computeIfAbsent(MASTERY_SHARING_DATA_KEY, k -> new HashMap<>());
        var data = map.computeIfAbsent(id, k -> new MasterySharingData(false, 0f));
        data.currentMP += amount;
        if (data.currentMP >= SHARED_MASTERY_MP_REQ) {
            int gained = (int) (data.currentMP / SHARED_MASTERY_MP_REQ);
            data.currentMP %= SHARED_MASTERY_MP_REQ;
            Global.getSector().getPlayerFleet().getCargo().addSpecial(
                    new SpecialItemData("sms_construct", KnowledgeConstructPlugin.PLAYER_CREATED_PREFIX), gained);
            UnlockAchievementAction.unlockWhenUnpaused(BlankConstructMade.class);
        }
    }
}
