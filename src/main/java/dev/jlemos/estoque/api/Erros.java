package dev.jlemos.estoque.api;

import dev.jlemos.estoque.service.RegraException;
import java.util.Map;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class Erros {

  @ExceptionHandler(RegraException.class)
  public ResponseEntity<?> regra(RegraException e) {
    return ResponseEntity.status(e.getStatus()).body(
      Map.of("error", e.getMessage())
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> validar(MethodArgumentNotValidException e) {
    return ResponseEntity.badRequest().body(
      Map.of(
        "error",
        "Revise os dados informados.",
        "fields",
        e
          .getBindingResult()
          .getFieldErrors()
          .stream()
          .map(x -> x.getField() + ": " + x.getDefaultMessage())
          .toList()
      )
    );
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<?> json() {
    return ResponseEntity.badRequest().body(
      Map.of("error", "Dados ou formato JSON inválidos.")
    );
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<?> conflito() {
    return ResponseEntity.status(409).body(
      Map.of("error", "Os dados conflitam com um registro existente.")
    );
  }

  @ExceptionHandler(PessimisticLockingFailureException.class)
  public ResponseEntity<?> concorrencia() {
    return ResponseEntity.status(409).body(
      Map.of("error", "Outro pedido está sendo processado. Tente novamente.")
    );
  }
}
