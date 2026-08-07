<div align="center">

# 🌀 Komizen Extensions

Repositorio unificado de extensiones para el ecosistema
**Mihon / Tachiyomi / Aniyomi / Anikku / Kototoro / Komizen-AZ**.

`Kotlin 2.4.20-Beta2` · **14 fuentes** · **~450 extensiones deduplicadas** · 3 formatos

</div>

---

## 🔗 URLs de acceso

**Array plano en raíz** (Komizen-AZ, Anizen, Kototoro):

```
https://raw.githubusercontent.com/Rethubswiki/Multi-Extension-Komizen/main/array.min.json
```

**Array plano en `repo/`** (Mihon, TachiyomiSY, Komikku, Dantotsu):

```
https://raw.githubusercontent.com/Rethubswiki/Multi-Extension-Komizen/main/repo/index.min.json
```

**Legacy store** (Tachiyomi antiguo):

```
https://raw.githubusercontent.com/Rethubswiki/Multi-Extension-Komizen/main/index.json
```

Ruta en la app: `Configuración → Extensiones → Repositorios → +`

---

## 📊 Fuentes integradas (v6 — verificadas 2026-08-07)

| Tipo | Fuentes |
|---|---|
| 🎴 Manga | LittleSurvival/copymanga-copy20 · Kareadita/tach-extension · Nyora-Manga/nyora-mihon |
| 🎬 Anime | Secozzi · Claudemirovsky · hollow (fr) · salmanbappi · punpunsx · ✨ yuzono/anime-repo · ✨ Kholbi/aniyomi-extensions-revived |
| 📖 Novela | wasu-code/novel-compat-shosetsu · novelsourcery/extensions · kitsumed/mihonyomi-extensions |
| ℹ️ Info | FelipeGFA/extensoes |

**Total: ~450 extensiones únicas** (deduplicadas por `pkg`).

---

## 🏗️ Estructura del repositorio

```
.
├── .github/workflows/
│   ├── update.yml            # auto-actualización (cada 12h)
│   ├── deploy-pages.yml      # despliegue
│   └── source-health.yml     # vigilancia de fuentes (diario)
├── repo/                     # array plano (Mihon moderno)
├── sources/                  # backups individuales por fuente
├── scripts/check_sources.py  # verificador de salud
├── generate.kts              # generador principal (Kotlin)
├── generate_komizen.py       # generador alternativo (Python)
├── array.json / array.min.json
├── index.json / index.min.json
├── extension.proto           # schema de referencia
├── sources-health-state.json # estado de salud (versionado)
└── README.md
~~~

---

## 🧬 Schema de una extensión

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
    {
      "name": "AnimeOnsen",
      "lang": "all",
      "id": "8542735178285060053",
      "baseUrl": "https://animeonsen.xyz"
    }
  ]
}
```

El generador valida `name`, `pkg`, `apk`, `lang`, `code`, `version` y `nsfw`.

---

## ⚡ Generación manual

Requisitos: **JDK 21+** y **Kotlin 2.4.20-Beta2**.

~~~
kotlin generate.kts
```

---

## 🔄 Generación automática

Workflow cada **12 horas** (`cron: 0 */12 * * *`) o bajo demanda.

---

## 🩺 Vigilancia de salud de fuentes

`source-health.yml` corre **a diario**: comprueba cada URL, detecta caídas
nuevas/recuperadas, abre/actualiza un issue `source-health` y commitea el estado.

Ejecutar localmente:

```
pip install requests
python scripts/check_sources.py
~~~

| Estado | Significado |
|---|---|
| ✅ `ok` | Fuente sana |
| 🟡 `low` | Pocas entradas (posible stub) |
| 🟠 `empty` | Responde sin extensiones |
| ❌ `http_error` / `not_json` | Error HTTP o JSON inválido |
| ⏱️ `timeout` / 🔌 `network` | Sin respuesta |

---

## ❌ Fuentes excluidas (verificadas 2026-08-07)

| Fuente | Razón |
|---|---|
| almightyhak · zosetsu · yuzono/manga-repo · Sadwhy | 404 |
| Kohi-Den | Inaccesible |
| keiyoushi/extensions | Solo 2 entradas informativas (migró a Mihon 0.20.1+) |
| Suwayomi · CranberrySoup · MegaRepo · UMA · dragonx943 | Error de red |
| kotatsu-83g/* | Formato incompatible |

---

## ⚖️ Aviso legal

No aloja contenido ni APKs: solo indexa metadatos de extensiones de terceros.

---

## 🤝 Contribuir

1. Añade `Source(...)` en `generate.kts` y verifica con `python scripts/check_sources.py`.
2. Reporta fuentes caídas con la etiqueta `source-health`.
3. El CI regenera los índices cada 12h.

---

<div align="center">

**Hecho para la comunidad de lectores** · Komizen Extensions

</div>
