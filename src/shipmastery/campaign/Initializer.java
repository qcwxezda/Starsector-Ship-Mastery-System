package shipmastery.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.state.AppDriver;
import shipmastery.util.ClassRefs;

import java.lang.reflect.Field;

public class Initializer implements EveryFrameScript {
    boolean isFirstFrame = true;
    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        StateTracker.setState(AppDriver.getInstance().getCurrentState().getID(), null);

        if (isFirstFrame) {
            // Since the coreUI's "screenPanel" isn't created on the first frame, trying to do anything with the UI
            // on the first frame will cause an NPE. Therefore, we will initialize the screenPanel before trying
            // to call findAllClasses, if it hasn't been initialized already.
            try {
                CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
                Field field = campaignUI.getClass().getDeclaredField("screenPanel");
                field.setAccessible(true);
                if (field.get(campaignUI) == null) {
                    field.set(campaignUI, field.getType().getConstructor(float.class, float.class)
                                               .newInstance(
                                                       Global.getSettings().getScreenWidth(),
                                                       Global.getSettings().getScreenHeight()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            isFirstFrame = false;
        }

        if (!ClassRefs.foundAllClasses()) {
            Global.getSector().getCampaignUI().setDisallowPlayerInteractionsForOneFrame();
            ClassRefs.findAllClasses();
        }
    }
}
