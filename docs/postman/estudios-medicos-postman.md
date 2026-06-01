# Estudios medicos desde Postman

Este flujo usa los endpoints actuales de catalogo y captura:

- `POST /api/auth/login`
- `POST /api/estudios/tipos`
- `POST /api/estudios/parametros`
- `GET /api/estudios/tipos`
- `POST /api/estudios`

## Archivos de apoyo

- Environment: [cohorte-estudios.environment.json](C:/Users/garci/OneDrive/Desktop/cohorteApp/cohorte_test/docs/postman/cohorte-estudios.environment.json)
- Coleccion base: [cohorte-estudios.collection.json](C:/Users/garci/OneDrive/Desktop/cohorteApp/cohorte_test/docs/postman/cohorte-estudios.collection.json)
- Catalogo de tipos y parametros: [estudios-medicos-catalogo.json](C:/Users/garci/OneDrive/Desktop/cohorteApp/cohorte_test/docs/postman/estudios-medicos-catalogo.json)

## Flujo recomendado

1. Importa el environment y la coleccion en Postman.
2. Ejecuta `Login`.
3. Ejecuta `Listar usuarios activos` y copia el `data[].UUID` del capturista.
4. Ejecuta `Listar pacientes activos` y copia el `data[].UUID` del paciente.
5. Crea un tipo de estudio con `POST /api/estudios/tipos`.
6. Crea sus parametros con `POST /api/estudios/parametros`.
7. Consulta `GET /api/estudios/tipos` para ubicar el `id` del tipo y los `id` de parametros.
8. Crea el estudio real con `POST /api/estudios`.

## Endpoints que necesitas para capturar una prueba

- `POST /api/auth/login`: devuelve el JWT en `data`
- `GET /api/users/activos`: devuelve `data[].UUID`
- `GET /api/pacientes/activos`: devuelve `data[].UUID`
- `POST /api/estudios/tipos`: crea el catalogo del estudio
- `POST /api/estudios/parametros`: crea las variables capturables
- `GET /api/estudios/tipos`: te devuelve cada tipo con sus parametros e ids
- `POST /api/estudios`: guarda la captura clinica

## Regla operativa

Para cualquier prueba nueva, el backend actual funciona asi:

1. Creas el `tipo de estudio`.
2. Creas sus `parametros`.
3. Tomas el `id` de cada parametro.
4. En `POST /api/estudios` mandas:
   - `pacienteUUID`
   - `usuarioRealizaUUID`
   - `idTipoEstudio`
   - `fechaEstudio`
   - `observaciones`
   - `resultados[]`
   - `adjuntos[]`

## Como mapear cada tipo de dato

- Numero: usa `valorNumerico`
- Texto libre o hallazgo: usa `valorTexto`
- Si/No: usa `valorBooleano`
- Pruebas por momentos, etapas o series: usa `grupoCodigo`, `grupoEtiqueta` y `orden`
- PDF o imagen: usa `adjuntos[]` con `rutaUrl`; el archivo binario no se sube aqui

## Restricciones practicas del endpoint

- Cada `idParametro` debe pertenecer al `idTipoEstudio` enviado.
- Un adjunto no puede repetir el mismo `orden` dentro del mismo estudio.
- En resultados agrupados conviene repetir el mismo `grupoCodigo` para el mismo bloque, por ejemplo `PRE`, `POST`, `BASAL`, `ETAPA_1`.
- Si una prueba no tiene variables definidas, puedes registrar `resultados: []` y solo `observaciones` o `adjuntos`.

## Payloads base

### Crear tipo

```json
{
  "nombre": "Antropometria",
  "descripcion": "Medidas corporales y bioimpedancia"
}
```

### Crear parametro

```json
{
  "idTipoEstudio": 5,
  "nombre": "Peso",
  "unidad": "kg"
}
```

### Crear estudio simple

```json
{
  "pacienteUUID": "REEMPLAZAR_PACIENTE_UUID",
  "usuarioRealizaUUID": "REEMPLAZAR_USUARIO_UUID",
  "idTipoEstudio": 5,
  "fechaEstudio": "2026-04-20",
  "observaciones": "Captura desde Postman",
  "resultados": [
    {
      "idParametro": 101,
      "valorNumerico": 72.4
    },
    {
      "idParametro": 102,
      "valorNumerico": 170
    },
    {
      "idParametro": 103,
      "valorNumerico": 25.1
    }
  ],
  "adjuntos": []
}
```

