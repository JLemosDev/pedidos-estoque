package dev.jlemos.estoque.service;

import dev.jlemos.estoque.api.Dados.*;
import dev.jlemos.estoque.model.*;
import dev.jlemos.estoque.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProdutoService {

  private final ProdutoRepository produtos;
  private final MovimentacaoRepository movimentos;

  public ProdutoService(
    ProdutoRepository produtos,
    MovimentacaoRepository movimentos
  ) {
    this.produtos = produtos;
    this.movimentos = movimentos;
  }

  @Transactional(readOnly = true)
  public List<ProdutoView> listar() {
    return produtos
      .findAllByOrderByNomeAsc()
      .stream()
      .map(ProdutoView::de)
      .toList();
  }

  public ProdutoView criar(NovoProduto d) {
    String sku = d.sku().trim().toUpperCase(Locale.ROOT);
    if (produtos.existsBySku(sku)) throw new RegraException(
      409,
      "Já existe um produto com esse código."
    );
    Produto p = produtos.save(
      new Produto(sku, d.nome().trim(), d.preco(), d.estoque())
    );
    if (d.estoque() > 0) movimentos.save(
      new Movimentacao(p.getId(), null, d.estoque(), "SALDO_INICIAL")
    );
    return ProdutoView.de(p);
  }

  public ProdutoView editar(Long id, EditarProduto d) {
    Produto p = bloquear(id);
    p.atualizar(d.nome().trim(), d.preco());
    return ProdutoView.de(p);
  }

  public ProdutoView repor(Long id, int quantidade) {
    if (quantidade < 1 || quantidade > 1000000) throw new RegraException(
      400,
      "Quantidade de reposição inválida."
    );
    Produto p = bloquear(id);
    if ((long) p.getEstoque() + quantidade > 1000000) throw new RegraException(
      409,
      "O saldo máximo é de 1.000.000 unidades."
    );
    p.ajustarEstoque(quantidade);
    movimentos.save(new Movimentacao(id, null, quantidade, "REPOSICAO"));
    return ProdutoView.de(p);
  }

  public Produto bloquear(Long id) {
    return produtos
      .bloquear(id)
      .orElseThrow(() -> new RegraException(404, "Produto não encontrado."));
  }

  @Transactional(readOnly = true)
  public List<MovimentoView> movimentos() {
    return movimentos
      .findTop100ByOrderByCriadoEmDesc()
      .stream()
      .map(MovimentoView::de)
      .toList();
  }
}
