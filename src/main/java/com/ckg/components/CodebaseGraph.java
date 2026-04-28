package com.ckg.components;

import com.ckg.models.CodeClass;
import com.ckg.models.CodeMethod;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CodebaseGraph {
    // Standard collections drastically reduce memory footprint
    private final Map<Object, Set<Object>> adjacencyList = new HashMap<>();
    private final Map<String, CodeMethod> methodRegistry = new HashMap<>();
    private final Map<String, CodeClass> classRegistry = new HashMap<>();
    public void addEdge(Object source, Object target) {
        if (source == null || target == null) return; // Prevent ConcurrentHashMap NPE
        addVertex(source);
        addVertex(target);
        adjacencyList.get(source).add(target);
    }

    public void addVertex(Object vertex) {
        if (vertex == null) return;
        adjacencyList.putIfAbsent(vertex, ConcurrentHashMap.newKeySet());
    }

    public Map<Object, Set<Object>> getAdjacencyList() { return adjacencyList; }
    public Set<Object> getVertices() { return adjacencyList.keySet(); }
    public Set<Object> getOutgoingDependencies(Object source) {
        return adjacencyList.getOrDefault(source, Set.of());
    }

    public Map<String, CodeMethod> getRegistry() { return methodRegistry; }
    public Map<String, CodeClass> getClassRegistry() { return classRegistry; }

    public void clear() {
        methodRegistry.clear();
        classRegistry.clear();
        adjacencyList.clear();
    }
}