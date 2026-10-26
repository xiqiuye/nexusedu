package com.xqy.nexusedu.shared.kernel.interfaces.web.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.xqy.nexusedu.ai.task.exception.AiRunAccessDeniedException;
import com.xqy.nexusedu.ai.task.exception.AiRunNotFoundException;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementAccessDeniedException;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementConflictException;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementNotFoundException;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallSigner;

/**
 * Stable RFC 9457 error contract for API clients.
 */
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException error) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", error.getMessage());
    }

    @ExceptionHandler({AccessDeniedException.class, InternalCallSigner.InternalCallAuthenticationException.class,
            AiRunAccessDeniedException.class, ManagementAccessDeniedException.class})
    ProblemDetail forbidden(RuntimeException error) {
        return problem(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied");
    }

    @ExceptionHandler(AiRunNotFoundException.class)
    ProblemDetail notFound(AiRunNotFoundException error) {
        return problem(HttpStatus.NOT_FOUND, "RUN_NOT_FOUND", error.getMessage());
    }

    @ExceptionHandler(ManagementNotFoundException.class)
    ProblemDetail resourceNotFound(ManagementNotFoundException error) {
        return problem(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", error.getMessage());
    }

    @ExceptionHandler(ManagementConflictException.class)
    ProblemDetail conflict(ManagementConflictException error) {
        return problem(HttpStatus.CONFLICT, "RESOURCE_CONFLICT", error.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail value = ProblemDetail.forStatusAndDetail(status, detail == null ? status.getReasonPhrase() : detail);
        value.setProperty("code", code);
        return value;
    }
}
