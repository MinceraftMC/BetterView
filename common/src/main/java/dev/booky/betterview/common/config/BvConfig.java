package dev.booky.betterview.common.config;
// Created by booky10 in BetterView (15:47 03.06.2025)

import dev.booky.betterview.common.config.loading.DurationSerializer;
import dev.booky.betterview.common.config.loading.KeySerializer;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.time.Duration;

@NullMarked
@ConfigSerializable
public class BvConfig {

    public static final TypeSerializerCollection SERIALIZERS = TypeSerializerCollection.builder()
            .register(Duration.class, DurationSerializer.INSTANCE)
            .register(Key.class, KeySerializer.INSTANCE)
            .register(DimensionsConfig.class, DimensionsConfig.SERIALIZER)
            .build();

    @Comment("The current configuration version, do not touch!")
    private int configVersion = 1;
    @Comment("An option for replacing the render distance of the integrated server when running on the client,\n"
            + "use a value of -1 to prevent replacing the render distance of the integrated server")
    private int integratedServerRenderDistance = -1;
    private BvGlobalConfig global = new BvGlobalConfig();
    private DimensionsConfig dimensions = new DimensionsConfig();

    public int getConfigVersion() {
        return this.configVersion;
    }

    public int getIntegratedServerRenderDistance() {
        return this.integratedServerRenderDistance;
    }

    public BvGlobalConfig getGlobalConfig() {
        return this.global;
    }

    public DimensionsConfig getDimensions() {
        return this.dimensions;
    }
}
