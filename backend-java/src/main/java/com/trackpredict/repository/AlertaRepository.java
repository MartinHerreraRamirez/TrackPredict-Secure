package com.trackpredict.repository;

import com.trackpredict.domain.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {
    List<Alerta> findTop100ByEmpresaIdOrderByCreadaEnDesc(Long empresaId);
    Optional<Alerta> findByIdAndEmpresaId(Long id, Long empresaId);
    /** Hay una alerta de ese tipo todavia abierta (en cualquier estado menos el indicado, que es CERRADA). */
    boolean existsByVehiculoIdAndTipoAndEstadoNot(Long vehiculoId, String tipo, String estado);
    /** Hubo una alerta de ese tipo desde cierto instante, en el estado que sea (evita que reaparezca al cerrarla). */
    boolean existsByVehiculoIdAndTipoAndCreadaEnAfter(Long vehiculoId, String tipo, Instant desde);
}
