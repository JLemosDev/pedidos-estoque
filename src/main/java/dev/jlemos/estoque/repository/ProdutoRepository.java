package dev.jlemos.estoque.repository;

import dev.jlemos.estoque.model.Produto;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
  boolean existsBySku(String sku);
  List<Produto> findAllByOrderByNomeAsc();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Produto p where p.id=:id")
  Optional<Produto> bloquear(@Param("id") Long id);
}
