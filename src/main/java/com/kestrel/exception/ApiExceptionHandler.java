
package com.kestrel.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(
                    MethodArgumentNotValidException exception) {

            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                            HttpStatus.BAD_REQUEST,
                            "One or more request fields are invalid.");

            problem.setTitle("Validation failed");

            problem.setProperty(
                            "errors",
                            exception.getBindingResult()
                                            .getFieldErrors()
                                            .stream()
                                            .map(error -> error.getField()
                                                            + ": " + error.getDefaultMessage())
                                            .toList());

            return problem;
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
        DataIntegrityViolationException ex) {

    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    problem.setTitle("Conflict");
    problem.setDetail("A document with this ID already exists.");

    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
}
}
