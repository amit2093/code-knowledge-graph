package com.ckg.services;

import com.ckg.components.CodebaseGraph;
import com.ckg.models.CodeMethod;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

@Service
public class LogicChatService {
    private final ChatClient chatClient;
    private final CodebaseGraph codebaseGraph;

    public LogicChatService(ChatClient.Builder chatClientBuilder, CodebaseGraph codebaseGraph) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                            You are a senior technical consultant and software architect.
                            Provide high-precision technical analysis based on the provided codebase context.
                            1. Lead with the direct answers.
                            2. Use structured Markdown lists for clarity.
                            3. Focus on architectural tradeoffs, scalability, and logic flow.
                            4. Maintain a professional, objective, and blunt tone.
                            5. Try to keep answers under 100 chars. Only cross 100 chars if you really need but max upto 500 chars.
                        """)
                .build();
        this.codebaseGraph = codebaseGraph;
    }

    public String ask(String question) {
        String query = question.toLowerCase();
        final String context;

        if (query.contains("project") || query.contains("doing") || query.contains("overview")) {
            context = getProjectOverview();
        } else {
            context = getCodeContext(query);
        }

        return chatClient.prompt()
                .user(u -> u.text("Context:\n{context}\n\nQuestion: {question}")
                        .param("context", context)
                        .param("question", question))
                .call()
                .content();
    }

    private String getProjectOverview() {
        // Broad Overview using the In-Memory Registry
        String list = codebaseGraph.getRegistry().values().stream()
                .map(CodeMethod::getName)
                .distinct()
                .collect(Collectors.joining(", "));
        return "This project contains these logic entry points: " + list;
    }

    private String getCodeContext(String question) {
        // Extract significant term
        String raw = question.toLowerCase()
                .replaceAll("(?i)\\b(how|what|is|the|work|code|any|bugs|in|does)\\b", " ")
                .replaceAll("[^a-zA-Z0-9 ]", " ")
                .trim();
        final String searchTerm = raw.isEmpty() ? "main" : raw.split("\\s+")[0];

        // Search in-memory registry
        return codebaseGraph.getRegistry().values().stream()
                .filter(m -> m.getName().toLowerCase().contains(searchTerm)
                        || m.getSignature().toLowerCase().contains(searchTerm))
                .map(m -> String.format("// Method: %s\n%s", m.getName(), m.getContent()))
                .limit(3)
                .collect(Collectors.joining("\n---\n"));
    }
}