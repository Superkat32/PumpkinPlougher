package net.superkat.pumpkinplougher;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class PumpkinPlougherItemRenderer extends GeoItemRenderer<PumpkinPlougherItem> {
    public PumpkinPlougherItemRenderer() {
        super(new DefaultedItemGeoModel<>(ResourceLocation.fromNamespaceAndPath(PumpkinPlougher.MODID, "pumpkinplougher")));
    }
}
