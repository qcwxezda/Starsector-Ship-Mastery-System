package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
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
                true, false, getIncreasePlayer());
    }

    @Override
    public void onActivate(PersonAPI commander) {
        float mult = getMultPlayer();
        for (int i = 1; i <= ShipMastery.getPlayerMaxMastery(getHullSpec()); i++) {
            for (MasteryEffect effect : ShipMastery.getMasteryEffectsBothOptions(getHullSpec(), i)) {
                if (effect instanceof ScaleOtherMasteries) continue;
                if (mult > 1) {
                    effect.modifyStrengthAdditive(Global.getSector().getPlayerPerson(), mult, id);
                }
                else {
                    effect.modifyStrengthMultiplicative(commander, mult, id);
                }
            }
        }
    }

    @Override
    public void onDeactivate(PersonAPI commander) {
        for (int i = 1; i <= ShipMastery.getPlayerMaxMastery(getHullSpec()); i++) {
            for (MasteryEffect effect : ShipMastery.getMasteryEffectsBothOptions(getHullSpec(), i)) {
                effect.unmodifyStrength(commander, id);
            }
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.ScaleOtherMasteriesPost, 5f);
    }
}
