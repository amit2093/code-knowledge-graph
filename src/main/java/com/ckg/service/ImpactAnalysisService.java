package com.ckg.service;

import com.ckg.CodebaseGraph;
import com.ckg.model.CodeMethod;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ImpactAnalysisService {
    private final ChatClient chatClient;
    private final CodebaseGraph codebaseGraph;

    public ImpactAnalysisService(ChatClient.Builder chatClientBuilder, CodebaseGraph codebaseGraph) {
        this.chatClient = chatClientBuilder
                .defaultSystem("You are a senior architect analyzing blast radius.")
                .build();
        this.codebaseGraph = codebaseGraph;
    }

    public String analyzeMethodChange(String methodName) {
        // FIX: Update receiving type to Object
        Graph<Object, DefaultEdge> graph = codebaseGraph.getGraph();

        List<String> impacted = codebaseGraph.getRegistry().values().stream()
                .filter(m -> graph.outgoingEdgesOf(m).stream()
                        .anyMatch(e -> {
                            Object target = graph.getEdgeTarget(e);
                            // Safeguard: Only check names if the target is a Method
                            return target instanceof CodeMethod targetMethod &&
                                    targetMethod.getName().equalsIgnoreCase(methodName);
                        }))
                .map(CodeMethod::getName)
                .collect(Collectors.toList());

        return "Impacted Methods: " + (impacted.isEmpty() ? "None" : impacted.toString());
    }
}