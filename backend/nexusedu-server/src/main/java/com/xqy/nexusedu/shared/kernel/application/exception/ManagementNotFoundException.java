package com.xqy.nexusedu.shared.kernel.application.exception;

public class ManagementNotFoundException extends RuntimeException {
    public ManagementNotFoundException() { super("记录不存在或不属于当前范围"); }
}
