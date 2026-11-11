package com.estebanmb18.labrecorridomaps;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQ_UBICACION = 101;
    // Punto de prueba cerca de Universidad Piloto de Colombia (NO GPS real).
    private static final double DEMO_LAT = 4.6281;
    private static final double DEMO_LON = -74.0648;

    private final List<Tutor> tutores = Arrays.asList(
            new Tutor("Ana Torres", "Programación", 4.6289, -74.0660),
            new Tutor("Carlos Pérez", "Bases de datos", 4.6343, -74.0638),
            new Tutor("Laura Gómez", "Redes", 4.6244, -74.0686),
            new Tutor("Andrés Rodríguez", "Matemáticas", 4.6370, -74.0580)
    );

    private double latActual = DEMO_LAT;
    private double lonActual = DEMO_LON;
    private boolean gpsReal = false;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private TextView txtEstadoUbicacion;
    private TextView txtTutorCercano;
    private LinearLayout contenedorTutores;
    private Spinner spinnerTransporte;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtEstadoUbicacion = findViewById(R.id.txtEstadoUbicacion);
        txtTutorCercano = findViewById(R.id.txtTutorCercano);
        contenedorTutores = findViewById(R.id.contenedorTutores);
        spinnerTransporte = findViewById(R.id.spinnerTransporte);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        String[] transportes = {"Caminando", "Automóvil", "Bicicleta"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, transportes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTransporte.setAdapter(adapter);

        findViewById(R.id.btnObtenerGps).setOnClickListener(v -> obtenerUbicacionGps());
        findViewById(R.id.btnDemo).setOnClickListener(v -> activarDemostracion());
        findViewById(R.id.btnCercano).setOnClickListener(v -> irAlMasCercano());
        findViewById(R.id.btnRutaCompleta).setOnClickListener(v -> planearRecorrido());

        actualizarPantalla();
    }

    private void activarDemostracion() {
        detenerEscuchaGps();
        latActual = DEMO_LAT;
        lonActual = DEMO_LON;
        gpsReal = false;
        actualizarPantalla();
        Toast.makeText(this, "Ubicación de demostración activada", Toast.LENGTH_SHORT).show();
    }

    private void obtenerUbicacionGps() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_UBICACION);
            return;
        }
        if (locationManager == null) {
            mensaje("Servicio de ubicación no disponible");
            return;
        }
        boolean gpsActivo = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean redActiva = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gpsActivo && !redActiva) {
            Toast.makeText(this, "Activa la ubicación del dispositivo", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }

        // Primero intenta una ubicación previamente conocida; después solicita una nueva.
        Location ultima = null;
        try {
            if (gpsActivo) ultima = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (ultima == null && redActiva) ultima = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException ignored) { }
        if (ultima != null) aplicarUbicacion(ultima);

        detenerEscuchaGps();
        locationListener = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                aplicarUbicacion(location);
                detenerEscuchaGps();
            }
            @Override public void onProviderDisabled(String provider) { }
            @Override public void onProviderEnabled(String provider) { }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
        };
        try {
            String proveedor = gpsActivo ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
            locationManager.requestLocationUpdates(proveedor, 1000L, 1f, locationListener);
            Toast.makeText(this, "Buscando ubicación actual…", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            mensaje("Permisos de ubicación insuficientes");
        }
    }

    private void aplicarUbicacion(Location loc) {
        latActual = loc.getLatitude();
        lonActual = loc.getLongitude();
        gpsReal = true;
        actualizarPantalla();
    }

    private void detenerEscuchaGps() {
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
            locationListener = null;
        }
    }

    @Override protected void onDestroy() {
        detenerEscuchaGps();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_UBICACION) {
            boolean permitido = false;
            for (int resultado : grantResults) {
                if (resultado == PackageManager.PERMISSION_GRANTED) permitido = true;
            }
            if (permitido) obtenerUbicacionGps();
            else mensaje("Permiso denegado. Puedes usar el modo demostración.");
        }
    }

    private void actualizarPantalla() {
        txtEstadoUbicacion.setText(String.format(Locale.US,
                "%s • Latitud %.5f / Longitud %.5f",
                gpsReal ? "GPS del dispositivo" : "MODO DEMOSTRACIÓN (Bogotá)", latActual, lonActual));
        Tutor masCercano = LocationUtils.masCercano(latActual, lonActual, tutores);
        float distancia = LocationUtils.distanciaMetros(latActual, lonActual,
                masCercano.getLatitud(), masCercano.getLongitud());
        txtTutorCercano.setText("Tutor más cercano (distancia directa): " + masCercano.getNombre()
                + " • " + LocationUtils.formatoDistancia(distancia));
        pintarTutores();
    }

    private void pintarTutores() {
        contenedorTutores.removeAllViews();
        for (Tutor tutor : tutores) {
            LinearLayout tarjeta = new LinearLayout(this);
            tarjeta.setOrientation(LinearLayout.VERTICAL);
            tarjeta.setPadding(dp(16), dp(14), dp(16), dp(14));
            tarjeta.setBackgroundResource(R.drawable.card_background);
            LinearLayout.LayoutParams tarLp = new LinearLayout.LayoutParams(-1, -2);
            tarLp.setMargins(dp(16), dp(8), dp(16), 0);
            contenedorTutores.addView(tarjeta, tarLp);

            TextView titulo = texto(tutor.getNombre(), 18, 0xFF163B6D);
            titulo.setTypeface(null, 1);
            tarjeta.addView(titulo);
            float distancia = LocationUtils.distanciaMetros(latActual, lonActual,
                    tutor.getLatitud(), tutor.getLongitud());
            tarjeta.addView(texto(tutor.getMateria() + "  ·  " + LocationUtils.formatoDistancia(distancia)
                    + " (aprox. en línea recta)", 13, 0xFF53677E));
            tarjeta.addView(texto(String.format(Locale.US, "%.5f, %.5f", tutor.getLatitud(), tutor.getLongitud()),
                    12, 0xFF72849A));

            LinearLayout botones = new LinearLayout(this);
            botones.setOrientation(LinearLayout.HORIZONTAL);
            botones.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
            bp.topMargin = dp(7);
            tarjeta.addView(botones, bp);
            Button ver = boton("Ver ubicación");
            Button ir = boton("Cómo llegar");
            botones.addView(ver, new LinearLayout.LayoutParams(0, dp(48), 1));
            botones.addView(ir, new LinearLayout.LayoutParams(0, dp(48), 1));
            ver.setOnClickListener(v -> mostrarTutor(tutor));
            ir.setOnClickListener(v -> navegarTutor(tutor));
        }
    }

    private TextView texto(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        return view;
    }

    private Button boton(String value) {
        Button b = new Button(this);
        b.setText(value);
        
        b.setTextSize(12);
        return b;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String transporte() {
        int selected = spinnerTransporte.getSelectedItemPosition();
        if (selected == 1) return "driving";
        if (selected == 2) return "bicycling";
        return "walking";
    }

    /** Requerimiento 2: ubica cada docente en Google Maps. */
    private void mostrarTutor(Tutor tutor) {
        String coords = coordenadas(tutor.getLatitud(), tutor.getLongitud());
        Uri uri = Uri.parse("geo:" + coords + "?q=" + coords + "(" + Uri.encode(tutor.getNombre()) + ")");
        Uri fallback = new Uri.Builder().scheme("https").authority("www.google.com")
                .appendPath("maps").appendPath("search").appendPath("")
                .appendQueryParameter("api", "1").appendQueryParameter("query", coords).build();
        abrirMaps(uri, fallback);
    }

    /** Ruta con origen en la ubicación actual y destino en un docente. */
    private void navegarTutor(Tutor tutor) {
        Uri url = new Uri.Builder().scheme("https").authority("www.google.com")
                .appendPath("maps").appendPath("dir").appendPath("")
                .appendQueryParameter("api", "1")
                .appendQueryParameter("origin", coordenadas(latActual, lonActual))
                .appendQueryParameter("destination", coordenadas(tutor.getLatitud(), tutor.getLongitud()))
                .appendQueryParameter("travelmode", transporte()).build();
        abrirMaps(url, url);
    }

    /** Requerimiento 3: punto más cercano calculado por distancia geográfica. */
    private void irAlMasCercano() {
        navegarTutor(LocationUtils.masCercano(latActual, lonActual, tutores));
    }

    /** Requerimiento 3: todos los tutores; orden heurístico y waypoints. */
    private void planearRecorrido() {
        List<Tutor> orden = LocationUtils.ordenSugerido(latActual, lonActual, tutores);
        if (orden.isEmpty()) return;
        Tutor destino = orden.get(orden.size() - 1);
        Uri.Builder builder = new Uri.Builder().scheme("https").authority("www.google.com")
                .appendPath("maps").appendPath("dir").appendPath("")
                .appendQueryParameter("api", "1")
                .appendQueryParameter("origin", coordenadas(latActual, lonActual))
                .appendQueryParameter("destination", coordenadas(destino.getLatitud(), destino.getLongitud()))
                .appendQueryParameter("travelmode", transporte());
        List<String> paradas = new ArrayList<>();
        for (int i = 0; i < orden.size() - 1; i++) {
            paradas.add(coordenadas(orden.get(i).getLatitud(), orden.get(i).getLongitud()));
        }
        if (!paradas.isEmpty()) builder.appendQueryParameter("waypoints", String.join("|", paradas));
        Uri url = builder.build();
        abrirMaps(url, url);
    }

    /** Requerimiento 1: ACTION_VIEW, paquete Google Maps, alternativa web. */
    private void abrirMaps(Uri uri, Uri fallbackWeb) {
        Intent google = new Intent(Intent.ACTION_VIEW, uri);
        google.setPackage("com.google.android.apps.maps");
        try {
            startActivity(google);
            return;
        } catch (ActivityNotFoundException ignored) { }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, fallbackWeb));
        } catch (ActivityNotFoundException e) {
            mensaje("No hay Google Maps ni navegador disponible");
        }
    }

    private String coordenadas(double lat, double lon) {
        return String.format(Locale.US, "%.6f,%.6f", lat, lon);
    }

    private void mensaje(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }
}

