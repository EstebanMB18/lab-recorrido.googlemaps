# TutorMaps — Laboratorio de Google Maps Intents para Android

**Universidad Piloto de Colombia · Desarrollo de aplicaciones para dispositivos convergentes**  
**Estudiante:** Brandon Esteban Melo Bolaños  
**Repositorio de entrega:** https://github.com/EstebanMB18/lab-recorrido.googlemaps

## 1. Objetivo

Desarrollar una aplicación Android nativa (Java) que muestre la ubicación del estudiante, ubique docentes de prueba, identifique el tutor más cercano y abra **Google Maps mediante intents** para consultar ubicaciones y rutas individuales o con varias paradas. Se implementan los ejemplos de la guía *Google Maps Intents for Android*, en particular `ACTION_VIEW`, `geo:`, `https://www.google.com/maps/dir/?api=1` y `waypoints`.

> **Nota:** La lista de nombres y coordenadas corresponde a datos **ficticios de demostración** en Bogotá. No se gestionan reservas reales de tutorías. Las distancias se calculan en línea recta y la elección del orden del recorrido usa una heurística simple: no se promete que sea el mejor recorrido posible sobre la red vial.

## 2. Requerimientos de la tarea

| Requerimiento | Cómo se cumple | Archivos |
|---|---|---|
| 1. Invocar Google Maps con Intent | `Intent.ACTION_VIEW`, `setPackage("com.google.android.apps.maps")`; fallback navegador | `MainActivity.java` (`abrirMaps`) |
| 2. Señalar la ubicación para cada proyecto/tutor | Ubicación individual por coordenadas `geo:lat,lon?q=...`; listado con latitud/longitud | `MainActivity.java` (`mostrarTutor`, `pintarTutores`) |
| 3. Alternativas de ruta y docentes cercanos | GPS real o simulado, cálculo de distancia, filtro del más cercano, rutas por modo de transporte y recorrido con `waypoints` | `LocationUtils.java`, `MainActivity.java` |
| Entrega | URL del repositorio con proyecto y documentación | GitHub y este README |

## 3. Tecnologías y requisitos

- Android Studio con Android SDK Platform **API 35** instalado.
- JDK **17** para Gradle (normalmente el Embedded JDK de Android Studio).
- Android Gradle Plugin **8.7.3** y Gradle **8.9**.
- Lenguaje Java 17; `minSdk 24` (Android 7.0); `targetSdk 34`.
- Un teléfono Android con Google Maps o emulador Android con imagen de Google Play (para abrir Maps). Si Maps no está instalado, se intenta abrir el navegador.
- Conexión a Internet para descargar Gradle/dependencias y para consultar mapas. **No necesita API Key**: se abre Google Maps como aplicación externa.

## 4. Estructura del proyecto

```text
lab-recorrido-googlemaps/
├── app/
│   ├── build.gradle                         # SDK, aplicación, Java
│   └── src/main/
│       ├── AndroidManifest.xml              # Permisos, Activity principal, Maps
│       ├── java/com/estebanmb18/labrecorridomaps/
│       │   ├── MainActivity.java            # Pantalla, GPS, botones, Intents de Maps
│       │   ├── Tutor.java                   # Modelo: nombre, materia, coordenadas
│       │   └── LocationUtils.java           # Distancias y orden sugerido
│       └── res/
│           ├── layout/activity_main.xml     # Interfaz de usuario
│           ├── drawable/                    # Fondos de tarjetas y encabezado
│           └── values/styles.xml            # Tema
├── gradle/wrapper/
│   ├── gradle-wrapper.properties            # Distribución oficial Gradle 8.9
│   └── gradle-wrapper.jar                   # Arranque del wrapper
├── gradlew / gradlew.bat                    # Arranque Gradle
├── build.gradle                             # Plugin Android
├── settings.gradle                          # Repositorios y módulo app
├── gradle.properties                        # Parámetros del build
├── .gitignore                               # Ignora carpetas generadas
└── README.md                                # Este documento
```

