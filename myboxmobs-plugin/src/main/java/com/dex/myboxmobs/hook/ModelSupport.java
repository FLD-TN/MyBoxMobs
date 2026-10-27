package com.dex.myboxmobs.hook;

import java.util.List;
import org.bukkit.entity.Entity;

/** ModelEngine behind an interface: the plugin is optional, {@link #NONE} is used without it. */
public interface ModelSupport {

    ModelSupport NONE = new ModelSupport() {
        @Override
        public boolean available() {
            return false;
        }

        @Override
        public List<String> models() {
            return List.of();
        }

        @Override
        public boolean exists(String id) {
            return false;
        }

        @Override
        public boolean hasModel(Entity entity) {
            return false;
        }

        @Override
        public boolean apply(Entity entity, String id, double scale, boolean hideBase, boolean replace) {
            return false;
        }

        @Override
        public double modelHeight(Entity entity) {
            return Double.NaN;
        }
    };

    boolean available();

    /** Loaded blueprint ids, sorted. */
    List<String> models();

    boolean exists(String id);

    /** True when the entity already shows at least one model (e.g. attached by MythicMobs). */
    boolean hasModel(Entity entity);

    /** Attaches {@code id}; {@code replace} removes the entity's other models first. False when it failed. */
    boolean apply(Entity entity, String id, double scale, boolean hideBase, boolean replace);

    /** Height of the tallest model hitbox on the entity (scaled); NaN without a model. */
    double modelHeight(Entity entity);
}
