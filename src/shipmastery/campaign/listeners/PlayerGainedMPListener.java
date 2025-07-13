package shipmastery.campaign.listeners;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.ShipMastery;

public interface PlayerGainedMPListener {
    /** Return the new amount of pts that should be gained. If multiple listeners are registered, no order is guaranteed.  */
    float modifyPlayerMPGain(ShipHullSpecAPI spec, float amount, ShipMastery.MasteryGainSource source);

    /** Occurs after all modifications have been made. */
    void reportPlayerMPGain(ShipHullSpecAPI spec, float amount, ShipMastery.MasteryGainSource source);
}
