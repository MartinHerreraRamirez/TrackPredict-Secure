package com.trackpredict.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long empresaId;
    private String email;
    private String passwordHash;
    private String nombre;
    private String rol;
    private boolean activo;

    protected Usuario() {}   // JPA

    /** Usuario nuevo y activo. La contrasena llega ya hasheada (BCrypt). */
    public Usuario(Long empresaId, String email, String passwordHash, String nombre, String rol) {
        this.empresaId = empresaId; this.email = email; this.passwordHash = passwordHash;
        this.nombre = nombre; this.rol = rol; this.activo = true;
    }

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getNombre() { return nombre; }
    public String getRol() { return rol; }
    public boolean isActivo() { return activo; }
    public void setNombre(String v) { this.nombre = v; }
    public void setRol(String v) { this.rol = v; }
    public void setActivo(boolean v) { this.activo = v; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
}
