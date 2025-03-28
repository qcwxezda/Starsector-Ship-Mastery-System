package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.plog.PlaythroughLog;
import com.fs.starfarer.api.impl.campaign.plog.SModRecord;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.config.Settings;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.ShipMasterySModRecord;
import shipmastery.util.Strings;
import shipmastery.util.Utils;
import shipmastery.util.VariantLookup;

import java.util.*;

public class ClearSModsPressed extends ActionListener {

    final MasteryPanel masteryPanel;
    final ShipAPI module;
    final ShipAPI root;
    final String defaultText;

    public ClearSModsPressed(MasteryPanel masteryPanel, ShipAPI module, ShipAPI root, String defaultText) {
        this.masteryPanel = masteryPanel;
        this.module = module;
        this.root = root;
        this.defaultText = defaultText;
    }

    @Override
    public void trigger(Object... args) {
        final ButtonAPI button = (ButtonAPI) args[1];
        ShipVariantAPI variant = module.getVariant();
        if (isConfirming(button)) {
            endConfirm(button);

            int removedCount = 0;
            // Copy required as removePermaMod also calls getSMods().remove()
            Set<String> removedIds = new HashSet<>();
            for (String id : new ArrayList<>(variant.getSMods())) {
                variant.removePermaMod(id);
                removedIds.add(id);
                removedCount++;
            }

            if (removedCount == 0) {
                return;
            }

            List<ShipMasterySModRecord> toRemove = new ArrayList<>();
            List<SModRecord> records = PlaythroughLog.getInstance().getSModsInstalled();
            float mpSpent = 0f, creditsSpent = 0f;

            String moduleId = null;
            String moduleVariantUID = VariantLookup.getVariantUID(module.getVariant());
            for (String id : root.getVariant().getModuleSlots()) {
                if (Objects.equals(
                        moduleVariantUID,
                        VariantLookup.getVariantUID(root.getVariant().getModuleVariant(id)))) {
                    moduleId = id;
                    break;
                }
            }
            for (SModRecord record : records) {
                if (record instanceof ShipMasterySModRecord smsRecord) {
                    if (!Objects.equals(smsRecord.getRootMember(), root.getFleetMember())) continue;
                    // Make sure the module ids are the same
                    if (!Objects.equals(smsRecord.getModuleId(), moduleId)) continue;
                    // Make sure the module actually has the hull mod and that it isn't built in / was actually removed
                    if (!removedIds.contains(smsRecord.getSMod())) continue;
                    mpSpent += smsRecord.getMpSpent();
                    creditsSpent += smsRecord.getCreditsSpent();
                    toRemove.add(smsRecord);
                }
            }
            for (SModRecord record : toRemove) {
                records.remove(record);
            }

            float creditsRefund = creditsSpent * Settings.CLEAR_SMODS_REFUND_FRACTION;
            int mpRefund = (int) (mpSpent * Settings.CLEAR_SMODS_REFUND_FRACTION);
            Utils.getPlayerCredits().add(creditsRefund);
            ShipMastery.addPlayerMasteryPoints(root.getHullSpec(), mpRefund, false, false);

            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(Strings.MasteryPanel.clearConfirm, Settings.MASTERY_COLOR);
            if (creditsRefund > 0f) {
                Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                        String.format(Strings.MasteryPanel.sModRefundTextCredits, Misc.getDGSCredits(creditsRefund)),
                        Settings.MASTERY_COLOR,
                        Misc.getDGSCredits(creditsRefund),
                        Misc.getHighlightColor());
            }
            if (mpRefund > 0f) {
                Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                        String.format(Strings.MasteryPanel.sModRefundTextMP, Utils.asInt(mpRefund)),
                        Settings.MASTERY_COLOR,
                        Utils.asInt(mpRefund),
                        Misc.getHighlightColor());
            }
            Global.getSoundPlayer().playUISound("sms_add_smod", 1f, 1f);
            masteryPanel.forceRefresh(true, true, true, false);

            // Some non-s-modded hullmods may no longer be applicable; remove these also
            // Do-while loop must terminate; variant's hullmod count is decrementing
            boolean changed;
            do {
                changed = false;
                for (String id : variant.getNonBuiltInHullmods()) {
                    HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
                    if (spec.getEffect() != null && !spec.getEffect().isApplicableToShip(module)) {
                        variant.removeMod(id);
                        changed = true;
                    }
                }
                if (changed) {
                    masteryPanel.forceRefresh(true, true, true, false);
                }
            } while (changed);
        }
        else {
            beginConfirm(button);
            DeferredActionPlugin.performLater(() -> endConfirm(button), Settings.DOUBLE_CLICK_INTERVAL);
        }
    }

    void beginConfirm(ButtonAPI button) {
        button.setCustomData(true);
        button.setText(Strings.MasteryPanel.confirmText);
    }

    void endConfirm(ButtonAPI button) {
        button.setCustomData(false);
        button.setText(defaultText);
    }

    boolean isConfirming(ButtonAPI button) {
        return button.getCustomData() != null && (boolean) button.getCustomData();
    }
}
