package dev.jlemos.estoque;

import static org.junit.jupiter.api.Assertions.*;

import dev.jlemos.estoque.api.Dados.*;
import dev.jlemos.estoque.repository.*;
import dev.jlemos.estoque.service.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EstoqueIntegrationTest {

  @Autowired
  ProdutoService produtos;

  @Autowired
  PedidoService pedidos;

  @Autowired
  ProdutoRepository produtoRepository;

  @Autowired
  PedidoRepository pedidoRepository;

  @Autowired
  MovimentacaoRepository movimentoRepository;

  @BeforeEach
  void limpar() {
    movimentoRepository.deleteAll();
    pedidoRepository.deleteAll();
    produtoRepository.deleteAll();
  }

  ProdutoView produto(String sku, int saldo) {
    return produtos.criar(
      new NovoProduto(sku, "Produto " + sku, new BigDecimal("19.90"), saldo)
    );
  }

  PedidoView pedido(ProdutoView produto, int quantidade) {
    return pedidos.criar(
      new NovoPedido(
        "Cliente fictício",
        List.of(new Item(produto.id(), quantidade))
      )
    );
  }

  int saldo(ProdutoView produto) {
    return produtoRepository.findById(produto.id()).orElseThrow().getEstoque();
  }

  @Test
  void confirmarDebitaERepetirNaoDebitaNovamente() {
    var produto = produto("SKU-1", 10);
    var pedido = pedido(produto, 3);
    pedidos.confirmar(pedido.id());
    pedidos.confirmar(pedido.id());
    assertEquals(7, saldo(produto));
    assertEquals(2, movimentoRepository.count());
  }

  @Test
  void cancelarDevolveUmaVez() {
    var produto = produto("SKU-1", 10);
    var pedido = pedido(produto, 3);
    pedidos.confirmar(pedido.id());
    pedidos.cancelar(pedido.id());
    pedidos.cancelar(pedido.id());
    assertEquals(10, saldo(produto));
    assertEquals(3, movimentoRepository.count());
    assertThrows(RegraException.class, () -> pedidos.confirmar(pedido.id()));
  }

  @Test
  void cancelarRascunhoNaoMovimentaSaldo() {
    var produto = produto("SKU-1", 10);
    pedidos.cancelar(pedido(produto, 2).id());
    assertEquals(10, saldo(produto));
    assertEquals(1, movimentoRepository.count());
  }

  @Test
  void saldoInsuficienteReverteTodosOsItens() {
    var primeiro = produto("SKU-1", 10);
    var segundo = produto("SKU-2", 1);
    var pedido = pedidos.criar(
      new NovoPedido(
        "Cliente",
        List.of(new Item(primeiro.id(), 2), new Item(segundo.id(), 2))
      )
    );
    assertThrows(RegraException.class, () -> pedidos.confirmar(pedido.id()));
    assertEquals(10, saldo(primeiro));
    assertEquals(1, saldo(segundo));
    assertEquals("RASCUNHO", pedidos.listar().getFirst().status().name());
    assertEquals(2, movimentoRepository.count());
  }

  @Test
  void itensDuplicadosSaoAgregados() {
    var produto = produto("SKU-1", 10);
    var pedido = pedidos.criar(
      new NovoPedido(
        "Cliente",
        List.of(new Item(produto.id(), 2), new Item(produto.id(), 3))
      )
    );
    assertEquals(1, pedido.itens().size());
    assertEquals(5, pedido.itens().getFirst().quantidade());
    assertEquals(new BigDecimal("99.50"), pedido.total());
  }

  @Test
  void precoDoPedidoPreservaHistorico() {
    var produto = produto("SKU-1", 10);
    var pedido = pedido(produto, 2);
    produtos.editar(
      produto.id(),
      new EditarProduto("Novo nome", new BigDecimal("29.90"))
    );
    var confirmado = pedidos.confirmar(pedido.id());
    assertEquals(new BigDecimal("39.80"), confirmado.total());
    assertEquals("Produto SKU-1", confirmado.itens().getFirst().produtoNome());
  }

  @Test
  void reposicaoGeraRegistro() {
    var produto = produto("SKU-1", 3);
    produtos.repor(produto.id(), 4);
    assertEquals(7, saldo(produto));
    assertEquals("REPOSICAO", produtos.movimentos().getFirst().motivo());
    assertThrows(RegraException.class, () -> produtos.repor(produto.id(), 0));
  }

  @Test
  void codigoDuplicadoEhRejeitado() {
    produto("SKU-1", 1);
    assertThrows(RegraException.class, () -> produto("sku-1", 5));
    assertEquals(1, produtoRepository.count());
  }

  @Test
  void duasVendasConcorrentesNaoVendamAMesmaUltimaUnidade() throws Exception {
    var produto = produto("SKU-1", 1);
    var primeiro = pedido(produto, 1);
    var segundo = pedido(produto, 1);
    var iniciar = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      Callable<Boolean> venda1 = () -> confirmarApos(iniciar, primeiro.id());
      Callable<Boolean> venda2 = () -> confirmarApos(iniciar, segundo.id());
      var a = executor.submit(venda1);
      var b = executor.submit(venda2);
      iniciar.countDown();
      assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
    }
    assertEquals(0, saldo(produto));
    assertEquals(
      1,
      pedidos
        .listar()
        .stream()
        .filter(p -> p.status().name().equals("CONFIRMADO"))
        .count()
    );
    assertEquals(2, movimentoRepository.count());
  }

  boolean confirmarApos(CountDownLatch iniciar, Long id)
    throws InterruptedException {
    iniciar.await();
    try {
      pedidos.confirmar(id);
      return true;
    } catch (RegraException e) {
      return false;
    }
  }
}