### Crear estudio con adjuntos

```json
{
  "pacienteUUID": "REEMPLAZAR_PACIENTE_UUID",
  "usuarioRealizaUUID": "REEMPLAZAR_USUARIO_UUID",
  "idTipoEstudio": 2,
  "fechaEstudio": "2026-04-20",
  "observaciones": "Consentimiento firmado",
  "resultados": [
    {
      "idParametro": 20,
      "valorBooleano": true
    }
  ],
  "adjuntos": [
    {
      "tipo": "PDF",
      "nombreOriginal": "consentimiento.pdf",
      "mimeType": "application/pdf",
      "rutaUrl": "/documentos/consentimiento.pdf",
      "descripcion": "Archivo almacenado externamente",
      "orden": 0
    }
  ]
}
```

### Crear estudio con grupos o series

Usa `grupoCodigo`, `grupoEtiqueta` y `orden` para pruebas por etapa o momento.

```json
{
  "pacienteUUID": "REEMPLAZAR_PACIENTE_UUID",
  "usuarioRealizaUUID": "REEMPLAZAR_USUARIO_UUID",
  "idTipoEstudio": 11,
  "fechaEstudio": "2026-04-20",
  "observaciones": "Prueba del escalon capturada como estudio medico",
  "resultados": [
    {
      "idParametro": 300,
      "valorNumerico": 120,
      "grupoCodigo": "BASAL",
      "grupoEtiqueta": "Basal",
      "orden": 0
    },
    {
      "idParametro": 301,
      "valorNumerico": 80,
      "grupoCodigo": "BASAL",
      "grupoEtiqueta": "Basal",
      "orden": 1
    },
    {
      "idParametro": 300,
      "valorNumerico": 135,
      "grupoCodigo": "ETAPA_1",
      "grupoEtiqueta": "Etapa 1",
      "orden": 0
    },
    {
      "idParametro": 301,
      "valorNumerico": 88,
      "grupoCodigo": "ETAPA_1",
      "grupoEtiqueta": "Etapa 1",
      "orden": 1
    },
    {
      "idParametro": 308,
      "valorTexto": "Sin incidencias",
      "grupoCodigo": "ETAPA_1",
      "grupoEtiqueta": "Etapa 1",
      "orden": 2
    }
  ],
  "adjuntos": []
}
```

## Ejemplos listos por estudio

### 1. Consentimiento informado

- Tipo: `Consentimiento informado`
- Parametros:
  - `Acepta participar`
- Adjuntos:
  - PDF de carta firmada

```json
{
  "pacienteUUID": "REEMPLAZAR_PACIENTE_UUID",
  "usuarioRealizaUUID": "REEMPLAZAR_USUARIO_UUID",
  "idTipoEstudio": 1,
  "fechaEstudio": "2026-04-20",
  "observaciones": "Consentimiento recabado con dos testigos",
  "resultados": [
    {
      "idParametro": 1,
      "valorBooleano": true
    }
  ],
  "adjuntos": [
    {
      "tipo": "PDF",
      "nombreOriginal": "consentimiento.pdf",
      "mimeType": "application/pdf",
      "rutaUrl": "/documentos/consentimiento.pdf",
      "descripcion": "Carta de consentimiento informado",
      "orden": 0
    }
  ]
}
```

### 2. Toma de tension arterial

```json
{
  "pacienteUUID": "REEMPLAZAR_PACIENTE_UUID",
  "usuarioRealizaUUID": "REEMPLAZAR_USUARIO_UUID",
  "idTipoEstudio": 2,
  "fechaEstudio": "2026-04-20",
  "observaciones": "Paciente en reposo",
  "resultados": [
    { "idParametro": 10, "valorNumerico": 118 },
    { "idParametro": 11, "valorNumerico": 76 },
    { "idParametro": 12, "valorNumerico": 68 }
  ],
  "adjuntos": []
}
```

### 3. Laboratorio sangre

