package com.example.voxelgame.world.gen;

import com.example.voxelgame.world.BlockRegistry;
import com.example.voxelgame.world.Chunk;
import com.example.voxelgame.world.gen.village.VillageGenerator;
import com.example.voxelgame.world.gen.village.VillageFeature;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class TerrainGenerator {
    private static final int BEDROCK_LAYERS = 4;
    private static final int TREE_MARGIN = 5;
    private static final BiomeProfile[] BIOMES = {
            new BiomeProfile(BiomeType.PLAINS, 0.56, 0.54, 0.0, 0.48, 0.42, 0.35, 0.62, 1.00),
            new BiomeProfile(BiomeType.FOREST, 0.52, 0.82, 2.0, 0.58, 0.58, 1.00, 0.72, 1.04),
            new BiomeProfile(BiomeType.MOUNTAINS, 0.34, 0.43, 18.0, 1.60, 1.38, 0.14, 0.28, 1.28),
            new BiomeProfile(BiomeType.DESERT, 0.88, 0.16, -2.0, 0.36, 0.30, 0.00, 0.08, 0.92),
            new BiomeProfile(BiomeType.SNOWY, 0.14, 0.48, 6.0, 0.72, 0.76, 0.22, 0.36, 1.10)
    };

    private final TerrainGeneratorConfig config;
    private final NoiseSampler heightNoise;
    private final NoiseSampler treeNoise;
    private final NoiseSampler temperatureNoise;
    private final NoiseSampler moistureNoise;
    private final NoiseSampler detailNoise;
    private final NoiseSampler waterNoise;
    private final VillageGenerator villageGenerator;

    public TerrainGenerator(TerrainGeneratorConfig config) {
        this(
                config,
                new SimplexNoise(config.seed()),
                new SimplexNoise(config.seed() ^ 0x6A09E667F3BCC909L),
                new SimplexNoise(config.seed() ^ 0xBB67AE8584CAA73BL),
                new SimplexNoise(config.seed() ^ 0x3C6EF372FE94F82BL),
                new SimplexNoise(config.seed() ^ 0xA54FF53A5F1D36F1L),
                new SimplexNoise(config.seed() ^ 0x510E527FADE682D1L)
        );
    }

    public TerrainGenerator(TerrainGeneratorConfig config, NoiseSampler heightNoise, NoiseSampler treeNoise) {
        this(
                config,
                heightNoise,
                treeNoise,
                new SimplexNoise(config.seed() ^ 0xBB67AE8584CAA73BL),
                new SimplexNoise(config.seed() ^ 0x3C6EF372FE94F82BL),
                new SimplexNoise(config.seed() ^ 0xA54FF53A5F1D36F1L),
                new SimplexNoise(config.seed() ^ 0x510E527FADE682D1L)
        );
    }

    public TerrainGenerator(
            TerrainGeneratorConfig config,
            NoiseSampler heightNoise,
            NoiseSampler treeNoise,
            NoiseSampler temperatureNoise,
            NoiseSampler moistureNoise,
            NoiseSampler detailNoise,
            NoiseSampler waterNoise
    ) {
        this.config = Objects.requireNonNull(config, "Terrain config cannot be null.");
        this.heightNoise = Objects.requireNonNull(heightNoise, "Height noise sampler cannot be null.");
        this.treeNoise = Objects.requireNonNull(treeNoise, "Tree noise sampler cannot be null.");
        this.temperatureNoise = Objects.requireNonNull(temperatureNoise, "Temperature noise sampler cannot be null.");
        this.moistureNoise = Objects.requireNonNull(moistureNoise, "Moisture noise sampler cannot be null.");
        this.detailNoise = Objects.requireNonNull(detailNoise, "Detail noise sampler cannot be null.");
        this.waterNoise = Objects.requireNonNull(waterNoise, "Water noise sampler cannot be null.");
        this.villageGenerator = new VillageGenerator(config.seed(), this);
    }

    public Chunk generateChunk(int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(chunkX, chunkZ);
        Map<Long, Integer> heightCache = new HashMap<>();
        Map<Long, BiomeSample> biomeCache = new HashMap<>();
        int[] columnHeights = computeColumnHeights(chunkX, chunkZ, heightCache);

        for (int localZ = 0; localZ < Chunk.DEPTH; localZ++) {
            for (int localX = 0; localX < Chunk.WIDTH; localX++) {
                int height = columnHeights[columnIndex(localX, localZ)];
                int worldX = chunkX * Chunk.WIDTH + localX;
                int worldZ = chunkZ * Chunk.DEPTH + localZ;
                populateColumn(chunk, localX, localZ, height, sampleBiomeCached(worldX, worldZ, biomeCache));
            }
        }

        populateTrees(chunk, chunkX, chunkZ, heightCache, biomeCache);
        villageGenerator.applyToChunk(chunk, heightCache, biomeCache);
        chunk.clearDirty();
        return chunk;
    }

    public int sampleHeight(int worldX, int worldZ) {
        BiomeSample biome = sampleBiome(worldX, worldZ);
        double continental = octaveNoise(heightNoise, worldX, worldZ, config.noiseScale(), 3, 0.52);
        double detail = detailNoise.sample(worldX * config.terrainDetailScale(), worldZ * config.terrainDetailScale());
        double shaped = continental * biome.heightScale() + detail * 0.16 * biome.roughness();
        int height = config.baseHeight()
                + (int) Math.round(biome.heightOffset())
                + (int) Math.round(shaped * config.heightVariation());
        return Math.clamp(height, 1, Chunk.HEIGHT - 1);
    }

    public BiomeSample sampleBiome(int worldX, int worldZ) {
        double scale = config.biomeScale();
        double temperature = normalize(temperatureNoise.sample(worldX * scale, worldZ * scale));
        double moisture = normalize(moistureNoise.sample((worldX + 10_000) * scale, (worldZ - 10_000) * scale));
        double[] weights = new double[BiomeType.values().length];
        double totalWeight = 0.0;
        BiomeProfile primary = BIOMES[0];
        double primaryWeight = Double.NEGATIVE_INFINITY;

        for (BiomeProfile profile : BIOMES) {
            double temperatureDistance = temperature - profile.idealTemperature();
            double moistureDistance = moisture - profile.idealMoisture();
            double distance = Math.sqrt(temperatureDistance * temperatureDistance + moistureDistance * moistureDistance);
            double influence = 1.0 - Math.clamp(distance / config.biomeBlendStrength(), 0.0, 1.0);
            double weight = smoothstep(influence);
            weights[profile.type().ordinal()] = weight;
            totalWeight += weight;
            if (weight > primaryWeight) {
                primaryWeight = weight;
                primary = profile;
            }
        }

        if (totalWeight <= 0.0) {
            weights[primary.type().ordinal()] = 1.0;
            totalWeight = 1.0;
        }

        double heightOffset = 0.0;
        double heightScale = 0.0;
        double roughness = 0.0;
        double vegetationDensity = 0.0;
        double waterFrequency = 0.0;
        double oreModifier = 0.0;
        for (BiomeProfile profile : BIOMES) {
            int index = profile.type().ordinal();
            weights[index] /= totalWeight;
            double weight = weights[index];
            heightOffset += profile.heightOffset() * weight;
            heightScale += profile.heightScale() * weight;
            roughness += profile.roughness() * weight;
            vegetationDensity += profile.vegetationDensity() * weight;
            waterFrequency += profile.waterFrequency() * weight;
            oreModifier += profile.oreModifier() * weight;
        }

        return new BiomeSample(primary.type(), temperature, moisture, heightOffset, heightScale, roughness, vegetationDensity, waterFrequency, oreModifier, weights);
    }

    public String sampleBiomeDebugText(int worldX, int worldZ) {
        return sampleBiome(worldX, worldZ).debugText();
    }

    public java.util.List<VillageFeature> villagesNearChunk(int chunkX, int chunkZ) {
        return villageGenerator.villagesNearChunk(chunkX, chunkZ, new HashMap<>(), new HashMap<>());
    }

    public java.util.Optional<VillageFeature> findNearestVillage(int worldX, int worldZ, int maxCellRadius) {
        return villageGenerator.findNearestVillage(worldX, worldZ, maxCellRadius);
    }

    private int[] computeColumnHeights(int chunkX, int chunkZ, Map<Long, Integer> heightCache) {
        int[] heights = new int[Chunk.WIDTH * Chunk.DEPTH];
        int baseWorldX = chunkX * Chunk.WIDTH;
        int baseWorldZ = chunkZ * Chunk.DEPTH;

        for (int localZ = 0; localZ < Chunk.DEPTH; localZ++) {
            for (int localX = 0; localX < Chunk.WIDTH; localX++) {
                int worldX = baseWorldX + localX;
                int worldZ = baseWorldZ + localZ;
                heights[columnIndex(localX, localZ)] = sampleHeightCached(worldX, worldZ, heightCache);
            }
        }

        return heights;
    }

    private void populateTrees(Chunk chunk, int chunkX, int chunkZ, Map<Long, Integer> heightCache, Map<Long, BiomeSample> biomeCache) {
        int baseWorldX = chunkX * Chunk.WIDTH;
        int baseWorldZ = chunkZ * Chunk.DEPTH;
        int margin = TREE_MARGIN;

        for (int worldZ = baseWorldZ - margin; worldZ < baseWorldZ + Chunk.DEPTH + margin; worldZ++) {
            for (int worldX = baseWorldX - margin; worldX < baseWorldX + Chunk.WIDTH + margin; worldX++) {
                int surfaceY = sampleHeightCached(worldX, worldZ, heightCache);
                BiomeSample biome = sampleBiomeCached(worldX, worldZ, biomeCache);
                if (!shouldPlaceTree(worldX, worldZ, surfaceY, biome, heightCache)) {
                    continue;
                }
                placeTree(chunk, worldX, worldZ, surfaceY + 1, chooseTreeType(worldX, worldZ, biome));
            }
        }
    }

    private void populateColumn(Chunk chunk, int localX, int localZ, int surfaceY, BiomeSample biome) {
        int dirtStartY = Math.max(BEDROCK_LAYERS, surfaceY - config.dirtDepth() + 1);
        int worldX = chunk.getChunkX() * Chunk.WIDTH + localX;
        int worldZ = chunk.getChunkZ() * Chunk.DEPTH + localZ;
        int localSeaLevel = localSeaLevel(worldX, worldZ, biome);
        boolean beach = surfaceY <= localSeaLevel + 2 && biome.primaryBiome() != BiomeType.SNOWY;

        for (int y = 0; y <= surfaceY; y++) {
            if (y < BEDROCK_LAYERS) {
                chunk.setBlock(localX, y, localZ, BlockRegistry.BEDROCK);
            } else if (y == surfaceY) {
                chunk.setBlock(localX, y, localZ, surfaceBlockFor(beach, biome));
            } else if (beach && y >= surfaceY - 3) {
                chunk.setBlock(localX, y, localZ, y >= surfaceY - 1 ? BlockRegistry.SAND : BlockRegistry.SANDSTONE);
            } else if (biome.primaryBiome() == BiomeType.DESERT && y >= dirtStartY - 1) {
                chunk.setBlock(localX, y, localZ, y >= surfaceY - 3 ? BlockRegistry.SAND : BlockRegistry.SANDSTONE);
            } else if (y >= dirtStartY) {
                chunk.setBlock(localX, y, localZ, BlockRegistry.DIRT);
            } else {
                chunk.setBlock(localX, y, localZ, stoneBlockFor(worldX, y, worldZ, biome));
            }
        }

        for (int y = surfaceY + 1; y <= localSeaLevel; y++) {
            chunk.setBlock(localX, y, localZ, biome.primaryBiome() == BiomeType.SNOWY && y == localSeaLevel ? BlockRegistry.ICE : BlockRegistry.WATER);
        }
    }

    private com.example.voxelgame.world.Block surfaceBlockFor(boolean beach, BiomeSample biome) {
        if (biome.primaryBiome() == BiomeType.SNOWY) {
            return BlockRegistry.SNOW;
        }
        if (biome.primaryBiome() == BiomeType.DESERT) {
            return BlockRegistry.SAND;
        }
        if (beach) {
            return BlockRegistry.SAND;
        }
        return BlockRegistry.GRASS;
    }

    private com.example.voxelgame.world.Block stoneBlockFor(int worldX, int y, int worldZ, BiomeSample biome) {
        long hash = mix64((((long) worldX) << 32) ^ (worldZ & 0xFFFFFFFFL) ^ (long) y * 0x9E3779B97F4A7C15L ^ config.seed());
        int roll = Math.floorMod((int) hash, 1000);
        int oreBoost = (int) Math.round((biome.oreModifier() - 1.0) * 40.0);
        if (y < 24 && roll < 7 + oreBoost / 6) {
            return BlockRegistry.DIAMOND_ORE;
        }
        if (y < 44 && roll < 16 + oreBoost / 3) {
            return BlockRegistry.GOLD_ORE;
        }
        if (y < 74 && roll < 30 + oreBoost) {
            return BlockRegistry.IRON_ORE;
        }
        if (y < 92 && roll < 46 + oreBoost) {
            return BlockRegistry.COPPER_ORE;
        }
        if (y < 118 && roll < 66 + oreBoost) {
            return BlockRegistry.COAL_ORE;
        }
        if (y > config.seaLevel() - 4 && roll > 986) {
            return BlockRegistry.MOSSY_STONE;
        }
        if (roll > 972) {
            return BlockRegistry.GRAVEL;
        }
        return BlockRegistry.STONE;
    }

    private boolean shouldPlaceTree(int worldX, int worldZ, int surfaceY, BiomeSample biome, Map<Long, Integer> heightCache) {
        if (surfaceY <= config.seaLevel() + 1 || surfaceY >= Chunk.HEIGHT - 8) {
            return false;
        }
        if (biome.vegetationDensity() <= 0.02 || biome.primaryBiome() == BiomeType.DESERT) {
            return false;
        }

        int cellSize = config.treeCellSize();
        int cellX = Math.floorDiv(worldX, cellSize);
        int cellZ = Math.floorDiv(worldZ, cellSize);
        long cellHash = mix64((((long) cellX) << 32) ^ (cellZ & 0xFFFFFFFFL) ^ config.seed());
        int candidateX = cellX * cellSize + Math.floorMod((int) cellHash, cellSize);
        int candidateZ = cellZ * cellSize + Math.floorMod((int) (cellHash >>> 32), cellSize);
        if (worldX != candidateX || worldZ != candidateZ) {
            return false;
        }

        double density = treeNoise.sample(worldX * config.treeNoiseScale(), worldZ * config.treeNoiseScale());
        double threshold = config.treeThreshold() + (1.0 - biome.vegetationDensity()) * 0.58;
        if (density < threshold) {
            return false;
        }

        int east = sampleHeightCached(worldX + 1, worldZ, heightCache);
        int west = sampleHeightCached(worldX - 1, worldZ, heightCache);
        int north = sampleHeightCached(worldX, worldZ - 1, heightCache);
        int south = sampleHeightCached(worldX, worldZ + 1, heightCache);
        return Math.abs(surfaceY - east) <= 2
                && Math.abs(surfaceY - west) <= 2
                && Math.abs(surfaceY - north) <= 2
                && Math.abs(surfaceY - south) <= 2;
    }

    private TreeType chooseTreeType(int worldX, int worldZ, BiomeSample biome) {
        long hash = mix64((((long) worldX) << 32) ^ (worldZ & 0xFFFFFFFFL) ^ config.seed() ^ 0xD1B54A32D192ED03L);
        double roll = (hash >>> 11) * 0x1.0p-53;
        return switch (biome.primaryBiome()) {
            case SNOWY, MOUNTAINS -> roll < 0.82 ? TreeType.SPRUCE : TreeType.BIRCH;
            case FOREST -> {
                if (roll < 0.42) {
                    yield TreeType.OAK;
                }
                if (roll < 0.68) {
                    yield TreeType.DARK_OAK;
                }
                if (roll < 0.86) {
                    yield TreeType.BIRCH;
                }
                yield TreeType.SPRUCE;
            }
            case PLAINS -> roll < 0.72 ? TreeType.OAK : TreeType.BIRCH;
            case DESERT -> TreeType.OAK;
        };
    }

    private void placeTree(Chunk chunk, int worldX, int worldZ, int trunkBaseY, TreeType treeType) {
        long treeHash = mix64((((long) worldX) << 32) ^ (worldZ & 0xFFFFFFFFL) ^ config.seed() ^ 0x9E3779B97F4A7C15L);
        switch (treeType) {
            case OAK -> placeOakTree(chunk, worldX, worldZ, trunkBaseY, 4 + Math.floorMod((int) (treeHash >>> 17), 3));
            case DARK_OAK -> placeDarkOakTree(chunk, worldX, worldZ, trunkBaseY, 5 + Math.floorMod((int) (treeHash >>> 17), 3));
            case BIRCH -> placeBirchTree(chunk, worldX, worldZ, trunkBaseY, 5 + Math.floorMod((int) (treeHash >>> 17), 3));
            case SPRUCE -> placeSpruceTree(chunk, worldX, worldZ, trunkBaseY, 6 + Math.floorMod((int) (treeHash >>> 17), 5));
        }
    }

    private void placeOakTree(Chunk chunk, int worldX, int worldZ, int trunkBaseY, int trunkHeight) {
        for (int dy = 0; dy < trunkHeight; dy++) {
            placeBlockIfInside(chunk, worldX, trunkBaseY + dy, worldZ, BlockRegistry.OAK_LOG, false);
        }

        int canopyBaseY = trunkBaseY + trunkHeight - 2;
        for (int dy = 0; dy <= 3; dy++) {
            int radius = switch (dy) {
                case 0, 1 -> 2;
                case 2 -> 1;
                default -> 0;
            };

            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (radius == 2 && Math.abs(dx) == 2 && Math.abs(dz) == 2 && dy < 2) {
                        continue;
                    }
                    if (dx == 0 && dz == 0 && dy < 3) {
                        continue;
                    }
                    placeBlockIfInside(chunk, worldX + dx, canopyBaseY + dy, worldZ + dz, BlockRegistry.OAK_LEAVES, true);
                }
            }
        }

        placeBlockIfInside(chunk, worldX, canopyBaseY + 4, worldZ, BlockRegistry.OAK_LEAVES, true);
    }

    private void placeDarkOakTree(Chunk chunk, int worldX, int worldZ, int trunkBaseY, int trunkHeight) {
        for (int dy = 0; dy < trunkHeight; dy++) {
            placeBlockIfInside(chunk, worldX, trunkBaseY + dy, worldZ, BlockRegistry.DARK_OAK_LOG, false);
            placeBlockIfInside(chunk, worldX + 1, trunkBaseY + dy, worldZ, BlockRegistry.DARK_OAK_LOG, false);
            placeBlockIfInside(chunk, worldX, trunkBaseY + dy, worldZ + 1, BlockRegistry.DARK_OAK_LOG, false);
            placeBlockIfInside(chunk, worldX + 1, trunkBaseY + dy, worldZ + 1, BlockRegistry.DARK_OAK_LOG, false);
        }

        int canopyBaseY = trunkBaseY + trunkHeight - 3;
        for (int dy = 0; dy <= 4; dy++) {
            int radius = switch (dy) {
                case 0 -> 2;
                case 1, 2 -> 3;
                case 3 -> 2;
                default -> 1;
            };
            for (int dz = -radius; dz <= radius + 1; dz++) {
                for (int dx = -radius; dx <= radius + 1; dx++) {
                    boolean farCorner = Math.abs(dx) == radius && Math.abs(dz) == radius && dy < 3;
                    if (!farCorner) {
                        placeBlockIfInside(chunk, worldX + dx, canopyBaseY + dy, worldZ + dz, BlockRegistry.DARK_OAK_LEAVES, true);
                    }
                }
            }
        }
    }

    private void placeBirchTree(Chunk chunk, int worldX, int worldZ, int trunkBaseY, int trunkHeight) {
        for (int dy = 0; dy < trunkHeight; dy++) {
            placeBlockIfInside(chunk, worldX, trunkBaseY + dy, worldZ, BlockRegistry.BIRCH_LOG, false);
        }

        int canopyBaseY = trunkBaseY + trunkHeight - 2;
        for (int dy = 0; dy <= 3; dy++) {
            int radius = dy == 0 ? 1 : (dy <= 2 ? 2 : 1);
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                        continue;
                    }
                    if (dx == 0 && dz == 0 && dy < 3) {
                        continue;
                    }
                    placeBlockIfInside(chunk, worldX + dx, canopyBaseY + dy, worldZ + dz, BlockRegistry.BIRCH_LEAVES, true);
                }
            }
        }
        placeBlockIfInside(chunk, worldX, canopyBaseY + 4, worldZ, BlockRegistry.BIRCH_LEAVES, true);
    }

    private void placeSpruceTree(Chunk chunk, int worldX, int worldZ, int trunkBaseY, int trunkHeight) {
        for (int dy = 0; dy < trunkHeight; dy++) {
            placeBlockIfInside(chunk, worldX, trunkBaseY + dy, worldZ, BlockRegistry.SPRUCE_LOG, false);
        }

        int canopyTopY = trunkBaseY + trunkHeight;
        for (int layer = 0; layer < trunkHeight - 1; layer++) {
            int y = canopyTopY - layer;
            int radius = Math.max(0, Math.min(3, (layer + 1) / 2));
            if (layer > trunkHeight - 5) {
                radius = Math.max(radius, 2);
            }
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) + Math.abs(dz) > radius + 1) {
                        continue;
                    }
                    if (dx == 0 && dz == 0 && layer > 1) {
                        continue;
                    }
                    placeBlockIfInside(chunk, worldX + dx, y, worldZ + dz, BlockRegistry.SPRUCE_LEAVES, true);
                }
            }
        }
        placeBlockIfInside(chunk, worldX, canopyTopY + 1, worldZ, BlockRegistry.SPRUCE_LEAVES, true);
    }

    private void placeBlockIfInside(Chunk chunk, int worldX, int worldY, int worldZ, com.example.voxelgame.world.Block block, boolean onlyReplaceAir) {
        if (worldY < 0 || worldY >= Chunk.HEIGHT) {
            return;
        }

        int chunkX = Math.floorDiv(worldX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH);
        if (chunkX != chunk.getChunkX() || chunkZ != chunk.getChunkZ()) {
            return;
        }

        int localX = Math.floorMod(worldX, Chunk.WIDTH);
        int localZ = Math.floorMod(worldZ, Chunk.DEPTH);
        if (onlyReplaceAir && chunk.getBlock(localX, worldY, localZ) != BlockRegistry.AIR) {
            return;
        }

        chunk.setBlock(localX, worldY, localZ, block);
    }

    private int sampleHeightCached(int worldX, int worldZ, Map<Long, Integer> heightCache) {
        long key = ((((long) worldX) << 32) ^ (worldZ & 0xFFFFFFFFL));
        Integer cached = heightCache.get(key);
        if (cached != null) {
            return cached;
        }

        int value = sampleHeight(worldX, worldZ);
        heightCache.put(key, value);
        return value;
    }

    private BiomeSample sampleBiomeCached(int worldX, int worldZ, Map<Long, BiomeSample> biomeCache) {
        long key = ((((long) worldX) << 32) ^ (worldZ & 0xFFFFFFFFL));
        BiomeSample cached = biomeCache.get(key);
        if (cached != null) {
            return cached;
        }

        BiomeSample value = sampleBiome(worldX, worldZ);
        biomeCache.put(key, value);
        return value;
    }

    private int localSeaLevel(int worldX, int worldZ, BiomeSample biome) {
        double pondNoise = normalize(waterNoise.sample(worldX * config.terrainDetailScale() * 0.55, worldZ * config.terrainDetailScale() * 0.55));
        int waterOffset = pondNoise < biome.waterFrequency() * 0.16 ? 1 : 0;
        if (biome.primaryBiome() == BiomeType.DESERT) {
            waterOffset -= 2;
        }
        return Math.clamp(config.seaLevel() + waterOffset, 1, Chunk.HEIGHT - 1);
    }

    private double octaveNoise(NoiseSampler sampler, int worldX, int worldZ, double scale, int octaves, double persistence) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = 1.0;
        double amplitudeTotal = 0.0;
        for (int octave = 0; octave < octaves; octave++) {
            total += sampler.sample(worldX * scale * frequency, worldZ * scale * frequency) * amplitude;
            amplitudeTotal += amplitude;
            amplitude *= persistence;
            frequency *= 2.0;
        }
        return total / amplitudeTotal;
    }

    private double normalize(double noiseValue) {
        return Math.clamp((noiseValue + 1.0) * 0.5, 0.0, 1.0);
    }

    private double smoothstep(double value) {
        double clamped = Math.clamp(value, 0.0, 1.0);
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private static int columnIndex(int x, int z) {
        return x + z * Chunk.WIDTH;
    }

    private enum TreeType {
        OAK,
        DARK_OAK,
        BIRCH,
        SPRUCE
    }
}
