package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.CampaignUtils;
import shipmastery.util.Strings;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class sms_cNucleusRequestAdmin extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        dialog.showCustomDialog(450f, 110f, new RequestAdminDelegate(dialog, memoryMap));
        return true;
    }

    private static class RequestAdminDelegate extends CampaignUtils.TextFieldDelegate {

        private final InteractionDialogAPI dialog;
        private final Map<String, MemoryAPI> memoryMap;

        private RequestAdminDelegate(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
            super(Strings.Campaign.adminPromptText);
            this.dialog = dialog;
            this.memoryMap = memoryMap;
        }

        @Override
        public void onConfirm(String text) {
            if (text.isBlank()) {
                dialog.getTextPanel().addPara(Strings.Campaign.passcodeBlank, Misc.getNegativeHighlightColor());
                return;
            }
            if ("pseudocore7".equals(text)) {
                dialog.getTextPanel().addPara(Strings.Campaign.passcodeRight, Misc.getPositiveHighlightColor());
                memoryMap.get(MemKeys.GLOBAL).set(Strings.Campaign.NUCLEUS_ADMIN_ACCESS, true);
                FireBest.fire(null, dialog, memoryMap, "sms_tNucleusSelectOption");
            } else {
                dialog.getTextPanel().addPara(Strings.Campaign.passcodeWrong, Misc.getNegativeHighlightColor());
            }
        }
    }
}
