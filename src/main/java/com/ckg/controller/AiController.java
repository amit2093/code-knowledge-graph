package com.ckg.controller;

import com.ckg.CodebaseGraph;
import com.ckg.service.LogicChatService;
import com.ckg.model.CodeClass;
import com.ckg.model.CodeMethod;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AiController {
    private final LogicChatService logicChatService;
    private final CodebaseGraph codebaseGraph;

    public AiController(LogicChatService logicChatService, CodebaseGraph codebaseGraph) {
        this.logicChatService = logicChatService;
        this.codebaseGraph = codebaseGraph;
    }

    @GetMapping("/api/chat")
    public String chat(@RequestParam String question) {
        return logicChatService.ask(question);
    }

    @GetMapping("/api/graph")
    public List<Map<String, Object>> getGraphData() {
        List<Map<String, Object>> elements = new ArrayList<>();
        Graph<Object, DefaultEdge> graph = codebaseGraph.getGraph();

        // Add Nodes
        graph.vertexSet().forEach(v -> {
            Map<String, Object> data = new HashMap<>();
            if (v instanceof CodeClass c) {
                data.put("id", c.getQualifiedName());
                data.put("label", c.getSimpleName());
                data.put("type", "Class");
            } else {
                CodeMethod m = (CodeMethod) v;
                data.put("id", m.getSignature());
                data.put("label", m.getName());
                data.put("type", "Method");

                // Attach the raw source code
                if (m.getContent() != null) {
                    data.put("content", m.getContent());
                }
            }
            elements.add(Map.of("data", data));
        });

        // Add Edges
        graph.edgeSet().forEach(e -> {
            elements.add(Map.of("data", Map.of(
                    "source", getId(graph.getEdgeSource(e)),
                    "target", getId(graph.getEdgeTarget(e))
            )));
        });

        return elements;
    }

    private String getId(Object v) {
        return (v instanceof CodeClass c) ? c.getQualifiedName() : ((CodeMethod) v).getSignature();
    }
}