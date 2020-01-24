package weather2.client.foliage;

import java.util.ArrayList;
import java.util.List;

import extendedrenderer.foliage.Foliage;

public class FoliageLocationData {

    public FoliageReplacerBase foliageReplacer;
    public List<Foliage> listFoliage = new ArrayList<>();

    public FoliageLocationData(FoliageReplacerBase foliageReplacer) {
        this.foliageReplacer = foliageReplacer;
    }
}
