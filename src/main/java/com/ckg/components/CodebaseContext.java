package com.ckg.components;

import org.springframework.stereotype.Component;

@Component
public class CodebaseContext {
    private String projectPath;
    private long lastIngestedTime;

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
        this.lastIngestedTime = System.currentTimeMillis();
    }

    public String getProjectPath() {
        return projectPath;
    }

    public long getLastIngestedTime() {
        return lastIngestedTime;
    }
}