## 5. Cómo abrirlo en Windows — C:\Dev

1. Descarga y descomprime el ZIP. La carpeta resultante debe quedar en **`C:\Dev\lab-recorrido-googlemaps`**.
2. Abre **Android Studio** → **Open** → selecciona la **carpeta raíz** `C:\Dev\lab-recorrido-googlemaps` (no selecciones solo `app`).
3. Espera el **Gradle Sync**. Acepta instalar el Android SDK **35** si Android Studio lo solicita. En **Settings → Build, Execution, Deployment → Build Tools → Gradle**, selecciona **Gradle JDK 17 / Embedded JDK compatible** si fuera necesario.
4. En **Device Manager**, crea un emulador Android (preferiblemente con Google Play), o conecta un celular mediante depuración USB.
5. Selecciona la configuración **app** y presiona **Run ▶**.
6. Al abrir, la aplicación mostrará el modo demostración. Puedes probar sin GPS real. Pulsa **Obtener mi ubicación GPS** y permite el acceso a ubicación para probar con datos reales.
7. Pulsa **Ver ubicación** en un docente, **Cómo llegar**, **Ir al tutor más cercano** y **Planear recorrido**. Verifica que se abra Google Maps.

### Alternativa: ejecutar el build desde PowerShell

```powershell
cd C:\Dev\lab-recorrido-googlemaps
.\gradlew.bat assembleDebug
```

APK resultante: `app\build\outputs\apk\debug\app-debug.apk`.

**Primer build:** requiere conexión a Internet porque Gradle 8.9 y el plugin de Android pueden descargarse. El ZIP incluye un pequeño lanzador Java creado para este paquete (no el wrapper JAR oficial), que descarga la distribución oficial de Gradle 8.9 sin necesitar Gradle instalado globalmente. Requiere Internet en la primera ejecución; si Android Studio informa un error de wrapper, usa el menú de configuración de Gradle de Android Studio o regenera el wrapper oficial con `gradle wrapper --gradle-version 8.9` cuando dispongas de Gradle.

## 6. ¿Qué hace cada pantalla?

**Sección 01 — Ubicación:** parte de un punto de prueba próximo a la Universidad Piloto. Si se conceden permisos y se activa el GPS, se actualiza la ubicación con `LocationManager`. En un emulador puedes simular coordenadas desde los controles de ubicación del emulador.

**Sección 02 — Rutas:** muestra el docente más cercano por distancia directa. Permite elegir transporte (caminando, automóvil o bicicleta) y abrir Google Maps para el docente más cercano o para los cuatro tutores en una sola ruta con paradas.

**Sección 03 — Docentes:** muestra cuatro tutores de demostración, materia, coordenadas y distancia estimada en línea recta. Cada uno tiene los botones **Ver ubicación** y **Cómo llegar**.

## 7. Flujo interno (explicación técnica)

```text
Inicio en MainActivity
     ↓
Ubicación demo o permiso de GPS (LocationManager)
     ↓
Lista de 4 objetos Tutor
     ↓
LocationUtils.distanciaMetros() → distancia geodésica
     ↓
LocationUtils.masCercano() → tutor con menor distancia directa
     ↓
Usuario elige acción:
  A) Ver ubicación → URI geo:... → ACTION_VIEW
  B) Cómo llegar → Maps URL origin/destination/travelmode
  C) Tutor más cercano → B al tutor identificado
  D) Recorrido completo → vecino más próximo → waypoints → ACTION_VIEW
     ↓
Google Maps (o navegador si falta Maps)
```

### Diferencia entre distancia directa y tiempo real

`Location.distanceBetween()` calcula una **distancia aproximada en superficie terrestre**, no el tiempo ni el trazado por calles. La ruta de varias paradas se ordena con la heurística del **vecino más cercano**. Luego Google Maps recibe ese orden como puntos intermedios y muestra rutas según la vía y el medio de transporte; el usuario puede considerar alternativas propuestas por Maps. La aplicación **no consume Directions API, no consulta tráfico ni resuelve el problema de optimización de rutas de manera exacta**.

