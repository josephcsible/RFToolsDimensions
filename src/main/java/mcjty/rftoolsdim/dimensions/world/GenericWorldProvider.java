package mcjty.rftoolsdim.dimensions.world;

import mcjty.lib.compat.CompatWorldProvider;
import mcjty.lib.varia.Logging;
import mcjty.rftoolsdim.api.dimension.IRFToolsWorldProvider;
import mcjty.rftoolsdim.config.GeneralConfiguration;
import mcjty.rftoolsdim.config.PowerConfiguration;
import mcjty.rftoolsdim.dimensions.DimensionInformation;
import mcjty.rftoolsdim.dimensions.DimensionStorage;
import mcjty.rftoolsdim.dimensions.ModDimensions;
import mcjty.rftoolsdim.dimensions.RfToolsDimensionManager;
import mcjty.rftoolsdim.dimensions.description.WeatherDescriptor;
import mcjty.rftoolsdim.dimensions.dimlets.types.Patreons;
import mcjty.rftoolsdim.dimensions.types.ControllerType;
import mcjty.rftoolsdim.dimensions.types.SkyType;
import mcjty.rftoolsdim.dimensions.types.TerrainType;
import mcjty.rftoolsdim.network.PacketGetDimensionEnergy;
import mcjty.rftoolsdim.network.RFToolsDimMessages;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Set;

//@Optional.InterfaceList(@Optional.Interface(iface = "ivorius.reccomplex.dimensions.DimensionDictionary$Handler", modid = "reccomplex"))
public class GenericWorldProvider extends CompatWorldProvider implements  /*@todo implements DimensionDictionary.Handler,*/ IRFToolsWorldProvider {

    public static final String RFTOOLS_DIMENSION = "rftools_dimension";

    private DimensionInformation dimensionInformation;
    private DimensionStorage storage;
    private long seed;
    private Set<String> dimensionTypes = null;  // Used for Recurrent Complex support

    private long calculateSeed(long seed, int dim) {
        if (dimensionInformation == null || dimensionInformation.getWorldVersion() < DimensionInformation.VERSION_DIMLETSSEED) {
            return dim * 13L + seed;
        } else {
            return dimensionInformation.getDescriptor().calculateSeed(seed);
        }
    }

    @Override
    public DimensionType getDimensionType() {
        return ModDimensions.rftoolsType;
    }

    @Override
    public long getSeed() {
        if (dimensionInformation == null || dimensionInformation.getWorldVersion() < DimensionInformation.VERSION_CORRECTSEED) {
            return super.getSeed();
        } else {
            return seed;
        }
    }

    private DimensionInformation getDimensionInformation() {
        if (dimensionInformation == null) {
            // Note: we cannot use worldObj here since we are possibly still busy setting up our world so the 'mapStorage'
            // is always correct here. So we have to use the overworld.
//            WorldServer overworld = DimensionManager.getWorld(0);
            int dim = getDimension();
            dimensionInformation = RfToolsDimensionManager.getDimensionManager(getWorld()).getDimensionInformation(dim);
            if (dimensionInformation == null) {
                Logging.log("Dimension information for dimension " + dim + " is missing!");
            } else {
                setSeed(dim);
//                setupProviderInfo();
            }
        }
        return dimensionInformation;
    }

    @Override
    public String getSaveFolder() {
        return "RFTOOLS" + getDimension();
    }

