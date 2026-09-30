package dev.jlemos.estoque.service;

import dev.jlemos.estoque.api.Dados.*;
import dev.jlemos.estoque.model.*;
import dev.jlemos.estoque.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PedidoService {

  private final PedidoRepository pedidos;
  private final ProdutoRepository produtos;
  private final MovimentacaoRepository movimentos;
  private final ProdutoService produtoService;

  public PedidoService(
    PedidoRepository pedidos,
    ProdutoRepository produtos,
    MovimentacaoRepository movimentos,
    ProdutoService produtoService
  ) {
    this.pedidos = pedidos;
    this.produtos = produtos;
    this.movimentos = movimentos;
    this.produtoService = produtoService;
  }

  @Transactional(readOnly = true)
  public List<PedidoView> listar() {
    return pedidos
      .findAllByOrderByCriadoEmDesc()
      .stream()
      .map(PedidoView::de)
      .toList();
  }

  public PedidoView criar(NovoPedido dados) {
    if (
      dados.cliente() == null ||
      dados.cliente().trim().length() < 2 ||
      dados.itens() == null ||
      dados.itens().isEmpty()
    ) throw new RegraException(400, "Informe cliente e itens.");
    SortedMap<Long, Integer> itens = new TreeMap<>();
    for (Item item : dados.itens()) {
      if (
        item.produtoId() == null ||
        item.quantidade() < 1 ||
        item.quantidade() > 9999
      ) throw new RegraException(400, "Quantidade de item inválida.");
      int qtd = itens.getOrDefault(item.produtoId(), 0) + item.quantidade();
      if (qtd > 9999) throw new RegraException(
        400,
        "Limite de 9.999 unidades por produto."
      );
      itens.put(item.produtoId(), qtd);
    }
    Pedido p = new Pedido(dados.cliente().trim());
    for (var item : itens.entrySet()) {
      Produto produto = produtos
        .findById(item.getKey())
        .orElseThrow(() -> new RegraException(404, "Produto não encontrado."));
      p.adicionar(produto, item.getValue());
    }
    return PedidoView.de(pedidos.save(p));
  }

  public PedidoView confirmar(Long id) {
    Pedido p = bloquear(id);
    if (p.getStatus() == Pedido.Status.CONFIRMADO) return PedidoView.de(p);
    if (p.getStatus() != Pedido.Status.RASCUNHO) throw new RegraException(
      409,
      "Um pedido cancelado não pode ser confirmado."
    );
    for (PedidoItem item : ordenados(p)) {
      Produto produto = produtoService.bloquear(item.getProdutoId());
      if (produto.getEstoque() < item.getQuantidade()) throw new RegraException(
        409,
        "Estoque insuficiente para " + produto.getNome() + "."
      );
      produto.ajustarEstoque(-item.getQuantidade());
      movimentos.save(
        new Movimentacao(
          produto.getId(),
          p.getId(),
          -item.getQuantidade(),
          "PEDIDO_CONFIRMADO"
        )
      );
    }
    p.confirmar();
    return PedidoView.de(p);
  }

  public PedidoView cancelar(Long id) {
    Pedido p = bloquear(id);
    if (p.getStatus() == Pedido.Status.CANCELADO) return PedidoView.de(p);
    if (
      p.getStatus() == Pedido.Status.CONFIRMADO
    ) for (PedidoItem item : ordenados(p)) {
      Produto produto = produtoService.bloquear(item.getProdutoId());
      if (
        (long) produto.getEstoque() + item.getQuantidade() > 1000000
      ) throw new RegraException(
        409,
        "O cancelamento excederia o saldo máximo de estoque."
      );
      produto.ajustarEstoque(item.getQuantidade());
      movimentos.save(
        new Movimentacao(
          produto.getId(),
          p.getId(),
          item.getQuantidade(),
          "PEDIDO_CANCELADO"
        )
      );
    }
    p.cancelar();
    return PedidoView.de(p);
  }

  private Pedido bloquear(Long id) {
    return pedidos
      .bloquear(id)
      .orElseThrow(() -> new RegraException(404, "Pedido não encontrado."));
  }

  private List<PedidoItem> ordenados(Pedido p) {
    return p
      .getItens()
      .stream()
      .sorted(Comparator.comparing(PedidoItem::getProdutoId))
      .toList();
  }
}
