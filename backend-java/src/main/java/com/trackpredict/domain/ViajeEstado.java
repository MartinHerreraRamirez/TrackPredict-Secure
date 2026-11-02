package com.trackpredict.domain;

/** Maquina de estados de un viaje: PLANIFICADO -> EN_CURSO -> FINALIZADO (o CANCELADO desde los dos primeros). */
public final class ViajeEstado {
    public static final String PLANIFICADO = "PLANIFICADO";
    public static final String EN_CURSO = "EN_CURSO";
    public static final String FINALIZADO = "FINALIZADO";
    public static final String CANCELADO = "CANCELADO";

    private ViajeEstado() {}

    public static boolean transicionValida(String desde, String hasta) {
        return switch (desde) {
            case PLANIFICADO -> EN_CURSO.equals(hasta) || CANCELADO.equals(hasta);
            case EN_CURSO -> FINALIZADO.equals(hasta) || CANCELADO.equals(hasta);
            default -> false;   // FINALIZADO y CANCELADO son estados finales
        };
    }
}
