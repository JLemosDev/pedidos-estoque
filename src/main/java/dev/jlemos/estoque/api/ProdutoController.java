package dev.jlemos.estoque.api;

import dev.jlemos.estoque.api.Dados.*;
import dev.jlemos.estoque.service.ProdutoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

  private final ProdutoService service;

  public ProdutoController(ProdutoService service) {
    this.service = service;
  }

  @GetMapping
  public List<ProdutoView> listar() {
    return service.listar();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProdutoView criar(@Valid @RequestBody NovoProduto d) {
    return service.criar(d);
  }

  @PutMapping("/{id}")
  public ProdutoView editar(
    @PathVariable Long id,
    @Valid @RequestBody EditarProduto d
  ) {
    return service.editar(id, d);
  }

  @PostMapping("/{id}/reposicoes")
  public ProdutoView repor(
    @PathVariable Long id,
    @Valid @RequestBody Reposicao d
  ) {
    return service.repor(id, d.quantidade());
  }

  @GetMapping("/movimentacoes")
  public List<MovimentoView> movimentos() {
    return service.movimentos();
  }
}
