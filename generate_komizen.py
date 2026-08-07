#!/usr/bin/env python3
"""
Komizen Extensions Generator
Fusiona múltiples fuentes Mihon/Tachiyomi en un solo index.json / index.min.json
"""

import json
import os
import sys
import time
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

# ─── CONFIGURACIÓN ───────────────────────────────────────────────────────────

SOURCES = [
    ("LittleSurvival/copymanga-copy20", "https://raw.githubusercontent.com/LittleSurvival/copymanga-copy20/repo/index.min.json"),
    ("Kareadita/tach-extension", "https://raw.githubusercontent.com/Kareadita/tach-extension/repo/index.min.json"),
    ("Secozzi/aniyomi-extensions", "https://raw.githubusercontent.com/Secozzi/aniyomi-extensions/refs/heads/repo/index.min.json"),
    ("Claudemirovsky/cursedyomi-extensions", "https://raw.githubusercontent.com/Claudemirovsky/cursedyomi-extensions/repo/index.min.json"),
    ("hollow/aniyomi-extensions-fr", "https://codeberg.org/hollow/aniyomi-extensions-fr/media/branch/repo/index.min.json"),
    ("wasu-code/novel-compat-shosetsu", "https://raw.githubusercontent.com/wasu-code/novel-compat-shosetsu/repo/index.min.json"),
    ("novelsourcery/extensions", "https://raw.githubusercontent.com/novelsourcery/extensions/repo/index.min.json"),
    ("FelipeGFA/extensoes", "https://raw.githubusercontent.com/FelipeGFA/extensoes/repo/index.min.json"),
    ("salmanbappi/extensions-repo", "https://raw.githubusercontent.com/salmanbappi/extensions-repo/main/index.min.json"),
]

TIMEOUT = 30
RETRIES = 2
USER_AGENT = "Mozilla/5.0 (compatible; KomizenBot/1.0)"

# ─── FUNCIONES ───────────────────────────────────────────────────────────────

def fetch_json(name, url):
    """Descarga JSON con reintentos."""
    req = Request(url, headers={"User-Agent": USER_AGENT})
    for attempt in range(1, RETRIES + 1):
        try:
            with urlopen(req, timeout=TIMEOUT) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                count = len(data) if isinstance(data, list) else 1
                print(f"  [OK] {name}: {count} entradas")
                return data if isinstance(data, list) else [data]
        except HTTPError as e:
            print(f"  [ERR] {name}: HTTP {e.code} (intento {attempt}/{RETRIES})")
        except URLError as e:
            print(f"  [ERR] {name}: {e.reason} (intento {attempt}/{RETRIES})")
        except json.JSONDecodeError as e:
            print(f"  [ERR] {name}: JSON inválido (intento {attempt}/{RETRIES})")
        except Exception as e:
            print(f"  [ERR] {name}: {e} (intento {attempt}/{RETRIES})")
        if attempt < RETRIES:
            time.sleep(2)
    print(f"  [SKIP] {name}: excluida tras {RETRIES} fallos")
    return []

def merge_entries(entries_list):
    """Fusiona listas eliminando duplicados por campo pkg."""
    seen = {}
    unique = []
    for entry in entries_list:
        pkg = entry.get("pkg", "")
        if not pkg:
            pkg = entry.get("name", "") + "_" + str(entry.get("id", ""))
        if pkg not in seen:
            seen[pkg] = True
            unique.append(entry)
    return unique

# ─── EJECUCIÓN PRINCIPAL ─────────────────────────────────────────────────────

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    repo_dir = os.path.join(script_dir, "repo")
    os.makedirs(repo_dir, exist_ok=True)

    all_entries = []
    ok_count = 0
    fail_count = 0

    print("Komizen Extensions Generator")
    print("=" * 50)
    print(f"Fuentes configuradas: {len(SOURCES)}")
    print()

    for name, url in SOURCES:
        print(f"Descargando {name}...")
        entries = fetch_json(name, url)
        if entries:
            all_entries.extend(entries)
            ok_count += 1
        else:
            fail_count += 1
        time.sleep(0.5)

    print()
    print(f"Descargas exitosas: {ok_count}/{len(SOURCES)}")
    print(f"Descargas fallidas: {fail_count}/{len(SOURCES)}")
    print(f"Total entradas (bruto): {len(all_entries)}")

    merged = merge_entries(all_entries)
    print(f"Total entradas (deduplicado): {len(merged)}")

    # Guardar index.json (legible)
    index_path = os.path.join(repo_dir, "index.json")
    with open(index_path, "w", encoding="utf-8") as f:
        json.dump(merged, f, indent=2, ensure_ascii=False)
    print(f"Guardado: {index_path} ({os.path.getsize(index_path)} bytes)")

    # Guardar index.min.json (minificado)
    min_path = os.path.join(repo_dir, "index.min.json")
    with open(min_path, "w", encoding="utf-8") as f:
        json.dump(merged, f, separators=(",", ":"), ensure_ascii=False)
    print(f"Guardado: {min_path} ({os.path.getsize(min_path)} bytes)")

    print()
    print("Generación completada.")
    return 0 if fail_count == 0 else 1

if __name__ == "__main__":
    sys.exit(main())
