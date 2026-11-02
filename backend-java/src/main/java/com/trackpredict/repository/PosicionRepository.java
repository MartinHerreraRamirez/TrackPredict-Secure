package com.trackpredict.repository;

import com.trackpredict.domain.Posicion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PosicionRepository extends JpaRepository<Posicion, Long> {
    List<Posicion> findByVehiculoIdAndEmpresaIdOrderByTsDesc(Long vehiculoId, Long empresaId, Pageable page);

    @Query("select p from Posicion p where p.id in " +
           "(select max(p2.id) from Posicion p2 where p2.empresaId = :empresaId group by p2.vehiculoId)")
    List<Posicion> ultimasPorVehiculo(@Param("empresaId") Long empresaId);
}
