package dev.jlemos.estoque.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "pedidos")
public class Pedido {

  public enum Status {
    RASCUNHO,
    CONFIRMADO,
    CANCELADO,
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 80)
  private String cliente;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.RASCUNHO;

  @Column(name = "criado_em", nullable = false)
  private Instant criadoEm = Instant.now();

  @OneToMany(
    mappedBy = "pedido",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  private List<PedidoItem> itens = new ArrayList<>();

  protected Pedido() {}

  public Pedido(String cliente) {
    this.cliente = cliente;
  }

  public Long getId() {
    return id;
  }

  public String getCliente() {
    return cliente;
  }

  public Status getStatus() {
    return status;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  public List<PedidoItem> getItens() {
    return Collections.unmodifiableList(itens);
  }

  public void adicionar(Produto produto, int quantidade) {
    itens.add(new PedidoItem(this, produto, quantidade));
  }

  public void confirmar() {
    status = Status.CONFIRMADO;
  }

  public void cancelar() {
    status = Status.CANCELADO;
  }

  public BigDecimal getTotal() {
    return itens
      .stream()
      .map(PedidoItem::getSubtotal)
      .reduce(BigDecimal.ZERO, BigDecimal::add)
      .setScale(2);
  }
}