    //    @Override
//    @Optional.Method(modid = "reccomplex")
//    public Set<String> getDimensionTypes() {
//        getDimensionInformation();
//        if (dimensionInformation == null) {
//            return Collections.EMPTY_SET;
//        }
//        if (dimensionTypes == null) {
//            dimensionTypes = new HashSet<String>();
//            dimensionTypes.add(DimensionDictionary.INFINITE);
//            dimensionTypes.add("RFTOOLS_DIMENSION");
//            // @todo temporary. This should probably be in the TerrainType enum.
//            switch (dimensionInformation.getTerrainType()) {
//                case TERRAIN_VOID:
//                case TERRAIN_ISLAND:
//                case TERRAIN_ISLANDS:
//                case TERRAIN_CHAOTIC:
//                case TERRAIN_PLATEAUS:
//                case TERRAIN_GRID:
//                    dimensionTypes.add(DimensionDictionary.NO_TOP_LIMIT);
//                    dimensionTypes.add(DimensionDictionary.NO_BOTTOM_LIMIT);
//                    break;
//                case TERRAIN_FLAT:
//                case TERRAIN_AMPLIFIED:
//                case TERRAIN_NORMAL:
//                case TERRAIN_NEARLANDS:
//                    dimensionTypes.add(DimensionDictionary.NO_TOP_LIMIT);
//                    dimensionTypes.add(DimensionDictionary.BOTTOM_LIMIT);
//                    break;
//                case TERRAIN_CAVERN_OLD:
//                    dimensionTypes.add(DimensionDictionary.BOTTOM_LIMIT);
//                    dimensionTypes.add(DimensionDictionary.TOP_LIMIT);
//                    break;
//                case TERRAIN_CAVERN:
//                case TERRAIN_LOW_CAVERN:
//                case TERRAIN_FLOODED_CAVERN:
//                    dimensionTypes.add(DimensionDictionary.BOTTOM_LIMIT);
//                    dimensionTypes.add(DimensionDictionary.NO_TOP_LIMIT);
//                    break;
//            }
//            if (dimensionInformation.hasStructureType(StructureType.STRUCTURE_RECURRENTCOMPLEX)) {
//                Collections.addAll(dimensionTypes, dimensionInformation.getDimensionTypes());
//            }
//        }
//        return dimensionTypes;
//    }
//
    private void setSeed(int dim) {
        if (dimensionInformation == null) {
            if (getWorld() == null) {
                return;
            }
            dimensionInformation = RfToolsDimensionManager.getDimensionManager(getWorld()).getDimensionInformation(dim);
            if (dimensionInformation == null) {
                Logging.log("Error: setSeed() called with null diminfo. Error ignored!");
                return;
            }
        }
        long forcedSeed = dimensionInformation.getForcedDimensionSeed();
        if (forcedSeed != 0) {
            Logging.log("Forced seed for dimension " + dim + ": " + forcedSeed);
            seed = forcedSeed;
        } else {
            long baseSeed = dimensionInformation.getBaseSeed();
            if (baseSeed != 0) {
                seed = calculateSeed(baseSeed, dim) ;
            } else {
                seed = calculateSeed(getWorld().getSeed(), dim) ;
            }
        }
//        seed = dimensionInformation.getBaseSeed();
//        System.out.println("seed = " + seed);
    }

    private DimensionStorage getStorage() {
        if (storage == null) {
            storage = DimensionStorage.getDimensionStorage(getWorld());
        }
        return storage;
    }


//    @Override
//    public void registerWorldChunkManager() {
//        getDimensionInformation();
//        setupProviderInfo();
//    }


    @Override
    protected void generateLightBrightnessTable() {
        getDimensionInformation();
        if (dimensionInformation != null && dimensionInformation.getTerrainType() == TerrainType.TERRAIN_INVERTIGO) {
            for (int i = 0; i <= 15; ++i)
            {
                float f1 = 1.0F - (float)i / 15.0F;
                this.lightBrightnessTable[i] = (1.0F - f1) / (f1 * 3.0F + 1.0F) * 1.0F + 1.0F;
            }
            return;
        }
        super.generateLightBrightnessTable();
    }

    @Override
    public BiomeProvider getBiomeProvider() {
        if (biomeProvider == null) {
            createBiomeProviderInternal();
        }
        return biomeProvider;
    }

    @Override
    protected void initialize() {
        if (getWorld() instanceof WorldServer) {
            createBiomeProviderInternal();
            return;
        }

        // We are on a client here and we don't have sufficient information right here (dimension information has not synced yet)
        biomeProvider = null;
    }

    private void createBiomeProviderInternal() {
        getDimensionInformation();
        if (dimensionInformation != null) {
            ControllerType type = dimensionInformation.getControllerType();
            if (type == ControllerType.CONTROLLER_SINGLE) {
                this.biomeProvider = new BiomeProviderSingle(dimensionInformation.getBiomes().get(0));
            } else if (type == ControllerType.CONTROLLER_DEFAULT) {
                WorldInfo worldInfo = getWorld().getWorldInfo();
                worldInfo = new WorldInfo(worldInfo) {
                    @Override
                    public long getSeed() {
                        return seed;
                    }
                };
                this.biomeProvider = new BiomeProvider(worldInfo);
            } else {
                GenericBiomeProvider.hackyDimensionInformation = dimensionInformation;      // Hack to get the dimension information in the superclass.
                this.biomeProvider = new GenericBiomeProvider(seed, getWorld().getWorldInfo(), dimensionInformation);
            }
        } else {
            this.biomeProvider = new BiomeProvider(getWorld().getWorldInfo());
        }

        if (dimensionInformation != null) {
            hasNoSky = !dimensionInformation.getTerrainType().hasSky();

            if (getWorld().isRemote) {
                // Only on client!
                SkyType skyType = dimensionInformation.getSkyDescriptor().getSkyType();
                if (hasNoSky) {
                    SkyRenderer.registerNoSky(this);
                } else if (skyType == SkyType.SKY_ENDER) {
                    SkyRenderer.registerEnderSky(this);
                } else if (skyType == SkyType.SKY_INFERNO || skyType == SkyType.SKY_STARS1 || skyType == SkyType.SKY_STARS2 || skyType == SkyType.SKY_STARS3) {
                    SkyRenderer.registerSkybox(this, skyType);
                } else {
                    SkyRenderer.registerSky(this, dimensionInformation);
                }

                if (dimensionInformation.getSkyDescriptor().isCloudColorGiven() || dimensionInformation.isPatreonBitSet(Patreons.PATREON_KENNEY)) {
                    SkyRenderer.registerCloudRenderer(this, dimensionInformation);
                }
            }
        }
    }

