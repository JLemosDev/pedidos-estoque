package dev.jlemos.estoque.api;

import dev.jlemos.estoque.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class Dados {

  private Dados() {}

  public record NovoProduto(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{2,40}") String sku,
    @NotBlank @Size(min = 2, max = 100) String nome,
    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    BigDecimal preco,
    @Min(0) @Max(1000000) int estoque
  ) {}

  public record EditarProduto(
    @NotBlank @Size(min = 2, max = 100) String nome,
    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    BigDecimal preco
  ) {}

  public record Reposicao(@Min(1) @Max(1000000) int quantidade) {}

  public record Item(
    @NotNull @Positive Long produtoId,
    @Min(1) @Max(9999) int quantidade
  ) {}

  public record NovoPedido(
    @NotBlank @Size(min = 2, max = 80) String cliente,
    @NotEmpty @Size(max = 30) List<@Valid Item> itens
  ) {}

  public record ProdutoView(
    Long id,
    String sku,
    String nome,
    BigDecimal preco,
    int estoque
  ) {
    public static ProdutoView de(Produto p) {
      return new ProdutoView(
        p.getId(),
        p.getSku(),
        p.getNome(),
        p.getPreco(),
        p.getEstoque()
      );
    }
  }

  public record ItemView(
    Long produtoId,
    String produtoNome,
    int quantidade,
    BigDecimal precoUnitario,
    BigDecimal subtotal
  ) {}

  public record PedidoView(
    Long id,
    String cliente,
    Pedido.Status status,
    Instant criadoEm,
    BigDecimal total,
    List<ItemView> itens
  ) {
    public static PedidoView de(Pedido p) {
      return new PedidoView(
        p.getId(),
        p.getCliente(),
        p.getStatus(),
        p.getCriadoEm(),
        p.getTotal(),
        p
          .getItens()
          .stream()
          .map(i ->
            new ItemView(
              i.getProdutoId(),
              i.getProdutoNome(),
              i.getQuantidade(),
              i.getPrecoUnitario(),
              i.getSubtotal()
            )
          )
          .toList()
      );
    }
  }

  public record MovimentoView(
    Long id,
    Long produtoId,
    Long pedidoId,
    int quantidade,
    String motivo,
    Instant criadoEm
  ) {
    public static MovimentoView de(Movimentacao m) {
      return new MovimentoView(
        m.getId(),
        m.getProdutoId(),
        m.getPedidoId(),
        m.getQuantidade(),
        m.getMotivo(),
        m.getCriadoEm()
      );
    }
  }
}
