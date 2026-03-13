# ⚙️ Kit 1.0 — Manual del Orquestador Python

> **Ubicación:** `tools/orchestrators/README_ORQUESTADOR.md`  
> Este documento es la referencia operativa del `orquestador_kit.py`. Define modos de ejecución, contratos de datos, reglas de generación y el blindaje ante crashes de Gradle.

---

## Arquitectura del flujo

```
Android (Kubo Map)
    │
    │  repository_dispatch
    │  { event_type: "iniciar-sintesis-app", client_payload: AppGenomePayload }
    ▼
GitHub Actions (kit_sintesis_app.yml)
    │
    │  env: CLAUDE_API_KEY, APP_GENOME (JSON)
    ▼
orquestador_kit.py
    │
    ├─ Modo: sintetizar  ──▶ Claude API → escribe Kotlin → commit
    ├─ Modo: asimilar    ──▶ escanea repo → genera JSON Kubo Map → guarda en Córtex
    └─ Modo: reparar     ──▶ lee crash report → Claude propone fix → commit
```

---

## 1. Modo SINTETIZAR (default)

El modo estándar. Recibe el genoma de la app y genera la pantalla solicitada.

### Comando
```bash
python orquestador_kit.py --mode sintetizar
```
*(Los datos llegan por la variable de entorno `APP_GENOME`, inyectada por GitHub Actions.)*

### Variables de entorno requeridas
| Variable | Descripción |
|---|---|
| `CLAUDE_API_KEY` | API key de Anthropic (guardada en GitHub Secrets) |
| `APP_GENOME` | JSON con `AppGenomePayload` serializado |

### AppGenomePayload esperado
```json
{
  "appName":           "NombreDeLaApp",
  "aesthetic":         "Minimalismo Industrial",
  "isLocalFirst":      true,
  "agentInstructions": "Prioriza código estóico. Cero comentarios innecesarios."
}
```

### Ruta de salida del código generado
```
app/src/main/java/com/dreiz/kit/generated/MainScreen.kt
```

---

## 2. Modo ASIMILAR (ingeniería inversa)

Cuando Kit retoma un proyecto existente. El orquestador escanea el código fuente y genera el JSON de Kubo Map compatible con la malla 4×8.

### Comando
```bash
python orquestador_kit.py \
  --mode asimilar \
  --path /ruta/al/repo \
  --project_id proj_vskel_01
```

### Comportamiento paso a paso
1. **Escaneo** — recorre `app/src/main/java/` recursivamente buscando archivos `.kt`.
2. **Clasificación** — Claude categoriza cada módulo según el Diccionario de Arquetipos (ver sección 3).
3. **Generación del JSON** — produce un `KuboMapState` válido con la malla 4×8 y 32 slots.
4. **Persistencia** — guarda el ADN resultante en la tabla `libreria_experiencia` del Córtex (Room DB) vía el endpoint de la app, para futuras referencias.

### JSON de salida esperado
```json
{
  "project_id": "proj_vskel_01",
  "seed":        "<SHA-256 del project_id>",
  "grid":        { "rows": 4, "columns": 8 },
  "kubos":       [ ... ],
  "connections": [ ... ],
  "slots":       [ ... ]
}
```

---

## 3. Inyección del Diccionario de Arquetipos

> **Regla crítica:** Claude **nunca** inventa tipos de Kubos.  
> Antes de cada prompt de clasificación, el orquestador inyecta esta matriz:

| Color | Tipo | Módulos que cubre |
|---|---|---|
| 🔵 Azul Cobalto | `core` | Lógica de negocio, red, base de datos, repositorios |
| 🟠 Mandarina | `security` | Autenticación, cifrado, alertas críticas, permisos |
| ⚫ Gris Arcilla | `utility` | UI/Compose, extensiones, helpers, adaptadores |

