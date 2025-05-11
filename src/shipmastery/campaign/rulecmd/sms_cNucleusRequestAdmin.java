package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
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

    private static class RequestAdminDelegate implements CustomDialogDelegate {

        private TextFieldAPI passwordField = null;
        private final InteractionDialogAPI dialog;
        private final Map<String, MemoryAPI> memoryMap;

        private RequestAdminDelegate(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
            this.dialog = dialog;
            this.memoryMap = memoryMap;
        }

        @Override
        public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
            float w = panel.getPosition().getWidth();
            float h = panel.getPosition().getHeight();
            TooltipMakerAPI ttm = panel.createUIElement(w, h, false);
            ttm.setParaFont(Fonts.ORBITRON_20AA);
            ttm.addPara("Please enter admin passcode", 20f).setAlignment(Alignment.MID);
            passwordField = ttm.addTextField(w-10f, 20f);
            passwordField.setMidAlignment();
            passwordField.setHandleCtrlV(false);
            passwordField.setMaxChars(20);
            passwordField.grabFocus();
            panel.addUIElement(ttm).inLMid(0f);
        }

        @Override
        public boolean hasCancelButton() {
            return false;
        }

        @Override
        public String getConfirmText() {
            return Strings.Misc.confirm;
        }

        @Override
        public String getCancelText() {
            return null;
        }

        @Override
        public void customDialogConfirm() {
            if (passwordField == null || passwordField.getText() == null) return;
            if (passwordField.getText().isBlank()) {
                dialog.getTextPanel().addPara(Strings.Campaign.passcodeBlank, Misc.getNegativeHighlightColor());
                return;
            }

            if ("pseudocore7".equals(passwordField.getText().trim())) {
                dialog.getTextPanel().addPara(Strings.Campaign.passcodeRight, Misc.getPositiveHighlightColor());
                memoryMap.get(MemKeys.GLOBAL).set(Strings.Campaign.NUCLEUS_ADMIN_ACCESS, true);
                FireBest.fire(null, dialog, memoryMap, "sms_tNucleusSelectOption");
            } else {
                dialog.getTextPanel().addPara(Strings.Campaign.passcodeWrong, Misc.getNegativeHighlightColor());
            }
        }

        @Override
        public void customDialogCancel() {}

        @Override
        public CustomUIPanelPlugin getCustomPanelPlugin() {
            return null;
        }
    }
}
