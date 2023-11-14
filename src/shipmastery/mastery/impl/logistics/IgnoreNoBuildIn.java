package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

import java.util.*;

public class IgnoreNoBuildIn extends BaseMasteryEffect {
    Set<String> hullmodIds = new TreeSet<>();
    @Override
    public void init(String... args) {
        super.init(args);
        hullmodIds.addAll(Arrays.asList(args).subList(1, args.length));
    }

    @Override
    public MasteryDescription getDescription() {
        return MasteryDescription.initDefaultHighlight(Strings.IGNORE_NO_BUILD_IN).params(makeString());
    }

    @Override
    public void applyEffectsOnBeginRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.IGNORE_NO_BUILD_IN_HULLMOD_IDS.addAll(hullmodIds);
    }

    @Override
    public boolean isUniqueEffect() {
        return true;
    }

    @Override
    public void unapplyEffectsOnEndRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.IGNORE_NO_BUILD_IN_HULLMOD_IDS.removeAll(hullmodIds);
    }

    @Override
    public boolean canBeDeactivated() {
        return false;
    }

    String makeString() {
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = hullmodIds.iterator();
        sb.append(Global.getSettings().getHullModSpec(itr.next()).getDisplayName());

        while (itr.hasNext()) {
            sb.append(", ").append(Global.getSettings().getHullModSpec(itr.next()).getDisplayName());
        }
        return sb.toString();
    }
}