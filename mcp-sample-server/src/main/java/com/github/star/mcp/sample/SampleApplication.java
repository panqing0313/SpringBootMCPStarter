package com.github.star.mcp.sample;

import com.github.star.mcp.autoconfigure.McpServerRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SampleApplication {

    public static void main(String[] args) {
        McpServerRunner.run(SampleApplication.class, args);
    }
}