    @Override
    public double getHorizon() {
        getDimensionInformation();
        if (dimensionInformation != null && dimensionInformation.getTerrainType().hasNoHorizon()) {
            return 0;
        } else {
            return super.getHorizon();
        }
    }

    @Override
    public boolean isSurfaceWorld() {
        getDimensionInformation();
        if (dimensionInformation == null) {
            return super.isSurfaceWorld();
        }
        return dimensionInformation.getTerrainType().hasSky();
    }

//    @Override
//    public String getDimensionName() {
//        return RFTOOLS_DIMENSION;
//    }

    @Override
    public String getWelcomeMessage() {
        return "Entering the rftools dimension!";
    }

    @Override
    public boolean canRespawnHere() {
        return false;
    }

    @Override
    public int getRespawnDimension(EntityPlayerMP player) {
        getDimensionInformation();
        if (GeneralConfiguration.respawnSameDim || (dimensionInformation != null && dimensionInformation.isRespawnHere())) {
            DimensionStorage dimensionStorage = getStorage();
            int power = dimensionStorage.getEnergyLevel(getDimension());
            if (power < 1000) {
                return GeneralConfiguration.spawnDimension;
            } else {
                return getDimension();
            }
        }
        return GeneralConfiguration.spawnDimension;
    }

    @Override
    public IChunkGenerator createChunkGenerator() {
        int dim = getDimension();
        setSeed(dim);
        return new GenericChunkGenerator(getWorld(), seed);
    }

    @Override
    public Biome getBiomeForCoords(BlockPos pos) {
        return super.getBiomeForCoords(pos);
    }

    @Override
    public int getActualHeight() {
        return 256;
    }

    private static long lastFogTime = 0;