```json
{
  "pacienteUUID": "REEMPLAZAR_PACIENTE_UUID",
  "usuarioRealizaUUID": "REEMPLAZAR_USUARIO_UUID",
  "idTipoEstudio": 3,
  "fechaEstudio": "2026-04-20",
  "observaciones": "Resultados de laboratorio cargados",
  "resultados": [
    { "idParametro": 30, "valorNumerico": 95 },
    { "idParametro": 31, "valorNumerico": 0.98 },
    { "idParametro": 32, "valorNumerico": 5.7 },
    { "idParametro": 33, "valorNumerico": 188 }
  ],
  "adjuntos": [
    {
      "tipo": "PDF",
      "nombreOriginal": "laboratorio.pdf",
      "mimeType": "application/pdf",
      "rutaUrl": "/laboratorio/resultado-2026-04-20.pdf",
      "descripcion": "Reporte de laboratorio",
      "orden": 0
    }
  ]
}
```

### 4. Entrega de biomasa

```json
{
  "pacienteUUID": "REEMPLAZAR_PACIENTE_UUID",
  "usuarioRealizaUUID": "REEMPLAZAR_USUARIO_UUID",
  "idTipoEstudio": 4,
  "fechaEstudio": "2026-04-20",
  "observaciones": "Acepto entrega de biomasa",
  "resultados": [
    {
      "idParametro": 40,
      "valorBooleano": true
    }
  ],
  "adjuntos": []
}
```

### 5. Antropometria

```json
{
  "pacienteUUID": "REEMPLAZAR_PACIENTE_UUID",
  "usuarioRealizaUUID": "REEMPLAZAR_USUARIO_UUID",
  "idTipoEstudio": 5,
  "fechaEstudio": "2026-04-20",
  "observaciones": "Toma de medidas y TANITA",
  "resultados": [
    { "idParametro": 50, "valorNumerico": 72.4 },
    { "idParametro": 51, "valorNumerico": 170 },
    { "idParametro": 52, "valorNumerico": 25.1 },
    { "idParametro": 53, "valorNumerico": 92 },
    { "idParametro": 54, "valorNumerico": 101 },
    { "idParametro": 55, "valorTexto": "Estandar" },
    { "idParametro": 61, "valorNumerico": 29.8 }
  ],
  "adjuntos": []
}
```

### 6. Ultrasonido vias biliares

```json
{
  "pacienteUUID": "REEMPLAZAR_PACIENTE_UUID",
  "usuarioRealizaUUID": "REEMPLAZAR_USUARIO_UUID",
  "idTipoEstudio": 6,
  "fechaEstudio": "2026-04-20",
  "observaciones": "US hepatobiliar",
  "resultados": [
    { "idParametro": 70, "valorTexto": "No" },
    { "idParametro": 71, "valorTexto": "Leve" },
    { "idParametro": 72, "valorNumerico": 3.2 }
  ],
  "adjuntos": [
    {
      "tipo": "PDF",
      "nombreOriginal": "us-vias-biliares.pdf",
      "mimeType": "application/pdf",
      "rutaUrl": "/imagenes/us-vias-biliares.pdf",
      "descripcion": "Reporte radiologico",
      "orden": 0
    },
    {
      "tipo": "IMG",
      "nombreOriginal": "us-vias-biliares.jpg",
      "mimeType": "image/jpeg",
      "rutaUrl": "/imagenes/us-vias-biliares.jpg",
      "descripcion": "Imagen diagnostica",
      "orden": 1
    }
  ]
}
```

### 7. Electrocardiograma

```json
{
  "pacienteUUID": "REEMPLAZAR_PACIENTE_UUID",
  "usuarioRealizaUUID": "REEMPLAZAR_USUARIO_UUID",
  "idTipoEstudio": 7,
  "fechaEstudio": "2026-04-20",
  "observaciones": "ECG en reposo",
  "resultados": [
    { "idParametro": 80, "valorNumerico": 71 },
    { "idParametro": 81, "valorTexto": "Sinusal" },
    { "idParametro": 83, "valorNumerico": 160 },
    { "idParametro": 84, "valorNumerico": 92 },
    { "idParametro": 85, "valorNumerico": 410 },
    { "idParametro": 93, "valorTexto": "Sin arritmias" }
  ],
  "adjuntos": [
    {
      "tipo": "PDF",
      "nombreOriginal": "ecg.pdf",
      "mimeType": "application/pdf",
      "rutaUrl": "/ecg/ecg.pdf",
      "descripcion": "Trazado PDF",
      "orden": 0
    },
    {
      "tipo": "IMG",
      "nombreOriginal": "ecg.png",
      "mimeType": "image/png",
      "rutaUrl": "/ecg/ecg.png",
      "descripcion": "Imagen del trazado",
      "orden": 1
    }
  ]
}
```

