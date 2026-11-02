package com.trackpredict.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "alerta")
public class Alerta {
    public static final String EXCESO_VELOCIDAD = "EXCESO_VELOCIDAD";
    public static final String SALIDA_GEOCERCA = "SALIDA_GEOCERCA";
    public static final String PARADA_PROLONGADA = "PARADA_PROLONGADA";
    public static final String ANOMALIA_TRAYECTO = "ANOMALIA_TRAYECTO";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long empresaId;
    private Long vehiculoId;
    private String tipo;
    private String detalle;
    private String estado = "NUEVA";
    private Instant creadaEn = Instant.now();

    public Alerta() {}
    public Alerta(Long empresaId, Long vehiculoId, String tipo, String detalle) {
        this.empresaId = empresaId; this.vehiculoId = vehiculoId; this.tipo = tipo; this.detalle = detalle;
    }

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public Long getVehiculoId() { return vehiculoId; }
    public String getTipo() { return tipo; }
    public String getDetalle() { return detalle; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Instant getCreadaEn() { return creadaEn; }
}
