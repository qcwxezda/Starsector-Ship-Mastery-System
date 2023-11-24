package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.combat.MoteControlScript;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.Objects;

public class HighFrequencyMotes extends BaseMasteryEffect {

    public static float RANGE_REDUCTION = 0.6f;
    public static float MAX_MOTES_REDUCTION = 0.5f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.HighFrequencyMotes)
                                 .params(Strings.Descriptions.HighFrequencyMotesName);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.HighFrequencyMotesPost, 0f, Misc.getNegativeHighlightColor(), Utils.asPercent(RANGE_REDUCTION), Utils.asPercent(MAX_MOTES_REDUCTION));
    }

    @Override
    public void onFlagshipStatusGained(PersonAPI commander, MutableShipStatsAPI stats, @Nullable ShipAPI ship) {
        if (ship != null) {
            if (!Objects.equals("mote_control", ship.getHullSpec().getShipSystemId())) {
                return;
            }
            ship.getVariant().addMod(HullMods.HIGH_FREQUENCY_ATTRACTOR);
            ship.getMutableStats().getSystemRangeBonus().modifyMult(id, 1f - RANGE_REDUCTION);
            // Only works properly if only one commander has ziggurat(s). If multiple commanders have ziggurat(s) this
            // will incorrectly affect all of them.
            MoteControlScript.MOTE_DATA.get(MoteControlScript.MOTELAUNCHER_HF).maxMotes = (int) (MoteControlScript.MAX_MOTES_HF * (1f - MAX_MOTES_REDUCTION));
        }
    }

    @Override
    public void onFlagshipStatusLost(PersonAPI commander, MutableShipStatsAPI stats, @NotNull ShipAPI ship) {
        ship.getVariant().removeMod(HullMods.HIGH_FREQUENCY_ATTRACTOR);
        MoteControlScript.MOTE_DATA.get(MoteControlScript.MOTELAUNCHER_HF).maxMotes = MoteControlScript.MAX_MOTES_HF;
        ship.getMutableStats().getSystemRangeBonus().unmodify(id);
    }
}
