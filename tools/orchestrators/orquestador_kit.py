# orquestador_kit.py — Motor de generación de código con Claude API
# Modos: sintetizar (default) | asimilar | reparar
import os, json, argparse, datetime, anthropic

ARCHAETYPE_DICT = """
Clasifica módulos Android SOLO con estos tipos:
- "core"     : lógica de negocio, red, base de datos
- "security" : autenticación, cifrado, alertas críticas
- "utility"  : interfaz de usuario, extensiones, helpers
No inventes tipos. Si dudas, asigna "utility".
"""

def ejecutar_kit():
    parser = argparse.ArgumentParser()
    parser.add_argument("--mode",      default="sintetizar", choices=["sintetizar", "asimilar", "reparar"])
    parser.add_argument("--path",      default=".")
    parser.add_argument("--project_id",default="kit_default")
    parser.add_argument("--crash_log", default="build_output.txt")
    args = parser.parse_args()

    client = anthropic.Anthropic(api_key=os.environ.get("CLAUDE_API_KEY"))

    if args.mode == "sintetizar":
        _modo_sintetizar(client)
    elif args.mode == "asimilar":
        _modo_asimilar(client, args.path, args.project_id)
    elif args.mode == "reparar":
        _modo_reparar(client, args.crash_log)

def _modo_sintetizar(client):
    data = json.loads(os.environ.get("APP_GENOME", "{}"))
    prompt = f"""
Rol: Architect & Senior Android Developer.
Contexto: Sintetizando app "{data.get('appName')}" con estética {data.get('aesthetic')}.
Instrucciones: {data.get('agentInstructions')}.
{ARCHAETYPE_DICT}
TAREA: Genera la pantalla principal en Jetpack Compose.
- Package: com.dreiz.kit.generated
- Usa Material 3 y Design Tokens de Kit (colores oscuros, neumorfismo suave).
- Cero explicaciones. Solo código Kotlin funcional.
"""
    res  = client.messages.create(model="claude-opus-4-6", max_tokens=3000,
                                   messages=[{"role": "user", "content": prompt}])
    path = "app/src/main/java/com/dreiz/kit/generated/MainScreen.kt"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(res.content[0].text)
    print(f"[Kit] Síntesis completa → {path}")

def _modo_asimilar(client, repo_path, project_id):
    # Recolectar archivos .kt del repo
    kt_files = []
    for root, _, files in os.walk(repo_path):
        for f in files:
            if f.endswith(".kt") and "generated" not in root:
                full = os.path.join(root, f)
                try:
                    kt_files.append({"file": f, "content": open(full).read()[:800]})
                except: pass

    prompt = f"""
{ARCHAETYPE_DICT}
Analiza estos archivos Kotlin y devuelve SOLO JSON con esta estructura:
{{"kubos": [{{"kubo_id": "...", "type": "core|security|utility", "label": "NombreModulo"}}]}}
Archivos: {json.dumps(kt_files[:20])}
"""
    res  = client.messages.create(model="claude-opus-4-6", max_tokens=2000,
                                   messages=[{"role": "user", "content": prompt}])
    ts   = datetime.datetime.utcnow().strftime("%Y%m%d_%H%M%S")
    path = f"tools/orchestrators/asimilacion_{project_id}_{ts}.json"
    with open(path, "w") as f:
        f.write(res.content[0].text)
    print(f"[Kit] Asimilación completa → {path}")

def _modo_reparar(client, crash_log_path):
    try:
        crash = open(crash_log_path).read()[-4000:]
    except:
        crash = "Log no disponible"

    prompt = f"""
Eres un experto en Kotlin y Gradle. Analiza este error de build y propón el fix exacto.
Responde SOLO con el fragmento de código corregido, sin explicaciones.
ERROR:
{crash}
"""
    res  = client.messages.create(model="claude-opus-4-6", max_tokens=2000,
                                   messages=[{"role": "user", "content": prompt}])
    ts   = datetime.datetime.utcnow().strftime("%Y%m%dT%H%M%SZ")
    report_path = f"KIT_CRASH_REPORT_{ts}.txt"
    with open(report_path, "w") as f:
        f.write(f"KIT_CRASH_REPORT_{ts}\n{'─'*40}\n")
        f.write(f"Proyecto:   com.dreiz.kit\nTimestamp:  {ts}\n{'─'*40}\n")
        f.write(f"ERROR:\n{crash}\n{'─'*40}\n")
        f.write(f"FIX PROPUESTO POR CLAUDE:\n{res.content[0].text}\n")
    print(f"[Kit] Crash report → {report_path}")

if __name__ == "__main__":
    ejecutar_kit()
