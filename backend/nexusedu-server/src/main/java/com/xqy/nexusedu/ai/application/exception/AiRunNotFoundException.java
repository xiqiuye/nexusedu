package com.xqy.nexusedu.ai.application.exception;

public class AiRunNotFoundException extends RuntimeException {
    public AiRunNotFoundException(String runId) { super("AI task run not found: " + runId); }
}
