package com.aibankingagent.ai_banking_agent.exception;

import com.aibankingagent.ai_banking_agent.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Single source of truth for HTTP error shapes. Every handler returns an
 * {@link ErrorResponse} with a stable code, the request id, and the
 * timestamp so the frontend can render one error format consistently.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(FileTooLargeException.class)
    public ResponseEntity<ErrorResponse> handleFileTooLarge(
            FileTooLargeException ex, HttpServletRequest req) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", ex.getMessage(), req);
    }

    /**
     * Triggered by Tomcat/Spring when a multipart upload exceeds the
     * multipart limit declared in {@code application.properties}. We map
     * it to the same code/path as our own size check.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUpload(
            MaxUploadSizeExceededException ex, HttpServletRequest req) {
        long bytes = ex.getMaxUploadSize();
        String msg = bytes > 0
                ? "Uploaded file exceeds the configured limit of " + bytes + " bytes"
                : "Uploaded file exceeds the configured size limit";
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", msg, req);
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedType(
            UnsupportedFileTypeException ex, HttpServletRequest req) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_FILE_TYPE",
                ex.getMessage(), req);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalid(
            InvalidRequestException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage(), req);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage(), req);
    }

    @ExceptionHandler(LlmUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleLlm(
            LlmUnavailableException ex, HttpServletRequest req) {
        // Log the cause at WARN — operators need it, the user does not.
        log.warn("LLM call failed: {}", ex.getMessage(), ex.getCause());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "LLM_UNAVAILABLE",
                "Service is temporarily unavailable. Please try again later.", req);
    }

    /**
     * Last-resort handler. Logs the full stack at ERROR with the request id
     * so ops can correlate, but never leaks the stack to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception ex, HttpServletRequest req) {
        String reqId = (String) req.getAttribute("requestId");
        log.error("Unhandled exception (requestId={}): {}", reqId, ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred.", req);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code,
                                                String message, HttpServletRequest req) {
        String reqId = (String) req.getAttribute("requestId");
        ErrorResponse body = new ErrorResponse(code, message, reqId);
        return ResponseEntity.status(status).body(body);
    }
}
