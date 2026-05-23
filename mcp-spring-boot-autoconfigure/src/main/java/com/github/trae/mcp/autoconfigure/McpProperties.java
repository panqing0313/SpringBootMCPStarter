
package com.github.trae.mcp.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mcp")
public class McpProperties {

    private boolean enabled = true;
    private String basePath = "/mcp";
    private boolean exposeApi = true;
    private boolean streamingEnabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public boolean isExposeApi() {
        return exposeApi;
    }

    public void setExposeApi(boolean exposeApi) {
        this.exposeApi = exposeApi;
    }

    public boolean isStreamingEnabled() {
        return streamingEnabled;
    }

    public void setStreamingEnabled(boolean streamingEnabled) {
        this.streamingEnabled = streamingEnabled;
    }
}
