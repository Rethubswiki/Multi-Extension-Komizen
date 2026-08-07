#!/usr/bin/env python3
"""
Verifica el estado de todas las fuentes de Multi-Extension-Komizen.

- Lee la lista de fuentes directamente de generate.kts (fuente única de verdad).
- Comprueba cada URL (HTTP, JSON, schema).
- Genera un informe Markdown + estado JSON.
- Detecta regresiones vs. el estado anterior (nuevas caídas / recuperadas).

Uso:
    python scripts/check_sources.py
Salidas:
    health-report.md           informe legible
    sources-health-state.json  estado nuevo (se commitea)
    health-summary.json        resumen legible por el workflow
"""
import argparse
import json
import re
import sys
import time
from pathlib import Path

try:
    import requests
except ImportError:
    print("Falta 'requests'. Instálalo con: pip install requests")
    sys.exit(2)

REQUIRED = {"name", "pkg", "apk", "lang", "code", "version", "nsfw"}
HEADERS = {"User-Agent": "Mozilla/5.0 (compatible; KomizenHealthBot/1.0)"}
TIMEOUT = 30
RETRIES = 2
LOW_COUNT_THRESHOLD = 3   # menos de N entradas → stub informativo (ej. keiyoushi)

STATUS_ICON = {
    "ok": "✅", "low": "🟡", "empty": "🟠",
    "http_error": "❌", "not_json": "❌",
    "timeout": "⏱️", "network": "🔌",
}


def parse_sources(generate_path: Path):
    """Extrae Source("name", "url", Kind.X) de generate.kts."""
    text = generate_path.read_text(encoding="utf-8")
    pattern = re.compile(r'Source\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*Kind\.(\w+)\s*\)')
    return [
        {"name": n, "url": u, "kind": k}
        for n, u, k in pattern.findall(text)
    ]


def check_source(src):
    url = src["url"]
    for attempt in range(1, RETRIES + 1):
        try:
            r = requests.get(url, headers=HEADERS, timeout=TIMEOUT)
            if r.status_code != 200:
                if attempt < RETRIES:
                    time.sleep(2); continue
                return {"status": "http_error", "detail": f"HTTP {r.status_code}"}
            try:
                data = r.json()
            except Exception:
                return {"status": "not_json", "detail": "La respuesta no es JSON válido"}

            entries = data if isinstance(data, list) else ([data] if isinstance(data, dict) else [])
            valid = [
                e for e in entries
                if isinstance(e, dict) and REQUIRED.issubset(e.keys()) and e.get("pkg")
            ]
            nv, total = len(valid), len(entries)

            if nv == 0:
                return {"status": "empty", "detail": f"0 extensiones válidas de {total}"}
            if nv < LOW_COUNT_THRESHOLD:
                return {"status": "low", "detail": f"Solo {nv} entradas (posible stub)"}
            return {"status": "ok", "detail": f"{nv} extensiones"}

        except requests.exceptions.Timeout:
            if attempt < RETRIES:
                time.sleep(2); continue
            return {"status": "timeout", "detail": f"Timeout tras {TIMEOUT}s"}
        except Exception as e:
            if attempt < RETRIES:
                time.sleep(2); continue
            return {"status": "network", "detail": str(e)[:120]}
    return {"status": "network", "detail": "Fallo desconocido"}


def load_prev_state(path: Path):
    if path.exists():
        try:
            return json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            return {}
    return {}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--generate", default="generate.kts")
    ap.add_argument("--state", default="sources-health-state.json")
    ap.add_argument("--report", default="health-report.md")
    ap.add_argument("--summary", default="health-summary.json")
    args = ap.parse_args()

    sources = parse_sources(Path(args.generate))
    print(f"Fuentes a verificar: {len(sources)}")

    results = {}
    for src in sources:
        print(f"  → {src['name']} ...", end=" ", flush=True)
        res = check_source(src)
        results[src["name"]] = {**res, "url": src["url"], "kind": src["kind"]}
        print(f"{STATUS_ICON[res['status']]} {res['status']} ({res['detail']})")
        time.sleep(0.5)

    prev = load_prev_state(Path(args.state))

    # Detección de regresiones
    newly_failed, recovered, still_ok = [], [], []
    for name, res in results.items():
        was = prev.get(name, {}).get("status")
        is_ok = res["status"] == "ok"
        if is_ok:
            (recovered if was and was != "ok" else still_ok).append(name)
        else:
            if was == "ok" or was is None:
                newly_failed.append(name)

    # ── Informe Markdown ─────────────────────────────────────────────────────
    lines = [
        "# 🩺 Informe de salud de fuentes",
        "",
        f"_Generado automáticamente el {time.strftime('%Y-%m-%d %H:%M UTC', time.gmtime())}_",
        "",
        "| Estado | Fuente | Tipo | Detalle |",
        "|---|---|---|---|",
    ]
    for name, res in results.items():
        icon = STATUS_ICON[res["status"]]
        lines.append(f"| {icon} `{res['status']}` | {name} | {res['kind']} | {res['detail']} |")

    ok_count = sum(1 for r in results.values() if r["status"] == "ok")
    lines += [
        "",
        f"**Resumen:** {ok_count}/{len(results)} fuentes sanas.",
    ]
    if newly_failed:
        lines += ["", "## ⚠️ Caídas nuevas", ""] + [f"- `{n}`" for n in newly_failed]
    if recovered:
        lines += ["", "## ♻️ Recuperadas", ""] + [f"- `{n}`" for n in recovered]

    Path(args.report).write_text("\n".join(lines) + "\n", encoding="utf-8")

    # ── Estado + resumen legible por el workflow ─────────────────────────────
    Path(args.state).write_text(
        json.dumps(results, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    summary = {
        "total": len(results),
        "ok": ok_count,
        "failed": len(results) - ok_count,
        "newly_failed": newly_failed,
        "recovered": recovered,
    }
    Path(args.summary).write_text(json.dumps(summary, indent=2), encoding="utf-8")

    print("\n" + "=" * 50)
    print(f"Sanas: {ok_count}/{len(results)} · Caídas nuevas: {len(newly_failed)}")
    # Exit 0 siempre: el workflow decide si abrir issue (no romper el job)
    return 0


if __name__ == "__main__":
    sys.exit(main())
