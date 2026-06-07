package com.example.voxelgame.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.example.voxelgame.save.SaveManager;
import com.example.voxelgame.world.gen.TerrainGenerator;

public final class ChunkGenerationManager implements AutoCloseable {
    private static final int DEFAULT_WORKER_THREAD_CAP = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    private static final int DEFAULT_PENDING_SCALER = 3;
    private static final int SHUTDOWN_AWAIT_SECONDS = 1;

    private final TerrainGenerator terrainGenerator;
    private final SaveManager saveManager;
    private final ExecutorService executor;
    private final int maxPendingJobs;
    private final int maxFinalizationsPerFrame;
    private final int workerThreadCount;
    private final Object queueLock = new Object();

    private final PriorityBlockingQueue<ChunkPriority> pendingChunks = new PriorityBlockingQueue<>();
    private final ConcurrentHashMap<Long, ChunkGenerationState> chunkStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Future<?>> inProgress = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<ChunkGenerationResult> completedChunks = new ConcurrentLinkedQueue<>();
    private final AtomicLong totalGenerationNanos = new AtomicLong();
    private final AtomicInteger generatedChunkCount = new AtomicInteger();

    public ChunkGenerationManager(TerrainGenerator terrainGenerator, SaveManager saveManager, int renderDistanceChunks, int maxFinalizationsPerFrame) {
        this(terrainGenerator, saveManager, Math.max(16, renderDistanceChunks * renderDistanceChunks * DEFAULT_PENDING_SCALER), maxFinalizationsPerFrame, DEFAULT_WORKER_THREAD_CAP);
    }

    public ChunkGenerationManager(TerrainGenerator terrainGenerator, SaveManager saveManager, int maxPendingJobs, int maxFinalizationsPerFrame, int workerThreadCount) {
        this.terrainGenerator = Objects.requireNonNull(terrainGenerator, "Terrain generator cannot be null.");
        this.saveManager = Objects.requireNonNull(saveManager, "Save manager cannot be null.");
        if (maxPendingJobs < 1) {
            throw new IllegalArgumentException("Max pending jobs must be at least one.");
        }
        if (maxFinalizationsPerFrame < 1) {
            throw new IllegalArgumentException("Max finalizations per frame must be at least one.");
        }
        if (workerThreadCount < 1) {
            throw new IllegalArgumentException("Worker thread count must be at least one.");
        }

        this.maxPendingJobs = maxPendingJobs;
        this.maxFinalizationsPerFrame = maxFinalizationsPerFrame;
        this.workerThreadCount = workerThreadCount;
        this.executor = Executors.newFixedThreadPool(workerThreadCount, new ChunkGenerationThreadFactory());
    }

    public void updateDesiredChunks(int playerChunkX, int playerChunkZ, int renderDistanceChunks, Set<Long> loadedChunkKeys) {
        Objects.requireNonNull(loadedChunkKeys, "Loaded chunk keys cannot be null.");

        List<ChunkPriority> candidates = new ArrayList<>();
        for (int chunkZ = playerChunkZ - renderDistanceChunks; chunkZ <= playerChunkZ + renderDistanceChunks; chunkZ++) {
            for (int chunkX = playerChunkX - renderDistanceChunks; chunkX <= playerChunkX + renderDistanceChunks; chunkX++) {
                long key = ChunkPriority.chunkKey(chunkX, chunkZ);
                if (loadedChunkKeys.contains(key)) {
                    continue;
                }
                ChunkGenerationState state = chunkStates.get(key);
                if (state != null && state != ChunkGenerationState.UNLOADED) {
                    continue;
                }
                int distanceSquared = distanceSquared(chunkX, chunkZ, playerChunkX, playerChunkZ);
                candidates.add(new ChunkPriority(chunkX, chunkZ, distanceSquared));
            }
        }

        candidates.sort(null);
        synchronized (queueLock) {
            for (ChunkPriority candidate : candidates) {
                if (pendingChunks.size() >= maxPendingJobs) {
                    break;
                }
                long key = candidate.key();
                if (chunkStates.containsKey(key)) {
                    continue;
                }
                pendingChunks.offer(candidate);
                chunkStates.put(key, ChunkGenerationState.QUEUED);
            }
            schedulePendingWorkLocked();
        }
    }