**Snippet del prompt de clasificación:**
```python
SYSTEM_ARCHAETYPE = """
Eres un clasificador de módulos Android.
Asigna ÚNICAMENTE uno de estos tipos a cada módulo:
- "core"     : lógica de negocio, red, base de datos
- "security" : autenticación, cifrado, alertas críticas  
- "utility"  : interfaz de usuario, extensiones, helpers

No inventes tipos. Si dudas, asigna "utility".
Responde SOLO JSON. Sin markdown.
"""
```

---

## 4. Generación Determinística de Figuras

> **El orquestador NO asigna figuras geométricas. Solo asigna slots.**

El flujo correcto es:

```
orquestador_kit.py  →  asigna slot_id a cada Kubo
        ↓
KuboMapViewModel.kt →  generarMallaDeterminista(seed)
        ↓
KuboFigure.kt       →  BIBLIOTECA_FIGURAS_KIT[index] según seed
```

Esto garantiza que el mismo `project_id` siempre produce el mismo layout visual, sin importar cuántas veces se regenere.

---

## 5. Modo REPARAR (blindaje anti-crash)

Si el workflow de GitHub Actions detecta que `./gradlew assembleDebug` falla:

### Activación automática desde el workflow YAML
```yaml
- name: Build & Watch
  run: |
    ./gradlew assembleDebug 2>&1 | tee build_output.txt || \
    python orquestador_kit.py --mode reparar --crash_log build_output.txt
```

### Comportamiento
1. **Intercepta** el StackTrace del log de Gradle.
2. **Genera** un archivo local `KIT_CRASH_REPORT_<timestamp>.txt` con:
   - Timestamp ISO 8601
   - Extracto del error (primeras 80 líneas del stack)
   - Contexto del archivo fuente afectado
3. **Notifica** al agente `SYSTEM` con `isCritical = true` vía la función `addLog()` del `WarRoomViewModel`.
4. **Llama a Claude** con el stack trace para proponer un fix.
5. **Aplica el fix** y hace commit con el mensaje: `Kit-Bot: fix crash [<timestamp>]`.

### Formato del crash report
```
KIT_CRASH_REPORT_2026-03-12T23:00:00Z.txt
─────────────────────────────────────────
Proyecto:   com.dreiz.kit
Timestamp:  2026-03-12T23:00:00Z
Gradle cmd: ./gradlew assembleDebug
─────────────────────────────────────────
ERROR:
<StackTrace extraído aquí>
─────────────────────────────────────────
FIX PROPUESTO POR CLAUDE:
<Respuesta de Claude API aquí>
```

---

## 6. Estructura de archivos del orquestador

```
tools/orchestrators/
├── orquestador_kit.py      ← Punto de entrada principal
├── modes/
│   ├── sintetizar.py       ← Lógica del modo sintetizar
│   ├── asimilar.py         ← Lógica del modo asimilar
│   └── reparar.py          ← Lógica del modo reparar
├── prompts/
│   ├── system_architect.txt ← Prompt de personalidad del Architect
│   └── archaetype_dict.txt  ← Diccionario de arquetipos inyectable
└── README_ORQUESTADOR.md   ← Este archivo
```

---

## 7. Dependencias Python

```bash
pip install anthropic          # SDK de Claude
pip install pygments           # Coloreo de output en terminal (opcional)
```

**Python mínimo:** 3.10

---

## 8. Secretos de GitHub requeridos

| Secret | Descripción |
|---|---|
| `CLAUDE_API_KEY` | Clave API de Anthropic |
| `GH_PAT` | Personal Access Token con permisos `repo` + `workflow` (para el push del bot) |

---

## 9. Convenciones de commits del bot

| Tipo | Mensaje |
|---|---|
| Código generado | `Kit-Bot: síntesis [appName] — [aesthetic]` |
| Fix de crash | `Kit-Bot: fix crash [timestamp]` |
| Asimilación | `Kit-Bot: asimilación [project_id]` |

---

*Kit 1.0 · com.dreiz.kit · orquestador v1*
