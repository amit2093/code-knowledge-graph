package com.ckg;

import com.ckg.model.CodeClass;
import com.ckg.model.CodeMethod;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedPseudograph;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CodebaseGraph {
    // Vertex type is Object to support polymorphic nodes (Class + Method)
    private Graph<Object, DefaultEdge> graph = new DirectedPseudograph<>(DefaultEdge.class);

    private final Map<String, CodeMethod> methodRegistry = new ConcurrentHashMap<>();
    private final Map<String, CodeClass> classRegistry = new ConcurrentHashMap<>();

    // FIX: Change return type to Graph<Object, DefaultEdge>
    public Graph<Object, DefaultEdge> getGraph() {
        return graph;
    }

    public Map<String, CodeMethod> getRegistry() { return methodRegistry; }
    public Map<String, CodeClass> getClassRegistry() { return classRegistry; }

    public void clear() {
        methodRegistry.clear();
        classRegistry.clear();
        this.graph = new DirectedPseudograph<>(DefaultEdge.class);
    }
}