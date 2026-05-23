
package com.github.trae.mcp.autoconfigure.executor;

import com.github.trae.mcp.core.executor.ToolExecutor;
import com.github.trae.mcp.core.model.ToolCallRequest;
import com.github.trae.mcp.core.model.ToolCallResponse;
import com.github.trae.mcp.core.model.ToolDefinition;
import com.github.trae.mcp.core.model.ToolParameter;
import com.github.trae.mcp.core.registry.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;


public class DefaultToolExecutor implements ToolExecutor {

    private static final Logger logger = LoggerFactory.getLogger(DefaultToolExecutor.class);

    private final ToolRegistry toolRegistry;

    public DefaultToolExecutor(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public List<ToolDefinition> listTools() {
        return toolRegistry.getAllDefinitions();
    }

    @Override
    public ToolCallResponse execute(ToolCallRequest request) {
        String toolName = request.getToolName();
        
        if (!toolRegistry.contains(toolName)) {
            logger.warn("Tool not found: {}", toolName);
            return ToolCallResponse.error("Tool not found: " + toolName);
        }
        
        try {
            ToolDefinition definition = toolRegistry.getDefinition(toolName).get();
            Object bean = toolRegistry.getBean(toolName).get();
            Method method = toolRegistry.getMethod(toolName).get();
            
            Object[] args = buildArguments(definition.getParameters(), request.getArguments());
            Object result = method.invoke(bean, args);
            
            logger.debug("Tool executed successfully: {}", toolName);
            return ToolCallResponse.success(result);
            
        } catch (Exception e) {
            logger.error("Error executing tool: {}", toolName, e);
            return ToolCallResponse.error("Error executing tool: " + e.getMessage());
        }
    }

    private Object[] buildArguments(List<ToolParameter> parameters, Map<String, Object> arguments) {
        Object[] args = new Object[parameters.size()];
        
        for (int i = 0; i < parameters.size(); i++) {
            ToolParameter param = parameters.get(i);
            Object value = arguments != null ? arguments.get(param.getName()) : null;
            
            if (value == null) {
                if (param.isRequired()) {
                    throw new IllegalArgumentException("Missing required parameter: " + param.getName());
                }
                value = parseDefaultValue(param.getDefaultValue(), param.getType());
            }
            
            args[i] = convertValue(value, param.getType());
        }
        
        return args;
    }

    private Object convertValue(Object value, String targetType) {
        if (value == null) {
            return null;
        }
        
        String valueStr = value.toString();
        
        switch (targetType.toLowerCase()) {
            case "int":
            case "integer":
                return Integer.parseInt(valueStr);
            case "long":
                return Long.parseLong(valueStr);
            case "double":
                return Double.parseDouble(valueStr);
            case "float":
                return Float.parseFloat(valueStr);
            case "boolean":
                return Boolean.parseBoolean(valueStr);
            case "string":
            default:
                return valueStr;
        }
    }

    private Object parseDefaultValue(String defaultValue, String type) {
        if (defaultValue == null || defaultValue.isEmpty()) {
            return null;
        }
        return convertValue(defaultValue, type);
    }
}