    @SideOnly(Side.CLIENT)
    @Override
    public float getCloudHeight() {
        getDimensionInformation();
        if (dimensionInformation != null && dimensionInformation.getTerrainType() == TerrainType.TERRAIN_INVERTIGO) {
            return 5;
        }
        return super.getCloudHeight();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Vec3d getFogColor(float angle, float dt) {
        int dim = getDimension();
        if (System.currentTimeMillis() - lastFogTime > 1000) {
            lastFogTime = System.currentTimeMillis();
            RFToolsDimMessages.INSTANCE.sendToServer(new PacketGetDimensionEnergy(dim));
        }

        float factor = calculatePowerBlackout(dim);
        getDimensionInformation();

        float r;
        float g;
        float b;
        if (dimensionInformation == null) {
            r = g = b = 1.0f;
        } else {
            r = dimensionInformation.getSkyDescriptor().getFogColorFactorR() * factor;
            g = dimensionInformation.getSkyDescriptor().getFogColorFactorG() * factor;
            b = dimensionInformation.getSkyDescriptor().getFogColorFactorB() * factor;
        }

        Vec3d color = super.getFogColor(angle, dt);
        return new Vec3d(color.xCoord * r, color.yCoord * g, color.zCoord * b);
    }

    private static long lastTime = 0;

    @Override
    @SideOnly(Side.CLIENT)
    public Vec3d getSkyColor(Entity cameraEntity, float partialTicks) {
        int dim = getDimension();
        if (System.currentTimeMillis() - lastTime > 1000) {
            lastTime = System.currentTimeMillis();
            RFToolsDimMessages.INSTANCE.sendToServer(new PacketGetDimensionEnergy(dim));
        }

        float factor = calculatePowerBlackout(dim);
        getDimensionInformation();

        float r;
        float g;
        float b;
        if (dimensionInformation == null) {
            r = g = b = 1.0f;
        } else {
            r = dimensionInformation.getSkyDescriptor().getSkyColorFactorR() * factor;
            g = dimensionInformation.getSkyDescriptor().getSkyColorFactorG() * factor;
            b = dimensionInformation.getSkyDescriptor().getSkyColorFactorB() * factor;
        }

        Vec3d skyColor = super.getSkyColor(cameraEntity, partialTicks);
        return new Vec3d(skyColor.xCoord * r, skyColor.yCoord * g, skyColor.zCoord * b);
    }

    private float calculatePowerBlackout(int dim) {
        float factor = 1.0f;
        int power = getStorage().getEnergyLevel(dim);
        if (power < PowerConfiguration.DIMPOWER_WARN3) {
            factor = ((float) power) / PowerConfiguration.DIMPOWER_WARN3 * 0.2f;
        } else  if (power < PowerConfiguration.DIMPOWER_WARN2) {
            factor = (float) (power - PowerConfiguration.DIMPOWER_WARN3) / (PowerConfiguration.DIMPOWER_WARN2 - PowerConfiguration.DIMPOWER_WARN3) * 0.3f + 0.2f;
        } else if (power < PowerConfiguration.DIMPOWER_WARN1) {
            factor = (float) (power - PowerConfiguration.DIMPOWER_WARN2) / (PowerConfiguration.DIMPOWER_WARN1 - PowerConfiguration.DIMPOWER_WARN2) * 0.3f + 0.5f;
        } else if (power < PowerConfiguration.DIMPOWER_WARN0) {
            factor = (float) (power - PowerConfiguration.DIMPOWER_WARN1) / (PowerConfiguration.DIMPOWER_WARN0 - PowerConfiguration.DIMPOWER_WARN1) * 0.2f + 0.8f;
        }
        return factor;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public float getSunBrightness(float par1) {
        getDimensionInformation();
        if (dimensionInformation == null) {
            return super.getSunBrightness(par1);
        }
        int dim = getDimension();
        float factor = calculatePowerBlackout(dim);
        return super.getSunBrightness(par1) * dimensionInformation.getSkyDescriptor().getSunBrightnessFactor() * factor;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public float getStarBrightness(float par1) {
        getDimensionInformation();
        if (dimensionInformation == null) {
            return super.getStarBrightness(par1);
        }
        return super.getStarBrightness(par1) * dimensionInformation.getSkyDescriptor().getStarBrightnessFactor();
    }

    @Override
    public void updateWeather() {
        super.updateWeather();
        if (!getWorld().isRemote) {
            getDimensionInformation();
            if (dimensionInformation != null) {
                WeatherDescriptor descriptor = dimensionInformation.getWeatherDescriptor();
                float rs = descriptor.getRainStrength();
                if (rs > -0.5f) {
                    getWorld().rainingStrength = rs;
                    if (Math.abs(getWorld().rainingStrength) < 0.001) {
                        getWorld().prevRainingStrength = 0;
                        getWorld().rainingStrength = 0;
                        getWorld().getWorldInfo().setRaining(false);
                    }
                }

                float ts = descriptor.getThunderStrength();
                if (ts > -0.5f) {
                    getWorld().thunderingStrength = ts;
                    if (Math.abs(getWorld().thunderingStrength) < 0.001) {
                        getWorld().prevThunderingStrength = 0;
                        getWorld().thunderingStrength = 0;
                        getWorld().getWorldInfo().setThundering(false);
                    }
                }
            }
        }
    }

    @Override
    public float calculateCelestialAngle(long time, float dt) {
        getDimensionInformation();
        if (dimensionInformation == null) {
            return super.calculateCelestialAngle(time, dt);
        }

        if (!dimensionInformation.getTerrainType().hasSky()) {
            return 0.5F;
        }

        if (dimensionInformation.getCelestialAngle() == null) {
            if (dimensionInformation.getTimeSpeed() == null) {
                return super.calculateCelestialAngle(time, dt);
            } else {
                return super.calculateCelestialAngle((long) (time * dimensionInformation.getTimeSpeed()), dt);
            }
        } else {
            return dimensionInformation.getCelestialAngle();
        }
    }

    //------------------------ RFToolsWorldProvider


    @Override
    public int getCurrentRF() {
//        DimensionStorage dimensionStorage = DimensionStorage.getDimensionStorage(worldObj);
        return getStorage().getEnergyLevel(getDimension());
    }
}
