package com.ckg.services;

import com.ckg.components.CodebaseGraph;
import com.ckg.models.CodeClass;
import com.ckg.models.CodeMethod;
import org.springframework.stereotype.Service;

import javax.tools.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Service
public class GraphIngestionService {
    private final CodebaseGraph codebaseGraph;

    public GraphIngestionService(CodebaseGraph codebaseGraph) {
        this.codebaseGraph = codebaseGraph;
    }

    public void ingestProject(String projectPath) {
        codebaseGraph.clear();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("JDK required. JRE is insufficient to run JavaCompiler.");
        }

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        List<File> javaFiles = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(projectPath))) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> javaFiles.add(p.toFile()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read project directory", e);
        }

        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(javaFiles);
        JavacTask task = (JavacTask) compiler.getTask(null, fileManager, null, null, null, compilationUnits);

        try {
            Iterable<? extends CompilationUnitTree> asts = task.parse();
            Map<String, CodeMethod> methodRegistry = codebaseGraph.getRegistry();

            // Pass 1: Build Nodes and Class-to-Method Edges
            for (CompilationUnitTree ast : asts) {
                new NodeExtractionVisitor(ast).scan(new TreePath(ast), null);
            }

            // Pass 2: Build Method-to-Method Call Edges
            for (CompilationUnitTree ast : asts) {
                new EdgeExtractionVisitor(ast, methodRegistry).scan(new TreePath(ast), null);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to parse AST", e);
        }
    }

    // --- VISITOR 1: Extract Classes, Methods, and Source Code ---
    private class NodeExtractionVisitor extends TreePathScanner<Void, Void> {
        private final CompilationUnitTree currentUnit;
        private CodeClass currentClass;

        public NodeExtractionVisitor(CompilationUnitTree currentUnit) {
            this.currentUnit = currentUnit;
        }

        @Override
        public Void visitClass(ClassTree node, Void aVoid) {
            String packageName = currentUnit.getPackageName() != null ? currentUnit.getPackageName().toString() + "." : "";
            String className = node.getSimpleName().toString();
            if (className.isEmpty() || isExcluded(className)) return super.visitClass(node, aVoid);

            // FIX 1: Protect state from being overwritten by nested inner classes
            CodeClass previousClass = currentClass;
            currentClass = new CodeClass(packageName + className, className);

            codebaseGraph.getClassRegistry().put(currentClass.getQualifiedName(), currentClass);
            codebaseGraph.addVertex(currentClass);

            Void result = super.visitClass(node, aVoid);

            // Restore scope after exiting the class
            currentClass = previousClass;
            return result;
        }

        @Override
        public Void visitMethod(MethodTree node, Void aVoid) {
            if (currentClass == null || isBoilerplate(node.getName().toString())) return super.visitMethod(node, aVoid);

            String methodName = node.getName().toString();
            String signature = currentClass.getQualifiedName() + "." + methodName;

            CodeMethod methodNode = new CodeMethod(signature, methodName);
            if (node.getBody() != null) {
                methodNode.setContent(node.getBody().toString());
            }

            codebaseGraph.getRegistry().put(signature, methodNode);
            codebaseGraph.addVertex(methodNode);
            codebaseGraph.addEdge(currentClass, methodNode);

            return super.visitMethod(node, aVoid);
        }
    }

    // --- VISITOR 2: Extract Method Invocations ---
    private class EdgeExtractionVisitor extends TreePathScanner<Void, Void> {
        private final CompilationUnitTree currentUnit;
        private final Map<String, CodeMethod> methodRegistry;
        private CodeClass currentClass;
        private CodeMethod currentMethod;

        public EdgeExtractionVisitor(CompilationUnitTree currentUnit, Map<String, CodeMethod> methodRegistry) {
            this.currentUnit = currentUnit;
            this.methodRegistry = methodRegistry;
        }

        @Override
        public Void visitClass(ClassTree node, Void aVoid) {
            String packageName = currentUnit.getPackageName() != null ? currentUnit.getPackageName().toString() + "." : "";
            String className = node.getSimpleName().toString();
            if (className.isEmpty() || isExcluded(className)) return super.visitClass(node, aVoid);

            CodeClass previousClass = currentClass;
            currentClass = codebaseGraph.getClassRegistry().get(packageName + className);

            Void result = super.visitClass(node, aVoid);
            currentClass = previousClass;
            return result;
        }

        @Override
        public Void visitMethod(MethodTree node, Void aVoid) {
            if (currentClass == null) return super.visitMethod(node, aVoid);

            // FIX 2: Exact signature lookup instead of naive first-match
            String signature = currentClass.getQualifiedName() + "." + node.getName().toString();

            CodeMethod previousMethod = currentMethod;
            currentMethod = methodRegistry.get(signature);

            Void result = super.visitMethod(node, aVoid);
            currentMethod = previousMethod;
            return result;
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
            if (currentMethod != null) {
                String targetName = extractMethodName(node.getMethodSelect());

                // FIX 3: Target resolution still relies on string matching (dependency tradeoff),
                // but the SOURCE is now guaranteed accurate.
                methodRegistry.values().stream()
                        .filter(m -> m.getName().equals(targetName))
                        .forEach(targetMethod -> codebaseGraph.addEdge(currentMethod, targetMethod));
            }
            return super.visitMethodInvocation(node, aVoid);
        }

        private String extractMethodName(ExpressionTree methodSelect) {
            if (methodSelect instanceof IdentifierTree) {
                return ((IdentifierTree) methodSelect).getName().toString();
            } else if (methodSelect instanceof MemberSelectTree) {
                return ((MemberSelectTree) methodSelect).getIdentifier().toString();
            }
            return "";
        }
    }

    private boolean isExcluded(String name) {
        String lower = name.toLowerCase();
        return lower.contains("dto") || lower.contains("model");
    }

    private boolean isBoilerplate(String name) {
        return name.equals("toString") || name.equals("hashCode") ||
                name.startsWith("get") || name.startsWith("set") || name.equals("<init>");
    }
}