### 8. Ultrasonido de hueso

```json
{
  "pacienteUUID": "REEMPLAZAR_PACIENTE_UUID",
  "usuarioRealizaUUID": "REEMPLAZAR_USUARIO_UUID",
  "idTipoEstudio": 8,
  "fechaEstudio": "2026-04-20",
  "observaciones": "Calcaneo derecho",
  "resultados": [
    { "idParametro": 100, "valorNumerico": 1550 },
    { "idParametro": 101, "valorNumerico": -1.8 }
  ],
  "adjuntos": [
    {
      "tipo": "PDF",
      "nombreOriginal": "us-hueso.pdf",
      "mimeType": "application/pdf",
      "rutaUrl": "/hueso/us-hueso.pdf",
      "descripcion": "Reporte",
      "orden": 0
    }
  ]
}
```

### 9. Espirometria

```json
{
  "pacienteUUID": "REEMPLAZAR_PACIENTE_UUID",
  "usuarioRealizaUUID": "REEMPLAZAR_USUARIO_UUID",
  "idTipoEstudio": 9,
  "fechaEstudio": "2026-04-20",
  "observaciones": "Espirometria simple",
  "resultados": [
    { "idParametro": 110, "valorNumerico": 4.2 },
    { "idParametro": 111, "valorNumerico": 3.5 },
    { "idParametro": 112, "valorNumerico": 83.3 },
    { "idParametro": 113, "valorNumerico": 8.1 }
  ],
  "adjuntos": [
    {
      "tipo": "PDF",
      "nombreOriginal": "espirometria.pdf",
      "mimeType": "application/pdf",
      "rutaUrl": "/espirometria/reporte.pdf",
      "descripcion": "Reporte exportado",
      "orden": 0
    }
  ]
}
```

### 10. Densitometria

```json
{
  "pacienteUUID": "REEMPLAZAR_PACIENTE_UUID",
  "usuarioRealizaUUID": "REEMPLAZAR_USUARIO_UUID",
  "idTipoEstudio": 10,
  "fechaEstudio": "2026-04-20",
  "observaciones": "Densitometria DXA",
  "resultados": [
    { "idParametro": 120, "valorNumerico": 0.88 },
    { "idParametro": 121, "valorNumerico": -1.2 },
    { "idParametro": 122, "valorNumerico": -0.9 },
    { "idParametro": 123, "valorNumerico": 1.01 },
    { "idParametro": 126, "valorNumerico": 32.4 }
  ],
  "adjuntos": [
    {
      "tipo": "PDF",
      "nombreOriginal": "densitometria.pdf",
      "mimeType": "application/pdf",
      "rutaUrl": "/densitometria/densitometria.pdf",
      "descripcion": "Reporte principal",
      "orden": 0
    }
  ]
}
```

### 11. Caminata 6 minutos

```json
{
  "pacienteUUID": "REEMPLAZAR_PACIENTE_UUID",
  "usuarioRealizaUUID": "REEMPLAZAR_USUARIO_UUID",
  "idTipoEstudio": 11,
  "fechaEstudio": "2026-04-20",
  "observaciones": "Caminata de 6 minutos",
  "resultados": [
    { "idParametro": 140, "valorNumerico": 118, "grupoCodigo": "PRE", "grupoEtiqueta": "Antes", "orden": 0 },
    { "idParametro": 141, "valorNumerico": 76, "grupoCodigo": "PRE", "grupoEtiqueta": "Antes", "orden": 1 },
    { "idParametro": 143, "valorNumerico": 72, "grupoCodigo": "PRE", "grupoEtiqueta": "Antes", "orden": 2 },
    { "idParametro": 144, "valorNumerico": 12, "grupoCodigo": "POST", "grupoEtiqueta": "Despues", "orden": 0 },
    { "idParametro": 145, "valorNumerico": 460, "grupoCodigo": "POST", "grupoEtiqueta": "Despues", "orden": 1 },
    { "idParametro": 146, "valorNumerico": 3, "grupoCodigo": "POST", "grupoEtiqueta": "Despues", "orden": 2 }
  ],
  "adjuntos": []
}
```

### 12. Prueba del escalon

Usala dentro de `estudios`, no en el modulo legado.