## 8. Intents utilizados

**Ver la ubicación de un tutor:**

```java
Uri uri = Uri.parse("geo:4.628900,-74.066000?q=4.628900,-74.066000(Ana%20Torres)");
Intent intent = new Intent(Intent.ACTION_VIEW, uri);
intent.setPackage("com.google.android.apps.maps");
startActivity(intent);
```

**Abrir ruta individual:**

```text
https://www.google.com/maps/dir/?api=1&origin=4.628100,-74.064800&destination=4.628900,-74.066000&travelmode=walking
```

**Abrir ruta de varias paradas:** se pasa `origin`, `destination`, `waypoints` separados mediante `|`, y `travelmode`. La aplicación construye correctamente la URL con `Uri.Builder`.

**Por qué no se utiliza Maps SDK:** el objetivo del laboratorio es invocar Google Maps **mediante intents**, no dibujar mapas con un SDK; por tanto, no se requiere clave API.

## 9. Evidencia sugerida para la sustentación

1. Pantalla principal mostrando los cuatro tutores.
2. Permiso de localización y coordenadas actuales (o modo demostración).
3. Acción **Ver ubicación** y resultado en Google Maps.
4. Acción **Ir al tutor más cercano** y mapa de ruta.
5. Seleccionar transporte, pulsar **Planear recorrido** y mostrar paradas múltiples.
6. Explicar `MainActivity.java`, `Tutor.java`, `LocationUtils.java` y este README.

> Las pruebas deben realizarse en un emulador o dispositivo Android. El código se entrega listo para abrirse, pero la ejecución del APK no puede comprobarse dentro del ZIP.

## 10. Pruebas funcionales propuestas

| Prueba | Acción | Resultado esperado |
|---|---|---|
| P01 | Abrir app | Se ven 4 tutores, modo demostración y botones |
| P02 | Pulsar GPS | Se solicita permiso y se actualizan coordenadas al obtener ubicación |
| P03 | Pulsar demo | Restaura ubicación simulada de Bogotá |
| P04 | Ver ubicación | Abre Maps mostrando el punto del docente |
| P05 | Cómo llegar | Abre origen, destino y modo elegido |
| P06 | Tutor más cercano | Selecciona el tutor con menor distancia geográfica |
| P07 | Recorrido completo | Abre Google Maps con 4 docentes (3 paradas + destino) |
| P08 | Maps no instalado | Intenta abrir vínculo web en navegador |
| P09 | Denegar GPS | Muestra aviso y permite continuar en modo demo |

## 11. Cómo subir a GitHub

El repositorio destino es: **https://github.com/EstebanMB18/lab-recorrido.googlemaps.git**.

```powershell
cd C:\Dev\lab-recorrido-googlemaps
git init
git add .
git commit -m "Laboratorio Android GPS y recorridos con Google Maps Intents"
git branch -M main
git remote add origin https://github.com/EstebanMB18/lab-recorrido.googlemaps.git
git push -u origin main
```

Si el repositorio remoto ya contiene archivos y rechaza el `push`, **no utilices `--force` de entrada**: primero revisa `git status`, `git remote -v` y los commits remotos.

## 12. Referencias

- Guía entregada por el docente: *Google Maps Intents for Android*, Universidad Piloto de Colombia. En especial: introducción (p. 2); `ACTION_VIEW`, URI y paquete (p. 4); apertura de actividad (p. 5); ubicación (p. 8–9); navegación (p. 13–14); ruta con múltiples puntos (p. 15); `origin` / `destination` / `waypoints` (p. 16).
- Android Developers: Intents comunes y permisos de ubicación — https://developer.android.com/guide/components/intents-common y https://developer.android.com/develop/sensors-and-location/location/permissions
- Google Maps URLs — https://developers.google.com/maps/documentation/urls/guide
