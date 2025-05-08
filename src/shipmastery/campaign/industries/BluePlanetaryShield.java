package shipmastery.campaign.industries;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.PlanetaryShield;
import particleengine.Utils;

import java.awt.Color;

@SuppressWarnings("unused")
public class BluePlanetaryShield extends PlanetaryShield {

    @Override
    public void apply() {
        super.apply();
        applyVisuals(market.getPlanetEntity());
    }

    public static void applyVisuals(PlanetAPI planet, float alphaMult) {
        if (planet == null) return;
        String loc = "graphics/planets/sms_blue_shield.png";
        Utils.getLoadedSprite(loc);
        planet.getSpec().setShieldTexture(loc);
        planet.getSpec().setShieldThickness(0.2f);
        planet.getSpec().setShieldColor(new Color(1f, 1f, 1f, alphaMult));
        planet.applySpecChanges();
    }

    public static void applyVisuals(PlanetAPI planet) {
        applyVisuals(planet, 1f);
    }
}
