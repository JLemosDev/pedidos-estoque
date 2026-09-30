package dev.jlemos.estoque.service;

public class RegraException extends RuntimeException {

  private final int status;

  public RegraException(int status, String message) {
    super(message);
    this.status = status;
  }

  public int getStatus() {
    return status;
  }
}
