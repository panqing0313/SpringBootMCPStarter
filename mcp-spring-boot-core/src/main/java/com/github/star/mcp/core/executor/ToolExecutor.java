
package com.github.star.mcp.core.executor;

import com.github.star.mcp.core.model.ToolCallRequest;
import com.github.star.mcp.core.model.ToolCallResponse;
import com.github.star.mcp.core.model.ToolDefinition;

import java.util.List;

public interface ToolExecutor {

    List<ToolDefinition> listTools();

    ToolCallResponse execute(ToolCallRequest request);
}
