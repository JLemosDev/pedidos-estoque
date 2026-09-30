package dev.jlemos.estoque.repository;

import dev.jlemos.estoque.model.Pedido;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
  List<Pedido> findAllByOrderByCriadoEmDesc();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Pedido p where p.id=:id")
  Optional<Pedido> bloquear(@Param("id") Long id);
}
