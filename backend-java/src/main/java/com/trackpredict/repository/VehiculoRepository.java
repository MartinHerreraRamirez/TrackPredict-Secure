package com.trackpredict.repository;

import com.trackpredict.domain.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/** Todas las consultas filtran por empresaId: aislamiento multi-tenant. */
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    List<Vehiculo> findByEmpresaIdAndActivoTrueOrderByPatente(Long empresaId);
    Optional<Vehiculo> findByIdAndEmpresaIdAndActivoTrue(Long id, Long empresaId);
    Optional<Vehiculo> findByEmpresaIdAndPatente(Long empresaId, String patente);
    boolean existsByEmpresaIdAndPatente(Long empresaId, String patente);
}
