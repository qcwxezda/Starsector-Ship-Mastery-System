package shipmastery.campaign.industries;

import com.fs.starfarer.api.campaign.PlanetAPI;
import particleengine.Utils;
import java.awt.Color;

public class BluePlanetaryShield {
    public static void applyVisuals(PlanetAPI planet, float alphaMult) {
        if (planet == null) return;
        String loc = "graphics/planets/sms_blue_shield.png";
        Utils.getLoadedSprite(loc);
        planet.getSpec().setShieldTexture(loc);
        planet.getSpec().setShieldThickness(0.2f);
        planet.getSpec().setShieldColor(new Color(1f, 1f, 1f, alphaMult));
        planet.applySpecChanges();
    }
}