```json
{
  "pacienteUUID": "REEMPLAZAR_PACIENTE_UUID",
  "usuarioRealizaUUID": "REEMPLAZAR_USUARIO_UUID",
  "idTipoEstudio": 12,
  "fechaEstudio": "2026-04-20",
  "observaciones": "Captura en modelo unificado",
  "resultados": [
    { "idParametro": 160, "valorNumerico": 120, "grupoCodigo": "BASAL", "grupoEtiqueta": "Basal", "orden": 0 },
    { "idParametro": 161, "valorNumerico": 80, "grupoCodigo": "BASAL", "grupoEtiqueta": "Basal", "orden": 1 },
    { "idParametro": 162, "valorNumerico": 68, "grupoCodigo": "BASAL", "grupoEtiqueta": "Basal", "orden": 2 },
    { "idParametro": 163, "valorNumerico": 170, "grupoCodigo": "BASAL", "grupoEtiqueta": "Basal", "orden": 3 },
    { "idParametro": 164, "valorNumerico": 72, "grupoCodigo": "BASAL", "grupoEtiqueta": "Basal", "orden": 4 },
    { "idParametro": 167, "valorNumerico": 150, "grupoCodigo": "ETAPA_1", "grupoEtiqueta": "Etapa 1", "orden": 0 },
    { "idParametro": 168, "valorTexto": "Tolero bien la prueba", "grupoCodigo": "ETAPA_1", "grupoEtiqueta": "Etapa 1", "orden": 1 }
  ],
  "adjuntos": []
}
```

### 13. Acelerometria

```json
{
  "pacienteUUID": "REEMPLAZAR_PACIENTE_UUID",
  "usuarioRealizaUUID": "REEMPLAZAR_USUARIO_UUID",
  "idTipoEstudio": 13,
  "fechaEstudio": "2026-04-20",
  "observaciones": "Monitoreo de siete dias",
  "resultados": [
    { "idParametro": 180, "valorNumerico": 190 },
    { "idParametro": 181, "valorNumerico": 120 },
    { "idParametro": 182, "valorNumerico": 70 },
    { "idParametro": 183, "valorNumerico": 510 },
    { "idParametro": 184, "valorNumerico": 65 }
  ],
  "adjuntos": []
}
```

### 14. Fibroscan

No tiene variables definidas en tu lista. Por ahora puedes:

- crear el tipo `Fibroscan`
- no crear parametros aun
- registrar solo observaciones y adjuntos

```json
{
  "pacienteUUID": "REEMPLAZAR_PACIENTE_UUID",
  "usuarioRealizaUUID": "REEMPLAZAR_USUARIO_UUID",
  "idTipoEstudio": 14,
  "fechaEstudio": "2026-04-20",
  "observaciones": "Pendiente de definir variables clinicas",
  "resultados": [],
  "adjuntos": [
    {
      "tipo": "PDF",
      "nombreOriginal": "fibroscan.pdf",
      "mimeType": "application/pdf",
      "rutaUrl": "/fibroscan/fibroscan.pdf",
      "descripcion": "Reporte del equipo",
      "orden": 0
    }
  ]
}
```

### 15. Cuestionario Minimental

```json
{
  "pacienteUUID": "REEMPLAZAR_PACIENTE_UUID",
  "usuarioRealizaUUID": "REEMPLAZAR_USUARIO_UUID",
  "idTipoEstudio": 15,
  "fechaEstudio": "2026-04-20",
  "observaciones": "Aplicacion de cuestionario MMSE",
  "resultados": [
    { "idParametro": 190, "valorNumerico": 8 },
    { "idParametro": 191, "valorNumerico": 3 },
    { "idParametro": 192, "valorNumerico": 4 },
    { "idParametro": 193, "valorNumerico": 2 },
    { "idParametro": 194, "valorNumerico": 7 },
    { "idParametro": 195, "valorNumerico": 1 },
    { "idParametro": 196, "valorNumerico": 25 }
  ],
  "adjuntos": []
}
```

## Nota importante

- `adjuntos` solo guarda metadatos y ruta/URL. El archivo real lo tienes que subir o almacenar por fuera.
- Para `Caminata 6 minutos` y `Prueba del escalon`, el esquema correcto es usar grupos.
- Si quieres, en el siguiente paso te puedo generar tambien una coleccion Postman mucho mas grande, ya con una request separada para cada tipo y cada ejemplo de captura.
