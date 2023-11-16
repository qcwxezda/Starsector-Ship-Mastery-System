package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import org.jetbrains.annotations.Nullable;
import shipmastery.campaign.listeners.RefitScreenShipChangedListener;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class ReduceRestorationCost extends BaseMasteryEffect implements RefitScreenShipChangedListener {
    @Override
    public MasteryDescription getDescription(ShipHullSpecAPI spec) {
        return Utils.makeGenericNegatableDescription(1f - getMult(), Strings.REDUCE_RESTORATION_COST, Strings.REDUCE_RESTORATION_COST_NEG, true);
    }

    @Override
    public void onBeginRefit(ShipHullSpecAPI spec, String id) {
        float mult = getMult();
        if (mult >= 1f) {
            TransientSettings.SHIP_RESTORE_COST_MULT.modifyPercent(id, 100f*mult);
        }
        else {
            TransientSettings.SHIP_RESTORE_COST_MULT.modifyMult(id, mult);
        }
    }

    @Override
    public void onEndRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.SHIP_RESTORE_COST_MULT.unmodify(id);
    }

    @Override
    public void onActivate(ShipHullSpecAPI spec, String id) {
        ListenerManagerAPI listenerManager = Global.getSector().getListenerManager();
        if (!listenerManager.hasListenerOfClass(ReduceRestorationCost.class)) {
            listenerManager.addListener(this, true);
        }
    }

    @Override
    public void onDeactivate(ShipHullSpecAPI spec, String id) {
        Global.getSector().getListenerManager().removeListener(this);
    }

    public float getMult() {
        return Math.max(0f, 1f - 0.1f * getStrength());
    }

    @Override
    public void onRefitScreenBeforeMasteriesChanged(@Nullable String oldHullSpecId, @Nullable String newHullSpecId) {}

    @Override
    public void onRefitScreenAfterMasteriesChanged(@Nullable String oldHullSpecId, @Nullable String newHullSpecId) {
        Global.getSettings().setFloat("baseRestoreCostMult", TransientSettings.SHIP_RESTORE_COST_MULT.getModifiedValue());
    }
}
