package nz.co.westpac.poc.error;

import java.util.LinkedHashMap;
import java.util.Map;
import nz.co.westpac.poc.entity.validator.InvalidAccountException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidAccountException.class)
    ProblemDetail handleInvalidAccount(InvalidAccountException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid account", ex.getMessage());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    ProblemDetail handleAccountNotFound(AccountNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Savings bank account not found", ex.getMessage());
    }

    @ExceptionHandler(AccountLimitExceededException.class)
    ProblemDetail handleAccountLimit(AccountLimitExceededException ex) {
        return problem(HttpStatus.CONFLICT, "Savings bank account limit reached", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Invalid request", "Request validation failed");
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", "Request body is missing or malformed JSON");
    }

    @ExceptionHandler({DataAccessResourceFailureException.class, CannotCreateTransactionException.class})
    ProblemDetail handleDatabaseUnavailable(Exception ex) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Database unavailable",
                "The database is currently unavailable, please retry later");
    }

    @ExceptionHandler(DataAccessException.class)
    ProblemDetail handleDataAccess(DataAccessException ex) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Database error",
                "The request could not be completed because of a database error");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
