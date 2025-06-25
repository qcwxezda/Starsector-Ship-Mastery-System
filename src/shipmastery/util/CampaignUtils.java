package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CampaignUtils {

    public static PersonAPI getFleetCommanderForStats(MutableShipStatsAPI stats) {
        var lookup = VariantLookup.getVariantInfo(stats.getVariant());
        if (lookup == null) return null;
        return lookup.commander;

//        if (stats == null) {
//            if (BaseSkillEffectDescription.isInCampaign()) {
//                return Global.getSector().getPlayerPerson();
//            }
//            return null;
//        }
//
//        FleetMemberAPI member = stats.getFleetMember();
//        if (member == null) return null;
//        PersonAPI commander = member.getFleetCommanderForStats();
//        if (commander == null) {
//            boolean orig = false;
//            if (member.getFleetData() != null) {
//                orig = member.getFleetData().isForceNoSync();
//                member.getFleetData().setForceNoSync(true);
//            }
//            commander = member.getFleetCommander();
//            if (member.getFleetData() != null) {
//                member.getFleetData().setForceNoSync(orig);
//            }
//        }
//        return commander;
    }

    public static PersonAPI getCaptain(MutableShipStatsAPI stats) {
        PersonAPI captain;
        if (stats.getEntity() instanceof ShipAPI ship) {
            captain = ship.getCaptain();
        } else {
            captain = stats.getFleetMember() == null ? null : stats.getFleetMember().getCaptain();
        }
        return captain;
    }

    public static NavigableMap<CommoditySpecAPI, Integer> getPlayerCommodityCounts(Function<CommoditySpecAPI, Boolean> filter) {
        return Global.getSector().getPlayerFleet().getCargo().getStacksCopy()
                .stream()
                .<Map.Entry<CommoditySpecAPI, Integer>>mapMulti((x, c) -> {
                    var id = x.getCommodityId();
                    if (id == null) return;
                    var spec = Global.getSettings().getCommoditySpec(id);
                    if (spec == null || !filter.apply(spec)) return;
                    c.accept(Map.entry(spec, (int) x.getSize()));
                })
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        () -> new TreeMap<>(
                                Comparator.comparingDouble(CommoditySpecAPI::getOrder)
                                        .thenComparing((x, y) -> CharSequence.compare(x.getId(), y.getId()))),
                        Collectors.summingInt(Map.Entry::getValue)));
    }
}
