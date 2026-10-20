package com.xqy.nexusedu.shared.kernel.application.exception;

public class ManagementAccessDeniedException extends RuntimeException {
    public ManagementAccessDeniedException() { super("Access denied"); }
}
