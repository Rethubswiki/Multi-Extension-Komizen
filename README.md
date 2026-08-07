```markdown
<div align="center">

# 🌀 Komizen Extensions

Repositorio unificado de extensiones para el ecosistema
**Mihon / Tachiyomi / Aniyomi / Anikku / Kototoro / Komizen-AZ**.

`Kotlin 2.4.20-Beta2` · **14 fuentes** · **~450 extensiones deduplicadas** · 3 formatos

[![Update](https://github.com/Rethubswiki/Multi-Extension-Komizen/actions/workflows/update.yml/badge.svg)](https://github.com/Rethubswiki/Multi-Extension-Komizen/actions/workflows/update.yml)
[![Source Health](https://github.com/Rethubswiki/Multi-Extension-Komizen/actions/workflows/source-health.yml/badge.svg)](https://github.com/Rethubswiki/Multi-Extension-Komizen/actions/workflows/source-health.yml)

</div>

---

## 🔗 URLs de acceso

**Formato array plano en raíz** (Komizen-AZ, Anizen, Kototoro, apps modernas):
```
https://raw.githubusercontent.com/Rethubswiki/Multi-Extension-Komizen/main/array.json
https://raw.githubusercontent.com/Rethubswiki/Multi-Extension-Komizen/main/array.min.json
```

**Formato array plano en `repo/`** (Mihon, TachiyomiSY, Komikku, Dantotsu):
```
https://raw.githubusercontent.com/Rethubswiki/Multi-Extension-Komizen/main/repo/index.min.json
```

**Formato legacy store** (Tachiyomi antiguo, forks desactualizados):
```
https://raw.githubusercontent.com/Rethubswiki/Multi-Extension-Komizen/main/index.json
```

Ruta en la app: `Configuración → Extensiones → Repositorios → +`

---

## 📊 Fuentes integradas (v6 — verificadas 2026-08-07)

🎴 **Manga**
├─ LittleSurvival/copymanga-copy20 (zh)
├─ Kareadita/tach-extension (all)
└─ Nyora-Manga/nyora-mihon (all)

🎬 **Anime**
├─ Secozzi/aniyomi-extensions (all)
├─ Claudemirovsky/cursedyomi-extensions (pt-BR, tr)
├─ hollow/aniyomi-extensions-fr (fr)
├─ salmanbappi/extensions-repo (en, es, all)
├─ punpunsx/aniyomi-extensions (all, multilang)
├─ ✨ **yuzono/anime-repo** (re-activada — la URL correcta funciona)
└─ ✨ **Kholbi/aniyomi-extensions-revived** (nueva)

📖 **Novela / Texto**
├─ wasu-code/novel-compat-shosetsu (all, en)
├─ novelsourcery/extensions (multilang)
└─ kitsumed/mihonyomi-extensions (Komga, all)

ℹ️ **Informativo**
└─ FelipeGFA/extensoes

**Total: ~450 extensiones únicas** (deduplicadas por `pkg`).

---

## 🏗️ Estructura del repositorio

```
.
├── .github/workflows/
│   ├── update.yml                 # CI auto-actualización (cada 12h)
│   └── source-health.yml          # Vigilancia de fuentes (diario)
├── repo/
│   ├── index.json                 # Array plano (pretty)
│   └── index.min.json             # Array plano (minified)
├── sources/                       # Backups individuales por fuente
├── scripts/
│   └── check_sources.py           # Verificador de salud de fuentes
├── generate.kts                   # Generador principal (Kotlin 2.4.20-Beta2)
├── generate_komizen.py            # Generador alternativo (Python)
├── array.json / array.min.json    # Array plano raíz (Komizen-AZ/Anizen)
├── index.json / index.min.json    # Legacy store (apps antiguas)
├── extension.proto                # Schema Protobuf de referencia
├── sources-health-state.json      # Estado de salud (commiteado)
├── README.md
└── .nojekyll
```

---

## 🧬 Schema de una extensión

Cada entrada del índice sigue este contrato (ver `extension.proto`):

```json
{
  "name": "Aniyomi: AnimeOnsen",
  "pkg": "eu.kanade.tachiyomi.animeextension.all.animeonsen",
  "apk": "aniyomi-all.animeonsen-v14.10.apk",
  "lang": "all",
  "code": 10,
  "version": "14.10",
  "nsfw": 0,
  "sources": [
    { "name": "AnimeOnsen", "lang": "all",
      "id": "8542735178285060053", "baseUrl": "https://animeonsen.xyz" }
  ]
}
```

El generador **valida** que cada entrada tenga `name`, `pkg`, `apk`, `lang`,
`code`, `version` y `nsfw`; las que no cumplen se descartan y se reportan.

---

## ⚡ Generación manual

Requisitos: **JDK 21+** y **Kotlin 2.4.20-Beta2**.

```bash
kotlin generate.kts
```

Salida: `array.json`, `array.min.json`, `repo/index.min.json`, `index.json`,
`index.min.json` + un informe por fuente y por tipo.

---

## 🔄 Generación automática

Workflow de GitHub Actions cada **12 horas** (`cron: 0 */12 * * *`) o bajo
demanda. Regenera los tres formatos a partir de las fuentes vivas.

---

## 🩺 Vigilancia de salud de fuentes

El workflow **`source-health.yml`** se ejecuta **a diario** y:

1. Comprueba cada URL de `generate.kts` (HTTP + JSON + schema).
2. Detecta **caídas nuevas** y **fuentes recuperadas** vs. el estado anterior.
3. **Abre/actualiza un issue** etiquetado `source-health` si hay fuentes degradadas.
4. **Lo cierra** automáticamente cuando todo vuelve a estar sano.
5. Commitea `sources-health-state.json` para la próxima comparación.

Ejecutar localmente:

```bash
pip install requests
python scripts/check_sources.py
# → health-report.md · health-summary.json · sources-health-state.json
```

Estados posibles:

| Estado | Significado |
|---|---|
| ✅ `ok` | Fuente sana con extensiones válidas |
| 🟡 `low` | Muy pocas entradas (posible stub informativo) |
| 🟠 `empty` | Responde pero sin extensiones válidas |
| ❌ `http_error` / `not_json` | Error HTTP o respuesta no JSON |
| ⏱️ `timeout` / 🔌 `network` | Sin respuesta / error de red |

---

## 🛡️ Notas técnicas

- **Deduplicación** por campo `pkg` (fallback a `name_id`).
- Si una fuente falla, el script **continúa** con las restantes.
- Los **APKs no se alojan aquí**; solo se indexan metadatos.
- Tres formatos generados para máxima compatibilidad:
  - `array.json` → raíz (Komizen-AZ, Anizen, Kototoro)
  - `repo/index.min.json` → subcarpeta (Mihon moderno)
  - `index.json` → legacy store (apps antiguas)
- El verificador de salud marca como `low` las fuentes con <3 entradas
  (p. ej. stubs informativos), para distinguirlas de una caída real.

---

## ❌ Fuentes excluidas (verificadas 2026-08-07)

| Fuente | Razón |
|---|---|
| almightyhak/aniyomi-anime-repo | 404 (verificado) |
| zosetsu-repo/tachi-repo | 404 (verificado) |
| yuzono/manga-repo | 404 (verificado; la de anime sí funciona) |
| Sadwhy/aniyomi-extensions | 404 (verificado) |
| Kohi-Den (kohiden.xyz) | Inaccesible |
| keiyoushi/extensions | Índice con solo 2 entradas informativas (migró a Mihon 0.20.1+) |
| Suwayomi/tachiyomi-extension | Error de red persistente |
| CranberrySoup/AniyomiCompatExtension | Error de red persistente |
| self-similarity/MegaRepo | Error de red persistente |
| InvalidDavid/UMA | Error de red persistente |
| dragonx943/manga-repo | Error de red persistente |
| mojuru/cursed-manga-repo | Solo mensajes informativos |
| yuzono/anime-extensoes · LNReader/lnreader-plugins | 404 |
| kotatsu-83g/* (5 fuentes) | Formato Kotatsu (incompatible Mihon) |

---

## ⚖️ Aviso legal

- Este repositorio **no aloja contenido** ni APKs: solo indexa metadatos de
  extensiones creadas por terceros.
- Cada extensión es responsabilidad de su autor y de la fuente a la que accede.
- No hay afiliación con AniList, MyAnimeList, ni con ningún proveedor de contenido.

---

## 🤝 Contribuir

1. Para **añadir una fuente**, agrega una entrada `Source(...)` en `generate.kts`
   y verifica que esté viva ejecutando `python scripts/check_sources.py`.
2. Para **reportar una fuente caída**, abre un issue con la etiqueta `source-health`.
3. El CI regenera los índices automáticamente cada 12h.

---

<div align="center">

**Hecho para la comunidad de lectores** · Komizen Extensions

</div>
```

---
