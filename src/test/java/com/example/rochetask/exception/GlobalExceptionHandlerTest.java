package com.example.rochetask.exception;

import com.example.rochetask.controller.DatabaseEntriesController;
import com.example.rochetask.model.Entry;
import com.example.rochetask.model.ErrorMessage;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void shouldReturnNotFound_inCaseOfDataNotFoundException() {
        ResponseEntity<ErrorMessage> responseEntity =
                globalExceptionHandler.handleDataNotFoundException(new DataNotFoundException("detailed error message"));

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isEqualTo("detailed error message");
    }

    @Test
    void shouldReturnBadRequest_inCaseOfTransactionNotFoundException() {
        ResponseEntity<ErrorMessage> responseEntity =
                globalExceptionHandler.handleTransactionNotFoundException(
                        new TransactionNotFoundException("detailed error message"));

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isEqualTo("detailed error message");
    }

    @Test
    void shouldReturnBadRequest_inCaseOfTooManyOpenTransactionsException() {
        ResponseEntity<ErrorMessage> responseEntity =
                globalExceptionHandler.handleTooManyTransactionsOpenException(
                        new TooManyTransactionsOpenException("detailed error message"));

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isEqualTo("detailed error message");
    }

    @Test
    void shouldReturnBadRequest_inCaseOfMethodArgumentNotValidException() throws NoSuchMethodException {
        Entry entry = new Entry("wrong?key", "value");
        BindingResult bindingResult = new BeanPropertyBindingResult(entry, "testObject");
        bindingResult.addError(new FieldError("entry", "key", "error message"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                new MethodParameter(DatabaseEntriesController.class.getMethod("setEntry", Entry.class), 0),
                bindingResult);

        ResponseEntity<ErrorMessage> responseEntity =
                globalExceptionHandler.handleValidationException(exception);

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().message()).isEqualTo("[error message]");
    }

}
