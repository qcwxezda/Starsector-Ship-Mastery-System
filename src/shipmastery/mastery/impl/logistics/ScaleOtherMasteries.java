package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.ShipMastery;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.MultiplicativeMasteryEffect;
import shipmastery.util.Strings;
public class ScaleOtherMasteries extends MultiplicativeMasteryEffect {

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return makeGenericDescription(
                Strings.Descriptions.ScaleOtherMasteries,
                Strings.Descriptions.ScaleOtherMasteriesNeg,
                true, false, getIncrease());
    }

    @Override
    public void onActivate(String id) {
        float mult = getMult();
        for (int i = 1; i <= ShipMastery.getMaxMastery(getHullSpec()); i++) {
            for (MasteryEffect effect : ShipMastery.getMasteryEffects(getHullSpec(), i)) {
                if (effect instanceof ScaleOtherMasteries) continue;
                if (mult > 1) {
                    effect.modifyStrengthAdditive(id, mult);
                }
                else {
                    effect.modifyStrengthMultiplicative(id, mult);
                }
            }
        }
    }

    @Override
    public void onDeactivate(String id) {
        for (int i = 1; i <= ShipMastery.getMaxMastery(getHullSpec()); i++) {
            for (MasteryEffect effect : ShipMastery.getMasteryEffects(getHullSpec(), i)) {
                effect.unmodifyStrength(id);
            }
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.ScaleOtherMasteriesPost, 5f);
    }
}
