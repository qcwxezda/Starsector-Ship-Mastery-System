package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

public class IgnoreNoBuildIn extends BaseMasteryEffect {
    final Set<String> hullmodIds = new TreeSet<>();
    @Override
    public MasteryEffect postInit(String... args) {
        hullmodIds.addAll(Arrays.asList(args).subList(1, args.length));
        return this;
    }

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        Object[] params = new Object[hullmodIds.size()];
        String str = makeString(params);
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.IgnoreNoBuildIn + str).params(params);
    }

    @Override
    public void onBeginRefit(ShipVariantAPI selectedVariant, boolean isModule) {
        TransientSettings.IGNORE_NO_BUILD_IN_HULLMOD_IDS.addAll(hullmodIds);
    }

    @Override
    public void onEndRefit(ShipVariantAPI selectedVariant, boolean isModule) {
        TransientSettings.IGNORE_NO_BUILD_IN_HULLMOD_IDS.removeAll(hullmodIds);
    }

    String makeString(Object[] params) {
        List<String> names = new ArrayList<>();
        int i = 0;
        for (String id : hullmodIds) {
            String name = Global.getSettings().getHullModSpec(id).getDisplayName();
            names.add(name);
            params[i] = name;
            i++;
        }
        return Utils.joinList(names) + ".";
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if ((ShipAPI.HullSize.CAPITAL_SHIP.equals(spec.getHullSize()) || spec.isBuiltInMod(HullMods.SAFETYOVERRIDES)) &&
                (!spec.isPhase() || spec.isBuiltInMod(HullMods.PHASE_ANCHOR)) &&
                (!spec.isBuiltInMod(HullMods.AUTOMATED) || spec.isBuiltInMod(HullMods.NEURAL_INTEGRATOR))) return null;
        return 0.75f;
    }

    @Override
    public List<String> generateRandomArgs(ShipHullSpecAPI spec, int maxTier, long seed) {
        WeightedRandomPicker<String> wrp = new WeightedRandomPicker<>();
        wrp.setRandom(new Random(seed));
        Set<String> seenArgs = new HashSet<>();
        for (String[] args : getAllUsedArgs()) {
            seenArgs.addAll(Arrays.asList(args).subList(1, args.length));
        }
        if (!seenArgs.contains(HullMods.SAFETYOVERRIDES) && !ShipAPI.HullSize.CAPITAL_SHIP.equals(spec.getHullSize()) && !spec.isBuiltInMod(HullMods.SAFETYOVERRIDES)) {
            wrp.add(HullMods.SAFETYOVERRIDES);
        }
        if (!seenArgs.contains(HullMods.PHASE_ANCHOR) && spec.isPhase() && !spec.isBuiltInMod(HullMods.PHASE_ANCHOR)) {
            wrp.add(HullMods.PHASE_ANCHOR);
        }
        if (wrp.isEmpty()) return null;
        return Collections.singletonList(wrp.pick());
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        return 0f; // Masteries are picked after autofit, can't go back and S-mod something after masteries have already been chosen
    }
}
