package com.xqy.nexusedu.system.application.exception;

public class SystemAuthenticationException extends RuntimeException {
    public SystemAuthenticationException() { super("invalid credentials, session, or token"); }
}
