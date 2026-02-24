package io.nexflow.engine.app.loader;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blackbox provider for loaded workflows. Callers get a workflow graph by definition id
 * without touching the loader or DB directly. Loaded workflows are cached in memory;
 * later the backing store can be switched to Redis, Hazelcast, etc., without changing callers.
 */
@Service
public class WorkflowProvider {

    private final WorkflowLoader loader;

    /** In-memory cache: key = (workflowDefinitionId, requirePublished). Replace with Redis/Hazelcast later. */
    private final ConcurrentHashMap<CacheKey, WorkflowLoader.LoadedWorkflow> cache = new ConcurrentHashMap<>();

    public WorkflowProvider(WorkflowLoader loader) {
        this.loader = loader;
    }

    /**
     * Returns the loaded workflow (definition + start step id) for the given workflow definition id.
     * Uses in-memory cache on hit; on miss loads from DB via the loader and caches the result.
     *
     * @param workflowDefinitionId workflow_definition.id
     * @param requirePublished     if true, only published workflows are returned; if false, any version is allowed (e.g. for resume)
     * @return empty if not found or (when requirePublished) not published; otherwise the loaded workflow
     */
    public Optional<WorkflowView> getLoadedWorkflow(Long workflowDefinitionId, boolean requirePublished) {
        if (workflowDefinitionId == null) {
            return Optional.empty();
        }
        CacheKey key = new CacheKey(workflowDefinitionId, requirePublished);
        WorkflowLoader.LoadedWorkflow loaded = cache.get(key);
        if (loaded == null) {
            loaded = loader.load(workflowDefinitionId, requirePublished);
            if (loaded == null || loaded.getDefinition() == null) {
                return Optional.empty();
            }
            cache.put(key, loaded);
        }
        return Optional.of(new WorkflowView(loaded.getDefinition(), loaded.getStartStepId()));
    }

    /**
     * Invalidates cached entry for this workflow (e.g. after publish/update). Optional; use when definitions change.
     */
    public void invalidate(Long workflowDefinitionId) {
        if (workflowDefinitionId == null) return;
        cache.remove(new CacheKey(workflowDefinitionId, true));
        cache.remove(new CacheKey(workflowDefinitionId, false));
    }

    private record CacheKey(Long workflowDefinitionId, boolean requirePublished) {}
}
