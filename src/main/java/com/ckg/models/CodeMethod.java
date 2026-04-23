package com.ckg.models;

import java.util.Objects;

public class CodeMethod {
    private String signature;
    private String name;
    private String content;

    public CodeMethod(String signature, String name) {
        this.signature = signature;
        this.name = name;
    }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeMethod that = (CodeMethod) o;
        return Objects.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(signature);
    }
}