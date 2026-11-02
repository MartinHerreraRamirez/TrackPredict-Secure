package com.trackpredict.web;

import com.trackpredict.repository.GeoRepository.Punto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.util.List;

/** Coordenada recibida por la API (validada en rango). */
public record PuntoDto(@DecimalMin("-90") @DecimalMax("90") double lat,
                       @DecimalMin("-180") @DecimalMax("180") double lon) {
    public static List<Punto> aPuntos(List<PuntoDto> dtos) {
        return dtos.stream().map(d -> new Punto(d.lat(), d.lon())).toList();
    }
}