    public void pruneFarChunks(int playerChunkX, int playerChunkZ, int renderDistanceChunks) {
        Set<Long> toCancel = new HashSet<>();
        synchronized (queueLock) {
            pendingChunks.removeIf(pending -> {
                boolean outside = !isWithinRenderDistance(pending.chunkX(), pending.chunkZ(), playerChunkX, playerChunkZ, renderDistanceChunks);
                if (outside) {
                    toCancel.add(pending.key());
                }
                return outside;
            });
            for (long key : toCancel) {
                chunkStates.remove(key);
            }
        }

        for (long key : toCancel) {
            Future<?> future = inProgress.remove(key);
            if (future != null) {
                future.cancel(false);
                chunkStates.remove(key);
            }
        }
    }

    public List<ChunkGenerationResult> pollCompletedChunks(int playerChunkX, int playerChunkZ, int renderDistanceChunks, Set<Long> loadedChunkKeys) {
        Objects.requireNonNull(loadedChunkKeys, "Loaded chunk keys cannot be null.");
        List<ChunkGenerationResult> finalizable = new ArrayList<>();
        List<ChunkGenerationResult> toRemove = new ArrayList<>();

        for (ChunkGenerationResult result : completedChunks) {
            if (finalizable.size() >= maxFinalizationsPerFrame) {
                break;
            }
            long key = result.key();
            if (loadedChunkKeys.contains(key)) {
                toRemove.add(result);
                continue;
            }
            if (isWithinRenderDistance(result.chunk().getChunkX(), result.chunk().getChunkZ(), playerChunkX, playerChunkZ, renderDistanceChunks)) {
                finalizable.add(result);
                toRemove.add(result);
            }
        }

        if (!toRemove.isEmpty()) {
            completedChunks.removeAll(toRemove);
        }
        return finalizable;
    }

    public int getQueuedCount() {
        return pendingChunks.size();
    }

    public int getGeneratingCount() {
        return inProgress.size();
    }

    public int getCompletedCount() {
        return completedChunks.size();
    }

    public double getAverageGenerationTimeMillis() {
        int count = generatedChunkCount.get();
        if (count == 0) {
            return 0.0;
        }
        return (double) totalGenerationNanos.get() / count / 1_000_000.0;
    }

    public void notifyChunkLoaded(long chunkKey) {
        chunkStates.remove(chunkKey);
        pendingChunks.removeIf(p -> p.key() == chunkKey);
        Future<?> future = inProgress.remove(chunkKey);
        if (future != null) {
            future.cancel(false);
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_AWAIT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    @Override
    public void close() {
        shutdown();
    }

    void schedulePendingWork() {
        synchronized (queueLock) {
            schedulePendingWorkLocked();
        }
    }

    private void schedulePendingWorkLocked() {
        while (inProgress.size() < workerThreadCount && !pendingChunks.isEmpty()) {
            ChunkPriority next = pendingChunks.poll();
            if (next == null) {
                return;
            }
            long key = next.key();
            if (chunkStates.get(key) != ChunkGenerationState.QUEUED) {
                continue;
            }
            ChunkGenerationTask task = new ChunkGenerationTask(next.chunkX(), next.chunkZ(), terrainGenerator, saveManager);
            Future<?> future = executor.submit(() -> {
                ChunkGenerationResult result = null;
                try {
                    result = task.call();
                    completedChunks.add(result);
                    totalGenerationNanos.addAndGet(result.generationTimeNanos());
                    generatedChunkCount.incrementAndGet();
                    synchronized (queueLock) {
                        if (chunkStates.get(key) == ChunkGenerationState.GENERATING) {
                            chunkStates.put(key, ChunkGenerationState.GENERATED);
                        }
                    }
                } catch (RuntimeException exception) {
                    exception.printStackTrace();
                } finally {
                    inProgress.remove(key);
                }
            });
            inProgress.put(key, future);
            chunkStates.put(key, ChunkGenerationState.GENERATING);
        }
    }

    private static boolean isWithinRenderDistance(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ, int renderDistanceChunks) {
        return Math.abs(chunkX - playerChunkX) <= renderDistanceChunks
                && Math.abs(chunkZ - playerChunkZ) <= renderDistanceChunks;
    }

    private static int distanceSquared(int chunkX, int chunkZ, int originX, int originZ) {
        int dx = chunkX - originX;
        int dz = chunkZ - originZ;
        return dx * dx + dz * dz;
    }

    private static final class ChunkGenerationThreadFactory implements ThreadFactory {
        private int nextThreadId = 1;

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "chunk-generation-worker-" + nextThreadId++);
            thread.setDaemon(true);
            return thread;
        }
    }
}
