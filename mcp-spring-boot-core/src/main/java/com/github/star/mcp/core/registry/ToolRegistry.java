
package com.github.star.mcp.core.registry;

import com.github.star.mcp.core.model.ToolDefinition;

import java.util.List;
import java.util.Optional;

public interface ToolRegistry {

    void register(ToolDefinition definition, Object bean, java.lang.reflect.Method method);

    Optional<ToolDefinition> getDefinition(String toolName);

    Optional<Object> getBean(String toolName);

    Optional<java.lang.reflect.Method> getMethod(String toolName);

    List<ToolDefinition> getAllDefinitions();

    boolean contains(String toolName);
}
