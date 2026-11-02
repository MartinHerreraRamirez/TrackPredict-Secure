package com.trackpredict.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "viaje")
public class Viaje {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long empresaId;
    private Long vehiculoId;
    private Long conductorId;
    private Long rutaId;
    private String estado = ViajeEstado.PLANIFICADO;
    private Instant inicioPlan;
    private Instant finPlan;
    private Instant inicioReal;
    private Instant finReal;

    public Viaje() {}
    public Viaje(Long empresaId, Long vehiculoId, Long conductorId, Long rutaId, Instant inicioPlan, Instant finPlan) {
        this.empresaId = empresaId; this.vehiculoId = vehiculoId; this.conductorId = conductorId;
        this.rutaId = rutaId; this.inicioPlan = inicioPlan; this.finPlan = finPlan;
    }

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public Long getVehiculoId() { return vehiculoId; }
    public Long getConductorId() { return conductorId; }
    public Long getRutaId() { return rutaId; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { this.estado = v; }
    public Instant getInicioPlan() { return inicioPlan; }
    public Instant getFinPlan() { return finPlan; }
    public void setFinPlan(Instant v) { this.finPlan = v; }
    public Instant getInicioReal() { return inicioReal; }
    public void setInicioReal(Instant v) { this.inicioReal = v; }
    public Instant getFinReal() { return finReal; }
    public void setFinReal(Instant v) { this.finReal = v; }
}
