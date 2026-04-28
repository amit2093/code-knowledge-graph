package com.ckg.components;

import org.springframework.stereotype.Component;
import spoon.reflect.CtModel;

@Component
public class CodebaseContext {
    private CtModel model;

    public void setModel(CtModel model) { this.model = model; }
    public CtModel getModel() { return model; }
}