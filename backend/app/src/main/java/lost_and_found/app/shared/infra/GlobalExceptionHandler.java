package lost_and_found.app.shared.infra;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;
import lost_and_found.app.exceptions.AccountVerificationError;
import lost_and_found.app.exceptions.ClaimNotExistException;
import lost_and_found.app.exceptions.InvalidTokenException;
import lost_and_found.app.exceptions.ItemNotFoundException;
import lost_and_found.app.exceptions.MalformedLoginRequestError;
import lost_and_found.app.exceptions.MalformedRegistrationRequestError;
import lost_and_found.app.exceptions.UserMismatchException;
import lost_and_found.app.exceptions.UserNotFoundException;
import lost_and_found.app.exceptions.UserExistsException;
import lost_and_found.app.shared.domain.ApiError;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiError(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "User Not Found",
                e.getMessage()
            ));
    }

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ApiError> handleItemNotFound(ItemNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiError(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "Item Not Found",
                e.getMessage()
            ));
    }

    @ExceptionHandler(ClaimNotExistException.class)
    public ResponseEntity<ApiError> handleClaimNotFound(ClaimNotExistException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiError(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "Claim Not Found",
                e.getMessage()
            ));
    }

    @ExceptionHandler(UserMismatchException.class)
    public ResponseEntity<ApiError> handleUserMismatch(UserMismatchException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ApiError(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                e.getMessage()
            ));
    }

    @ExceptionHandler(UserExistsException.class)
    public ResponseEntity<ApiError> handleUserExists(UserExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ApiError(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "Conflict",
                e.getMessage()
            ));
    }

    @ExceptionHandler({
        MalformedLoginRequestError.class,
        MalformedRegistrationRequestError.class,
        InvalidTokenException.class
    })
    public ResponseEntity<ApiError> handleAuthErrors(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                e.getMessage()
            ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ApiError(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                e.getMessage()
            ));
    }

    @ExceptionHandler(AccountVerificationError.class)
    public ResponseEntity<ApiError> handleUnverified(AccountVerificationError e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ApiError(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                "Account Not Verified",
                e.getMessage()
            ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                e.getMessage()
            ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                e.getMessage()
            ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().stream()
            .map(error -> error instanceof FieldError fieldError
                ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                : error.getDefaultMessage())
            .collect(Collectors.joining("; "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message
            ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Request body is missing or malformed."
            ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception e) {
        log.error("Unhandled exception occurred", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiError(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred."
            ));
    }
}
