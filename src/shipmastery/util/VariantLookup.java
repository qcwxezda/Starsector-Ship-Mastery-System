package shipmastery.util;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.HashMap;
import java.util.Map;

public class VariantLookup extends BaseCampaignEventListener {
    private final Map<String, VariantInfo> variantInfoMap = new HashMap<>();
    public static final String UID_TAG = "shipmastery_uid_";
    public static final String UID_INDICATOR_TAG = "shipmastery_has_uid";
    private static VariantLookup instance;

    public VariantLookup() {
        super(false);
        instance = this;
    }

    @Override
    public void reportFleetDespawned(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
        for (FleetMemberAPI fm : Utils.getMembersNoSync(fleet)) {
            untrackVariant(fm.getVariant());
        }
    }

    private void untrackVariant(ShipVariantAPI variant) {
        variantInfoMap.remove(getUID(variant));
        for (String id : variant.getModuleSlots()) {
            untrackVariant(variant.getModuleVariant(id));
        }
    }

    private String getUID(ShipVariantAPI variant) {
        if (variant == null) return null;
        if (!variant.hasTag(UID_INDICATOR_TAG)) {
            return null;
        }
        for (String tag : variant.getTags()) {
            if (tag.startsWith(UID_TAG)) {
                return tag;
            }
        }
        return null;
    }

    private String getUIDGenerateIfNull(ShipVariantAPI variant) {
        if (!variant.hasTag(UID_INDICATOR_TAG)) {
            return generateUID(variant);
        }
        for (String tag : variant.getTags()) {
            if (tag.startsWith(UID_TAG)) {
                return tag;
            }
        }
        return generateUID(variant);
    }

    private String generateUID(ShipVariantAPI variant) {
        variant.addTag(UID_INDICATOR_TAG);
        String id = UID_TAG + Misc.genUID();
        variant.addTag(id);
        return id;
    }

    public static VariantLookup getInstance() {
        return instance;
    }

    public static void addVariantInfo(ShipVariantAPI variant, ShipVariantAPI root, FleetMemberAPI member) {
        VariantLookup instance = getInstance();
        if (instance == null) return;
        instance.variantInfoMap.put(instance.getUIDGenerateIfNull(variant), new VariantInfo(instance, variant, root, member));
    }

    public static VariantInfo getVariantInfo(ShipVariantAPI variant) {
        if (variant == null) return null;
        VariantLookup instance = getInstance();
        if (instance == null) return null;
        return instance.variantInfoMap.get(instance.getUID(variant));
    }

    public static String getVariantUID(ShipVariantAPI variant) {
        VariantLookup instance = getInstance();
        if (instance == null) return null;
        VariantInfo info = instance.variantInfoMap.get(instance.getUID(variant));
        if (info == null) return null;
        return info.uid;
    }

    public static class VariantInfo {
        public final ShipVariantAPI variant;
        public final String uid;

        /** Root module in the module tree */
        public final ShipVariantAPI root;
        public final FleetMemberAPI rootMember;
        public final String rootUid;

        public final PersonAPI commander;
        public final CampaignFleetAPI fleet;

        private VariantInfo(
                VariantLookup lookup,
                ShipVariantAPI variant,
                ShipVariantAPI root,
                FleetMemberAPI member) {
            this.variant = variant;
            uid = lookup.getUID(variant);
            this.root = root;
            rootUid = lookup.getUID(root);
            rootMember = member;
            var commanderTemp = member.getFleetCommanderForStats();
            if (commanderTemp == null) {
                commanderTemp = member.getFleetCommander();
            }
            this.commander = commanderTemp;
            this.fleet = member.getFleetData() == null ? null : member.getFleetData().getFleet();
        }
    }
}
