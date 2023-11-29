package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.HashMap;
import java.util.Map;

public class VariantLookup extends BaseCampaignEventListener {
    private final Map<String, VariantInfo> variantInfoMap = new HashMap<>();
    public static final String UID_TAG = "shipmastery_uid_";
    public static final String UID_INDICATOR_TAG = "shipmastery_has_uid";
    public static final String INSTANCE_KEY = "$shipmastery_CommanderLookup";

    private int nextId = 0;

    public VariantLookup(boolean permaRegister) {
        super(permaRegister);
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
        String id = UID_TAG + nextId++;
        variant.addTag(id);
        return id;
    }

    public static VariantLookup getInstance() {
        return (VariantLookup) Global.getSector().getMemoryWithoutUpdate().get(INSTANCE_KEY);
    }

    public static void addVariantInfo(ShipVariantAPI variant, ShipVariantAPI root, PersonAPI commander) {
        VariantLookup instance = getInstance();
        if (instance == null) return;
        instance.variantInfoMap.put(instance.getUIDGenerateIfNull(variant), new VariantInfo(instance, variant, root, commander));
    }

    public static VariantInfo getVariantInfo(ShipVariantAPI variant) {
        VariantLookup instance = getInstance();
        if (instance == null) return null;
        return instance.variantInfoMap.get(instance.getUID(variant));
    }

    public static class VariantInfo {
        public final ShipVariantAPI variant;
        public final String uid;

        /** Root module in the module tree */
        public final ShipVariantAPI root;
        public final String rootUid;

        public final PersonAPI commander;

        private VariantInfo(VariantLookup lookup, ShipVariantAPI variant, ShipVariantAPI root, PersonAPI commander) {
            this.variant = variant;
            uid = lookup.getUID(variant);
            this.root = root;
            rootUid = lookup.getUID(root);
            this.commander = commander;
        }
    }
}
