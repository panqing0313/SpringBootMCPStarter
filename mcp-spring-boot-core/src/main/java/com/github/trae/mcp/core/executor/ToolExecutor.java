
package com.github.trae.mcp.core.executor;

import com.github.trae.mcp.core.model.ToolCallRequest;
import com.github.trae.mcp.core.model.ToolCallResponse;
import com.github.trae.mcp.core.model.ToolDefinition;

import java.util.List;

public interface ToolExecutor {

    List<ToolDefinition> listTools();

    ToolCallResponse execute(ToolCallRequest request);
}
