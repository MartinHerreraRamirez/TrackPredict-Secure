package com.trackpredict.repository;

import com.trackpredict.domain.Dispositivo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {
    Optional<Dispositivo> findByApiKeyHashAndActivoTrue(String apiKeyHash);
}
