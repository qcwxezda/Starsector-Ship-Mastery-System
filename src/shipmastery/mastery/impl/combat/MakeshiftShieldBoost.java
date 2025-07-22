package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class MakeshiftShieldBoost extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedVariant);
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.MakeshiftShieldBoost)
                .params(Global.getSettings().getHullModSpec(HullMods.MAKESHIFT_GENERATOR).getDisplayName(),
                        Utils.asPercent(strength),
                        Utils.asPercent(strength*0.2f));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        if (stats.getVariant() == null || !stats.getVariant().hasHullMod(HullMods.MAKESHIFT_GENERATOR)) return;
        float strength = getStrength(stats);
        stats.getShieldArcBonus().modifyPercent(id, 100f*strength);
        stats.getShieldDamageTakenMult().modifyMult(id, 1f - strength*0.2f);
        stats.getMaxSpeed().unmodify(HullMods.MAKESHIFT_GENERATOR);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (spec.getShieldType() != ShieldAPI.ShieldType.NONE) return null;
        return 1f;
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        if (fm.getVariant().hasHullMod(HullMods.MAKESHIFT_GENERATOR)) {
            return 3f*super.getNPCWeight(fm);
        }
        return 0f;
    }
}
