package com.dex.myboxmobs.hook;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Entity;

/** ModelEngine R4 implementation. Only loaded when ModelEngine is enabled. */
public final class MegModelSupport implements ModelSupport {

    private final Logger log;

    public MegModelSupport(Logger log) {
        this.log = log;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public List<String> models() {
        List<String> ids = new ArrayList<>(ModelEngineAPI.getAPI().getModelRegistry().getKeys());
        ids.sort(String.CASE_INSENSITIVE_ORDER);
        return ids;
    }

    @Override
    public boolean exists(String id) {
        return id != null && !id.isEmpty() && ModelEngineAPI.getBlueprint(id) != null;
    }

    @Override
    public boolean hasModel(Entity entity) {
        ModeledEntity modeled = ModelEngineAPI.getModeledEntity(entity.getUniqueId());
        return modeled != null && !modeled.getModels().isEmpty();
    }

    @Override
    public boolean apply(Entity entity, String id, double scale, boolean hideBase, boolean replace) {
        if (!exists(id)) return false;
        try {
            ModeledEntity modeled = ModelEngineAPI.getModeledEntity(entity.getUniqueId());
            if (modeled != null && replace) {
                for (String key : List.copyOf(modeled.getModels().keySet())) modeled.removeModel(key);
            }
            if (modeled == null) modeled = ModelEngineAPI.createModeledEntity(entity);
            ActiveModel model = ModelEngineAPI.createActiveModel(id);
            if (model == null) return false;
            model.setScale(scale);
            model.setHitboxScale(scale);
            modeled.addModel(model, true);
            modeled.setBaseEntityVisible(!hideBase);
            return true;
        } catch (RuntimeException ex) {
            log.log(Level.WARNING, "ModelEngine could not attach model " + id, ex);
            return false;
        }
    }
}
