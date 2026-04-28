package com.ckg.controllers;

import com.ckg.components.CodebaseGraph;
import com.ckg.services.LogicChatService;
import com.ckg.models.CodeClass;
import com.ckg.models.CodeMethod;
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

        // Add Nodes
        codebaseGraph.getVertices().forEach(v -> {
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
                if (m.getContent() != null) data.put("content", m.getContent());
            }
            elements.add(Map.of("data", data));
        });

        // Add Edges via Adjacency List
        codebaseGraph.getAdjacencyList().forEach((source, targets) -> {
            targets.forEach(target -> {
                elements.add(Map.of("data", Map.of(
                        "source", getId(source),
                        "target", getId(target)
                )));
            });
        });

        return elements;
    }

    private String getId(Object v) {
        return (v instanceof CodeClass c) ? c.getQualifiedName() : ((CodeMethod) v).getSignature();
    }
}