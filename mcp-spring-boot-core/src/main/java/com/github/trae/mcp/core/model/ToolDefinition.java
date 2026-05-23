
package com.github.trae.mcp.core.model;

import java.util.List;

public class ToolDefinition {

    private String name;
    private String description;
    private boolean streaming;
    private List<ToolParameter> parameters;

    public ToolDefinition() {
    }

    public ToolDefinition(String name, String description, boolean streaming, List<ToolParameter> parameters) {
        this.name = name;
        this.description = description;
        this.streaming = streaming;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public void setStreaming(boolean streaming) {
        this.streaming = streaming;
    }

    public List<ToolParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<ToolParameter> parameters) {
        this.parameters = parameters;
    }
}
