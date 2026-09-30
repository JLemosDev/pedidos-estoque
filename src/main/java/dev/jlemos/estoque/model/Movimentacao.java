package dev.jlemos.estoque.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "movimentacoes")
public class Movimentacao {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "produto_id", nullable = false)
  private Long produtoId;

  @Column(name = "pedido_id")
  private Long pedidoId;

  @Column(nullable = false)
  private int quantidade;

  @Column(nullable = false, length = 40)
  private String motivo;

  @Column(name = "criado_em", nullable = false)
  private Instant criadoEm = Instant.now();

  protected Movimentacao() {}

  public Movimentacao(
    Long produto,
    Long pedido,
    int quantidade,
    String motivo
  ) {
    produtoId = produto;
    pedidoId = pedido;
    this.quantidade = quantidade;
    this.motivo = motivo;
  }

  public Long getId() {
    return id;
  }

  public Long getProdutoId() {
    return produtoId;
  }

  public Long getPedidoId() {
    return pedidoId;
  }

  public int getQuantidade() {
    return quantidade;
  }

  public String getMotivo() {
    return motivo;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }
}
