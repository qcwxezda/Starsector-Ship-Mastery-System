package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MultiplicativeMasteryEffect;
import shipmastery.util.Strings;
public class ScaleOtherMasteries extends MultiplicativeMasteryEffect {

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return makeGenericDescription(
                Strings.Descriptions.ScaleOtherMasteries,
                Strings.Descriptions.ScaleOtherMasteriesNeg,
                true, false, getIncrease(null));
    }

    @Override
    public void onActivate(PersonAPI commander) {
        float mult = getMult((PersonAPI) null); // Always use base strength, this effect cannot be scaled
        if (mult >= 1f) {
            commander.getStats().getDynamic().getMod(MASTERY_STRENGTH_MOD_FOR + getHullSpec().getHullId()).modifyPercent(id, 100f * (mult - 1f));
        }
        else {
            commander.getStats().getDynamic().getMod(MASTERY_STRENGTH_MOD_FOR + getHullSpec().getHullId()).modifyMult(id, mult);
        }
//        float mult = getMultPlayer();
//        for (int i = 1; i <= ShipMastery.getMaxMasteryLevel(getHullSpec()); i++) {
//            for (MasteryEffect effect : ShipMastery.getMasteryEffectsBothOptions(getHullSpec(), i)) {
//                if (effect instanceof ScaleOtherMasteries) continue;
//                if (mult > 1) {
//                    effect.modifyStrengthAdditive(commander, mult, id);
//                }
//                else {
//                    effect.modifyStrengthMultiplicative(commander, mult, id);
//                }
//            }
//        }
    }

    @Override
    public void onDeactivate(PersonAPI commander) {
        commander.getStats().getDynamic().getMod(MASTERY_STRENGTH_MOD_FOR + getHullSpec().getHullId()).unmodify(id);
//        for (int i = 1; i <= ShipMastery.getMaxMasteryLevel(getHullSpec()); i++) {
//            for (MasteryEffect effect : ShipMastery.getMasteryEffectsBothOptions(getHullSpec(), i)) {
//                effect.unmodifyStrength(commander, id);
//            }
//        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.ScaleOtherMasteriesPost, 0f);
    }
}
