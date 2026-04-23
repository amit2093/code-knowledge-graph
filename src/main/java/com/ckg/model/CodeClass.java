package com.ckg.model;

import java.util.Objects;

public class CodeClass {
    private String qualifiedName;
    private String simpleName;

    public CodeClass(String qualifiedName, String simpleName) {
        this.qualifiedName = qualifiedName;
        this.simpleName = simpleName;
    }

    public String getQualifiedName() { return qualifiedName; }
    public String getSimpleName() { return simpleName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeClass codeClass = (CodeClass) o;
        return Objects.equals(qualifiedName, codeClass.qualifiedName);
    }

    @Override
    public int hashCode() { return Objects.hash(qualifiedName); }
}