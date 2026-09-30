package dev.jlemos.estoque.repository;

import dev.jlemos.estoque.model.Movimentacao;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoRepository
  extends JpaRepository<Movimentacao, Long>
{
  List<Movimentacao> findTop100ByOrderByCriadoEmDesc();
}
