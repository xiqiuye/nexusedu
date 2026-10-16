package com.xqy.nexusedu.common.exception;

public class SystemAuthenticationException extends RuntimeException {
    public SystemAuthenticationException() { super("invalid credentials, session, or token"); }
}
