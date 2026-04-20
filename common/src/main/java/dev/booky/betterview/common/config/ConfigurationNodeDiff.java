package dev.booky.betterview.common.config;
// Created by booky10 in BetterView (6:25 PM 20.04.2026)

import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Map;
import java.util.Objects;

@NullMarked
final class ConfigurationNodeDiff {

    private ConfigurationNodeDiff() {
    }

    public static ConfigurationNode extractPatch(ConfigurationNode base, ConfigurationNode node) throws SerializationException {
        ConfigurationNode ret = node.copy();
        boolean eq;
        if (node.isList()) {
            if (!base.isList()) {
                throw new IllegalStateException("Type mismatch: node is list but base isn't");
            }
            eq = node.childrenList().equals(base.childrenList());
        } else if (node.isMap()) {
            if (!base.isMap()) {
                throw new IllegalStateException("Type mismatch: node isn't map or list, but base is map");
            }
            // recursively check each children value
            for (Map.Entry<Object, ? extends ConfigurationNode> child : node.childrenMap().entrySet()) {
                ConfigurationNode baseChild = base.node(child.getKey());
                if (!baseChild.virtual()) {
                    ret.node(child.getKey()).set(extractPatch(baseChild, child.getValue()));
                }
            }
            eq = ret.empty();
        } else {
            eq = Objects.equals(base.rawScalar(), node.rawScalar());
        }
        return eq ? ret.set(null) : ret;
    }
}
