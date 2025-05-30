package shipmastery.achievements;

import org.magiclib.achievements.MagicAchievement;
import org.magiclib.achievements.MagicAchievementManager;
import shipmastery.deferred.Action;
import shipmastery.deferred.DeferredActionPlugin;

// It's possible to save while the game is paused when coming from the fleet screen, unlike with interactable campaign objects.
// Therefore, need to avoid serializing lambdas
// Note: don't convert this to record as xstream also has trouble with those, something about can't get field offset
public final class UnlockAchievementAction implements Action {
    private final Class<? extends MagicAchievement> cls;
    public UnlockAchievementAction(Class<? extends MagicAchievement> cls) {
        this.cls = cls;
    }

    @Override
    public void perform() {
        MagicAchievementManager.getInstance().completeAchievement(cls);
    }

    public static void unlockWhenUnpaused(Class<? extends MagicAchievement> cls) {
        DeferredActionPlugin.performOnUnpause(new UnlockAchievementAction(cls));
    }
}
