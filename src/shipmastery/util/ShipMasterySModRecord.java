package shipmastery.util;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.plog.SModRecord;

import java.lang.ref.WeakReference;
import java.util.Objects;

/** Same as SModRecord, only supports a single S-mod per record */
public class ShipMasterySModRecord extends SModRecord {

    protected float mpSpent = 0f;
    protected float creditsSpent = 0f;
    protected WeakReference<FleetMemberAPI> rootMember;
    protected String moduleId;

    public ShipMasterySModRecord(FleetMemberAPI member) {
        super(member);
        updateRootInfo(member);
    }

    @Override
    public void setMember(FleetMemberAPI member) {
        super.setMember(member);
        updateRootInfo(member);
    }

    protected void updateRootInfo(FleetMemberAPI member) {
        VariantLookup.VariantInfo info = VariantLookup.getVariantInfo(member.getVariant());
        if (info != null && !Objects.equals(info.uid, info.rootUid)) {
            rootMember = new WeakReference<>(info.rootMember);
            String moduleId = null;
            for (String id : info.root.getModuleSlots()) {
                if (Objects.equals(info.uid, VariantLookup.getVariantUID(info.root.getModuleVariant(id)))) {
                    moduleId = id;
                    break;
                }
            }
            this.moduleId = moduleId;
        }
        else {
            rootMember = new WeakReference<>(member);
            moduleId = null;
        }
    }

    public String getSMod() {
        return smods.get(0);
    }

    public FleetMemberAPI getRootMember() {
        return rootMember.get();
    }

    public String getModuleId() {
        return moduleId;
    }

    public void setMPSpent(float mpSpent) {
        this.mpSpent = mpSpent;
    }

    public float getMpSpent() {
        return mpSpent;
    }

    public float getCreditsSpent() {
        return creditsSpent;
    }

    public void setCreditsSpent(float creditsSpent) {
        this.creditsSpent = creditsSpent;
    }
}
