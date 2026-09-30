package dev.jlemos.estoque.config;

import dev.jlemos.estoque.api.Dados.NovoProduto;
import dev.jlemos.estoque.repository.ProdutoRepository;
import dev.jlemos.estoque.service.ProdutoService;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;

@Configuration
@Profile("demo")
public class DadosDemo {

  @Bean
  CommandLineRunner exemplos(ProdutoService service, ProdutoRepository repo) {
    return args -> {
      if (repo.count() == 0) {
        service.criar(
          new NovoProduto(
            "TEC-001",
            "Teclado mecânico",
            new BigDecimal("189.90"),
            18
          )
        );
        service.criar(
          new NovoProduto(
            "MOU-002",
            "Mouse sem fio",
            new BigDecimal("79.90"),
            32
          )
        );
        service.criar(
          new NovoProduto(
            "FON-003",
            "Fone de ouvido",
            new BigDecimal("129.90"),
            4
          )
        );
        service.criar(
          new NovoProduto("CAB-004", "Cabo USB-C", new BigDecimal("29.90"), 45)
        );
      }
    };
  }
}
