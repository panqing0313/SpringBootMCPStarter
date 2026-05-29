package com.github.star.mcp.autoconfigure;

import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class McpKeepAliveTask implements SmartLifecycle {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final CountDownLatch latch;

    public McpKeepAliveTask(CountDownLatch mcpShutdownLatch) {
        this.latch = mcpShutdownLatch;
    }

    @Override
    public void start() {
        running.set(true);
    }

    @Override
    public void stop() {
        running.set(false);
        latch.countDown();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }
}
