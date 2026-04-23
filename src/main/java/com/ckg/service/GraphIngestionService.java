package com.ckg.service;

import com.ckg.CodebaseContext;
import com.ckg.CodebaseGraph;
import com.ckg.model.CodeClass;
import com.ckg.model.CodeMethod;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.springframework.stereotype.Service;
import spoon.Launcher;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.List;
import java.util.Map;

@Service
public class GraphIngestionService {
    private final CodebaseGraph codebaseGraph;
    private final CodebaseContext codebaseContext;

    public GraphIngestionService(CodebaseGraph codebaseGraph, CodebaseContext codebaseContext) {
        this.codebaseGraph = codebaseGraph;
        this.codebaseContext = codebaseContext;
    }

    public void ingestProject(String projectPath) {
        codebaseGraph.clear();
        Graph<Object, DefaultEdge> graph = codebaseGraph.getGraph();

        Launcher launcher = new Launcher();
        launcher.addInputResource(projectPath);
        launcher.getEnvironment().setNoClasspath(true);
        launcher.buildModel();

        // CRITICAL: Store the model for LogicChatService
        codebaseContext.setModel(launcher.getModel());

        Map<String, CodeMethod> registry = codebaseGraph.getRegistry();
        Map<String, CodeClass> classRegistry = codebaseGraph.getClassRegistry();

        // 1. Filtered Vertex Creation
        List<CtClass> classes = launcher.getModel().getElements(new TypeFilter<>(CtClass.class));
        for (CtClass<?> ctClass : classes) {
            // FIX: Check exclusion logic
            if (isExcluded(ctClass)) continue;

            CodeClass classNode = new CodeClass(ctClass.getQualifiedName(), ctClass.getSimpleName());
            classRegistry.put(classNode.getQualifiedName(), classNode);
            graph.addVertex(classNode);

            for (CtMethod<?> ctMethod : ctClass.getMethods()) {
                // FIX: Skip boilerplate getters/setters/etc.
                if (isBoilerplate(ctMethod)) continue;

                CodeMethod methodNode = new CodeMethod(ctMethod.getSignature(), ctMethod.getSimpleName());
                methodNode.setContent(ctMethod.toString());
                registry.put(methodNode.getSignature(), methodNode);

                graph.addVertex(methodNode);
                graph.addEdge(classNode, methodNode); // DECLARES
            }
        }

        // 2. Filtered Edge Creation
        List<CtMethod> allMethods = launcher.getModel().getElements(new TypeFilter<>(CtMethod.class));
        for (CtMethod<?> ctCaller : allMethods) {
            CodeMethod callerNode = registry.get(ctCaller.getSignature());
            if (callerNode == null) continue; // Already filtered out in Loop 1

            List<CtInvocation> invocations = ctCaller.getElements(new TypeFilter<>(CtInvocation.class));
            for (CtInvocation<?> invocation : invocations) {
                String targetSig = invocation.getExecutable().getSignature();
                CodeMethod calledNode = registry.get(targetSig);

                if (calledNode != null) {
                    graph.addEdge(callerNode, calledNode);
                }
            }
        }
    }

    // Exclusion logic
    private boolean isExcluded(CtClass<?> ctClass) {
        String qName = ctClass.getQualifiedName().toLowerCase();
        return qName.contains(".model.")
        || qName.contains(".pojo.");
    }

    private boolean isBoilerplate(CtMethod<?> method) {
        String name = method.getSimpleName();
        return name.equals("toString") ||
                name.equals("hashCode") ||
                name.equals("get*") ||
                name.equals("equals");
    }
}