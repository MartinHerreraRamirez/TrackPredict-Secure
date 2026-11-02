package com.trackpredict.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "posicion")
public class Posicion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long empresaId;
    private Long vehiculoId;
    private Long viajeId;
    private Instant ts;
    private double lat;
    private double lon;
    private double velocidadKmh;

    public Posicion() {}
    public Posicion(Long empresaId, Long vehiculoId, Instant ts, double lat, double lon, double velocidadKmh) {
        this.empresaId = empresaId; this.vehiculoId = vehiculoId; this.ts = ts;
        this.lat = lat; this.lon = lon; this.velocidadKmh = velocidadKmh;
    }

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public Long getVehiculoId() { return vehiculoId; }
    public Long getViajeId() { return viajeId; }
    public void setViajeId(Long viajeId) { this.viajeId = viajeId; }
    public Instant getTs() { return ts; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public double getVelocidadKmh() { return velocidadKmh; }
}
