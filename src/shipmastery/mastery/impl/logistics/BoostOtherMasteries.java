package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.ShipMastery;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.MultiplicativeMasteryEffect;
import shipmastery.util.Strings;
public class BoostOtherMasteries extends MultiplicativeMasteryEffect {

    @Override
    public MasteryDescription getDescription(ShipHullSpecAPI spec) {
        return makeGenericDescription(
                Strings.BOOST_OTHER_MASTERIES,
                Strings.BOOST_OTHER_MASTERIES_NEG,
                true, false, getIncrease());
    }

    @Override
    public void onActivate(ShipHullSpecAPI spec, String id) {
        float mult = getMult();
        for (int i = 1; i <= ShipMastery.getMaxMastery(spec); i++) {
            for (MasteryEffect effect : ShipMastery.getMasteryEffects(spec, i)) {
                if (effect instanceof BoostOtherMasteries) continue;
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
    public void onDeactivate(ShipHullSpecAPI spec, String id) {
        for (int i = 1; i <= ShipMastery.getMaxMastery(spec); i++) {
            for (MasteryEffect effect : ShipMastery.getMasteryEffects(spec, i)) {
                effect.unmodifyStrength(id);
            }
        }
    }

    @Override
    public void addPostDescriptionSection(ShipHullSpecAPI spec, TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.BOOST_OTHER_MASTERIES_POST, 5f);
    }
}
