package com.trackpredict.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "dispositivo")
public class Dispositivo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long empresaId;
    private Long vehiculoId;
    private String apiKeyHash;
    private boolean activo;

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public Long getVehiculoId() { return vehiculoId; }
    public String getApiKeyHash() { return apiKeyHash; }
    public boolean isActivo() { return activo; }
}
