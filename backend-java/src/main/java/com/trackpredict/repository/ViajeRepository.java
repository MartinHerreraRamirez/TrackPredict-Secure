package com.trackpredict.repository;

import com.trackpredict.domain.Viaje;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ViajeRepository extends JpaRepository<Viaje, Long> {
    List<Viaje> findTop50ByEmpresaIdOrderByIdDesc(Long empresaId);
    List<Viaje> findByEmpresaIdAndEstadoOrderByIdDesc(Long empresaId, String estado);
    Optional<Viaje> findByIdAndEmpresaId(Long id, Long empresaId);
    Optional<Viaje> findFirstByVehiculoIdAndEmpresaIdAndEstado(Long vehiculoId, Long empresaId, String estado);
}
