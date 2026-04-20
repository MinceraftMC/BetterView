package dev.booky.betterview.common.config;
// Created by booky10 in BetterView (5:25 PM 20.04.2026)

import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

@NullMarked
public final class DimensionsConfig {

    private static final String FALLBACK_KEY = "defaults";

    public static final TypeSerializer<DimensionsConfig> SERIALIZER = new TypeSerializer<>() {
        @SuppressWarnings("PatternValidation")
        @Override
        public DimensionsConfig deserialize(Type type, ConfigurationNode node) throws SerializationException {
            ConfigurationNode fallbackNode = node.node(FALLBACK_KEY);
            BvLevelConfig fallback = fallbackNode.get(BvLevelConfig.class, (Supplier<BvLevelConfig>) BvLevelConfig::new);

            Map<Key, BvLevelConfig> configs = new LinkedHashMap<>();
            for (Map.Entry<Object, ? extends ConfigurationNode> child : node.childrenMap().entrySet()) {
                String dimensionName = String.valueOf(child.getKey());
                if (FALLBACK_KEY.equals(dimensionName)) {
                    continue; // not a dimension
                }
                ConfigurationNode mergedNode = child.getValue().mergeFrom(fallbackNode);
                BvLevelConfig config = mergedNode.get(BvLevelConfig.class, (Supplier<BvLevelConfig>) BvLevelConfig::new);
                configs.put(Key.key(dimensionName), config);
            }

            return new DimensionsConfig(fallback, configs);
        }

        @Override
        public void serialize(Type type, @Nullable DimensionsConfig obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) {
                node.set(null);
                return;
            }

            // minimize lock time by copying
            Set<Map.Entry<Key, BvLevelConfig>> configEntries;
            obj.configsLock.readLock().lock();
            try {
                configEntries = Set.copyOf(obj.configs.entrySet());
            } finally {
                obj.configsLock.readLock().unlock();
            }

            // serialize to node
            ConfigurationNode fallbackNode = node.node(FALLBACK_KEY).set(BvLevelConfig.class, obj.fallback);
            for (Map.Entry<Key, BvLevelConfig> entry : configEntries) {
                ConfigurationNode levelNode = node.node(entry.getKey().asString());
                levelNode.set(BvLevelConfig.class, entry.getValue());
                // only keep "patches" when compared to fallback
                levelNode.set(ConfigurationNodeDiff.extractPatch(fallbackNode, levelNode));
            }
        }
    };

    private final BvLevelConfig fallback;
    private final Map<Key, BvLevelConfig> configs;
    private final ReadWriteLock configsLock = new ReentrantReadWriteLock();

    public DimensionsConfig() {
        this(new BvLevelConfig(), Map.of());
    }

    public DimensionsConfig(BvLevelConfig fallback, Map<Key, BvLevelConfig> configs) {
        this.fallback = fallback;
        this.configs = new LinkedHashMap<>(configs);
    }

    public BvLevelConfig get(Key dimension) {
        // fast path
        this.configsLock.readLock().lock();
        try {
            BvLevelConfig config = this.configs.get(dimension);
            if (config != null) {
                return config;
            }
        } finally {
            this.configsLock.readLock().unlock();
        }
        // "slow" path
        this.configsLock.writeLock().lock();
        try {
            return this.configs.computeIfAbsent(dimension, __ -> new BvLevelConfig());
        } finally {
            this.configsLock.writeLock().unlock();
        }
    }
}
