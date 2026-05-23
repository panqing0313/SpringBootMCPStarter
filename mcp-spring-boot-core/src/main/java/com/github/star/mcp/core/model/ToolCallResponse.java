
package com.github.star.mcp.core.model;

public class ToolCallResponse {

    private boolean success;
    private Object result;
    private String error;

    public ToolCallResponse() {
    }

    public ToolCallResponse(boolean success, Object result, String error) {
        this.success = success;
        this.result = result;
        this.error = error;
    }

    public static ToolCallResponse success(Object result) {
        return new ToolCallResponse(true, result, null);
    }

    public static ToolCallResponse error(String error) {
        return new ToolCallResponse(false, null, error);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
