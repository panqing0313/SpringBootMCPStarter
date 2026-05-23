
package com.github.star.mcp.autoconfigure.registry;

import com.github.star.mcp.core.model.ToolDefinition;
import com.github.star.mcp.core.registry.ToolRegistry;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultToolRegistry implements ToolRegistry {

    private final Map<String, ToolDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<String, Object> beans = new ConcurrentHashMap<>();
    private final Map<String, Method> methods = new ConcurrentHashMap<>();

    @Override
    public void register(ToolDefinition definition, Object bean, Method method) {
        definitions.put(definition.getName(), definition);
        beans.put(definition.getName(), bean);
        methods.put(definition.getName(), method);
    }

    @Override
    public Optional<ToolDefinition> getDefinition(String toolName) {
        return Optional.ofNullable(definitions.get(toolName));
    }

    @Override
    public Optional<Object> getBean(String toolName) {
        return Optional.ofNullable(beans.get(toolName));
    }

    @Override
    public Optional<Method> getMethod(String toolName) {
        return Optional.ofNullable(methods.get(toolName));
    }

    @Override
    public List<ToolDefinition> getAllDefinitions() {
        return new ArrayList<>(definitions.values());
    }

    @Override
    public boolean contains(String toolName) {
        return definitions.containsKey(toolName);
    }
}
