package mcjty.rftoolsdim.dimensions.description;

import net.minecraft.entity.EntityLiving;
import net.minecraft.world.biome.Biome;
import org.apache.commons.lang3.Validate;

public class MobDescriptor extends Biome.SpawnListEntry {
    private final int maxLoaded;

    public MobDescriptor(Class<? extends EntityLiving> entityClass, int spawnChance, int minGroup, int maxGroup, int maxLoaded) {
        super(Validate.notNull(entityClass), spawnChance, minGroup, maxGroup);
        this.maxLoaded = maxLoaded;
    }

    public int getMaxLoaded() {
        return maxLoaded;
    }

    @Override
    public String toString() {
        return super.toString() + " max " + maxLoaded;
    }
}
