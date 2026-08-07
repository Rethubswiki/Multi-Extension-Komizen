# 🌀 Komizen Extensions

> Repositorio unificado de extensiones para el ecosistema Mihon / Tachiyomi / Aniyomi / Anizen / Kototoro.
> Kotlin 2.4.20-Beta2 · 12 fuentes · 429 extensiones deduplicadas.

---

## 🔗 URLs de acceso

**Formato array plano en raíz** (Anizen, Kototoro, apps modernas):
```
https://raw.githubusercontent.com/Rethubswiki/Multi-Extension-Komizen/main/array.json
https://raw.githubusercontent.com/Rethubswiki/Multi-Extension-Komizen/main/array.min.json
```

**Formato array plano en repo/** (Mihon, TachiyomiSY, Komikku, Dantotsu):
```
https://raw.githubusercontent.com/Rethubswiki/Multi-Extension-Komizen/main/repo/index.min.json
```

**Formato legacy store** (Tachiyomi antiguo, forks desactualizados):
```
https://raw.githubusercontent.com/Rethubswiki/Multi-Extension-Komizen/main/index.json
```

Ruta en app: `Configuración → Extensiones → Repositorios → +`

---

## 📊 Fuentes integradas

🎴 **Manga**
├─ LittleSurvival/copymanga-copy20 (zh) · 4 entradas
├─ Kareadita/tach-extension (all) · 1 entrada
└─ Nyora-Manga/nyora-mihon (all) · 2 entradas

🎬 **Anime**
├─ Secozzi/aniyomi-extensions (all) · 3 entradas
├─ Claudemirovsky/cursedyomi-extensions (pt-BR, tr) · 31 entradas
├─ hollow/aniyomi-extensions-fr (fr) · 10 entradas
├─ salmanbappi/extensions-repo (en, es, all) · 56 entradas
└─ punpunsx/aniyomi-extensions (all, multilang) · 217 entradas

📖 **Novel / Texto**
├─ wasu-code/novel-compat-shosetsu (all, en) · 3 entradas
├─ novelsourcery/extensions (multilang) · 142 entradas
└─ kitsumed/mihonyomi-extensions (Komga, all) · 1 entrada

ℹ️ **Informativo**
└─ FelipeGFA/extensoes · 2 entradas

**Total: 429 extensiones únicas** (deduplicadas por `pkg`)

---

## 🏗️ Estructura del repositorio

```
.
├── .github/workflows/update.yml   # CI auto-actualización
├── repo/
│   ├── index.json                 # Array plano (pretty)
│   └── index.min.json             # Array plano (minified)
├── sources/                        # Backups individuales (12 fuentes)
├── generate.kts                    # Generador Kotlin 2.4.20-Beta2
├── array.json                      # Array plano raíz (Anizen/Kototoro)
├── array.min.json                  # Array plano raíz minified
├── index.json                      # Legacy store (pretty)
├── index.min.json                  # Legacy store (minified)
├── README.md
└── .nojekyll
```

---

## ⚡ Generación manual

Requisitos: JDK 21+ y Kotlin 2.4.20-Beta2

```bash
kotlin generate.kts
```

## 🔄 Generación automática

Workflow GitHub Actions cada 12 horas (`cron: 0 */12 * * *`) o bajo demanda.

---

## 🛡️ Notas técnicas

- Deduplicación por campo `pkg` con fallback a `name_id`.
- Si una fuente falla, el script continúa con las restantes.
- Los APKs no se alojan en este repo; solo se indexan metadatos.
- Tres formatos generados para máxima compatibilidad:
  · `array.json` → array plano en raíz (Anizen, Kototoro)
  · `repo/index.min.json` → array plano en subcarpeta (Mihon moderno)
  · `index.json` → legacy store (apps antiguas)

---

## ❌ Fuentes excluidas

| Fuente | Razón |
|--------|-------|
| Suwayomi/tachiyomi-extension | Error de red persistente |
| CranberrySoup/AniyomiCompatExtension | Error de red persistente |
| self-similarity/MegaRepo | Error de red persistente |
| InvalidDavid/UMA | Error de red persistente |
| dragonx943/manga-repo | Error de red persistente |
| keiyoushi/extensions | Solo mensajes informativos (sin extensiones reales) |
| mojuru/cursed-manga-repo | Solo mensajes informativos |
| yuzono/manga-repo | 404 |
| yuzono/anime-repo | 404 |
| FelipeGFA/anime-extensoes | 404 |
| LNReader/lnreader-plugins | 404 |
| kotatsu-83g/* (5 fuentes) | Formato Kotatsu (incompatible Mihon) |
