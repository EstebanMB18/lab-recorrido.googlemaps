package com.estebanmb18.labrecorridomaps;

import android.location.Location;
import java.util.ArrayList;
import java.util.List;

/** Cálculos locales: distancias geográficas, no tiempos de recorrido por vías. */
public final class LocationUtils {
    private LocationUtils() { }

    public static float distanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        float[] resultado = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, resultado);
        return resultado[0];
    }

    public static Tutor masCercano(double lat, double lon, List<Tutor> tutores) {
        Tutor mejor = null;
        float distanciaMinima = Float.MAX_VALUE;
        for (Tutor tutor : tutores) {
            float distancia = distanciaMetros(lat, lon, tutor.getLatitud(), tutor.getLongitud());
            if (distancia < distanciaMinima) {
                mejor = tutor;
                distanciaMinima = distancia;
            }
        }
        return mejor;
    }

    /** Heurística vecino más próximo. No garantiza la ruta vial global óptima. */
    public static List<Tutor> ordenSugerido(double lat, double lon, List<Tutor> tutores) {
        List<Tutor> pendientes = new ArrayList<>(tutores);
        List<Tutor> ordenados = new ArrayList<>();
        double actualLat = lat;
        double actualLon = lon;
        while (!pendientes.isEmpty()) {
            Tutor siguiente = masCercano(actualLat, actualLon, pendientes);
            ordenados.add(siguiente);
            pendientes.remove(siguiente);
            actualLat = siguiente.getLatitud();
            actualLon = siguiente.getLongitud();
        }
        return ordenados;
    }

    public static String formatoDistancia(float metros) {
        if (metros < 1000) return Math.round(metros) + " m";
        return String.format(java.util.Locale.US, "%.2f km", metros / 1000f);
    }
}
