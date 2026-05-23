
package com.github.trae.mcp.autoconfigure.scanner;

import com.github.trae.mcp.core.annotation.Tool;
import com.github.trae.mcp.core.annotation.ToolParam;
import com.github.trae.mcp.core.model.ToolDefinition;
import com.github.trae.mcp.core.model.ToolParameter;
import com.github.trae.mcp.core.registry.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ToolScanner {

    private static final Logger logger = LoggerFactory.getLogger(ToolScanner.class);

    private final ApplicationContext applicationContext;
    private final ToolRegistry toolRegistry;

    public ToolScanner(ApplicationContext applicationContext, ToolRegistry toolRegistry) {
        this.applicationContext = applicationContext;
        this.toolRegistry = toolRegistry;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void scan() {
        logger.info("=== Starting MCP Tool Scanning ===");
        Map<String, Object> beans = applicationContext.getBeansOfType(Object.class);
        
        int toolCount = 0;
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object bean = entry.getValue();
            Class<?> beanClass = bean.getClass();
            
            for (Method method : beanClass.getDeclaredMethods()) {
                Tool toolAnnotation = method.getAnnotation(Tool.class);
                if (toolAnnotation != null) {
                    registerTool(bean, method, toolAnnotation);
                    toolCount++;
                }
            }
        }
        
        logger.info("=== MCP Tool Scanning completed. Registered {} tools. ===", toolRegistry.getAllDefinitions().size());
    }

    private void registerTool(Object bean, Method method, Tool toolAnnotation) {
        String toolName = getToolName(toolAnnotation, method);
        String description = getToolDescription(toolAnnotation);
        
        List<ToolParameter> parameters = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            ToolParameter param = parseParameter(parameter);
            parameters.add(param);
        }
        
        ToolDefinition definition = new ToolDefinition(toolName, description, toolAnnotation.streaming(), parameters);
        toolRegistry.register(definition, bean, method);
        
        logger.debug("Registered tool: {} -> {}.{}", toolName, bean.getClass().getSimpleName(), method.getName());
    }

    private String getToolName(Tool annotation, Method method) {
        if (!annotation.name().isEmpty()) {
            return annotation.name();
        }
        if (!annotation.value().isEmpty()) {
            return annotation.value();
        }
        return method.getName();
    }

    private String getToolDescription(Tool annotation) {
        if (!annotation.description().isEmpty()) {
            return annotation.description();
        }
        if (!annotation.value().isEmpty() && !annotation.name().equals(annotation.value())) {
            return annotation.value();
        }
        return "";
    }

    private ToolParameter parseParameter(Parameter parameter) {
        ToolParam paramAnnotation = parameter.getAnnotation(ToolParam.class);
        
        String name = paramAnnotation != null && !paramAnnotation.value().isEmpty() 
            ? paramAnnotation.value() 
            : parameter.getName();
        
        String description = paramAnnotation != null ? paramAnnotation.description() : "";
        boolean required = paramAnnotation == null || paramAnnotation.required();
        String defaultValue = paramAnnotation != null ? paramAnnotation.defaultValue() : "";
        String type = parameter.getType().getSimpleName();
        
        return new ToolParameter(name, description, required, type, defaultValue);
    }
}
