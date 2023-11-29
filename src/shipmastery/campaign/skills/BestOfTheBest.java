package shipmastery.campaign.skills;

import com.fs.starfarer.api.characters.CharacterStatsSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class BestOfTheBest {

    public static float INCREASE_AMOUNT = 0.3f;

    public static class Level1 implements CharacterStatsSkillEffect {

        @Override
        public String getEffectDescription(float level) {
            return String.format(Strings.BEST_OF_THE_BEST_DESC, Utils.asPercent(INCREASE_AMOUNT));
        }

        @Override
        public String getEffectPerLevelDescription() {
            return null;
        }

        @Override
        public ScopeDescription getScopeDescription() {
            return ScopeDescription.ALL_SHIPS;
        }

        @Override
        public void apply(MutableCharacterStatsAPI stats, String id, float level) {
            stats.getDynamic().getMod(MasteryEffect.GLOBAL_MASTERY_STRENGTH_MOD).modifyPercent(id, 100f * INCREASE_AMOUNT);
//            if (stats.getFleet() == null) return;
//            for (FleetMemberAPI fm : Utils.getMembersNoSync(stats.getFleet())) {
//                ShipHullSpecAPI spec = Utils.getRestoredHullSpec(fm.getHullSpec());
//                for (int i = 1; i <= ShipMastery.getMaxMasteryLevel(spec); i++) {
//                    for (MasteryEffect effect : ShipMastery.getMasteryEffectsBothOptions(spec, i)) {
//                        effect.modifyStrengthAdditive(fm.getFleetCommander(), INCREASE_AMOUNT + 1f, id);
//                    }
//                }
//            }
        }

        @Override
        public void unapply(MutableCharacterStatsAPI stats, String id) {
            stats.getDynamic().getMod(MasteryEffect.GLOBAL_MASTERY_STRENGTH_MOD).unmodify(id);
//            if (stats.getFleet() == null) return;
//            for (FleetMemberAPI fm : Utils.getMembersNoSync(stats.getFleet())) {
//                ShipHullSpecAPI spec = Utils.getRestoredHullSpec(fm.getHullSpec());
//                for (int i = 1; i <= ShipMastery.getMaxMasteryLevel(spec); i++) {
//                    for (MasteryEffect effect : ShipMastery.getMasteryEffectsBothOptions(spec, i)) {
//                        effect.unmodifyStrength(fm.getFleetCommander(), id);
//                    }
//                }
//            }
        }
    }
}
