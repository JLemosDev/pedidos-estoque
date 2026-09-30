package dev.jlemos.estoque.api;

import dev.jlemos.estoque.api.Dados.*;
import dev.jlemos.estoque.service.PedidoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

  private final PedidoService service;

  public PedidoController(PedidoService service) {
    this.service = service;
  }

  @GetMapping
  public List<PedidoView> listar() {
    return service.listar();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PedidoView criar(@Valid @RequestBody NovoPedido d) {
    return service.criar(d);
  }

  @PostMapping("/{id}/confirmacao")
  public PedidoView confirmar(@PathVariable Long id) {
    return service.confirmar(id);
  }

  @PostMapping("/{id}/cancelamento")
  public PedidoView cancelar(@PathVariable Long id) {
    return service.cancelar(id);
  }
}
