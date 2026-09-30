package dev.jlemos.estoque.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "produtos")
public class Produto {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 40)
  private String sku;

  @Column(nullable = false, length = 100)
  private String nome;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal preco;

  @Column(nullable = false)
  private int estoque;

  protected Produto() {}

  public Produto(String sku, String nome, BigDecimal preco, int estoque) {
    this.sku = sku;
    this.nome = nome;
    this.preco = preco;
    this.estoque = estoque;
  }

  public Long getId() {
    return id;
  }

  public String getSku() {
    return sku;
  }

  public String getNome() {
    return nome;
  }

  public BigDecimal getPreco() {
    return preco;
  }

  public int getEstoque() {
    return estoque;
  }

  public void atualizar(String nome, BigDecimal preco) {
    this.nome = nome;
    this.preco = preco;
  }

  public void ajustarEstoque(int quantidade) {
    estoque = Math.addExact(estoque, quantidade);
    if (estoque < 0) throw new IllegalStateException(
      "Estoque não pode ser negativo."
    );
  }
}
