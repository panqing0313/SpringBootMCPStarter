
package com.github.star.mcp.core.model;

public class ToolParameter {

    private String name;
    private String description;
    private boolean required;
    private String type;
    private String defaultValue;

    public ToolParameter() {
    }

    public ToolParameter(String name, String description, boolean required, String type, String defaultValue) {
        this.name = name;
        this.description = description;
        this.required = required;
        this.type = type;
        this.defaultValue = defaultValue;
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

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
