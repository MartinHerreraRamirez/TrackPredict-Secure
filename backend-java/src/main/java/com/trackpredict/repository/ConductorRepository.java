package com.trackpredict.repository;

import com.trackpredict.domain.Conductor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ConductorRepository extends JpaRepository<Conductor, Long> {
    List<Conductor> findByEmpresaIdAndActivoTrueOrderByNombre(Long empresaId);
    Optional<Conductor> findByIdAndEmpresaIdAndActivoTrue(Long id, Long empresaId);
    Optional<Conductor> findByEmpresaIdAndDocumento(Long empresaId, String documento);
    boolean existsByEmpresaIdAndDocumento(Long empresaId, String documento);
}
