package dev.jlemos.estoque.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "pedido_itens")
public class PedidoItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pedido_id", nullable = false)
  private Pedido pedido;

  @Column(name = "produto_id", nullable = false)
  private Long produtoId;

  @Column(name = "produto_nome", nullable = false, length = 100)
  private String produtoNome;

  @Column(nullable = false)
  private int quantidade;

  @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
  private BigDecimal precoUnitario;

  protected PedidoItem() {}

  public PedidoItem(Pedido pedido, Produto produto, int quantidade) {
    this.pedido = pedido;
    produtoId = produto.getId();
    produtoNome = produto.getNome();
    this.quantidade = quantidade;
    precoUnitario = produto.getPreco();
  }

  public Long getProdutoId() {
    return produtoId;
  }

  public String getProdutoNome() {
    return produtoNome;
  }

  public int getQuantidade() {
    return quantidade;
  }

  public BigDecimal getPrecoUnitario() {
    return precoUnitario;
  }

  public BigDecimal getSubtotal() {
    return precoUnitario.multiply(BigDecimal.valueOf(quantidade)).setScale(2);
  }
}
