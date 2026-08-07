# KomizenExtensionsGenerator - Kotlin 2.4.20-Beta2

@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

import kotlinx.serialization.json.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.io.File
import kotlin.system.exitProcess

/** `kind` clasifica la fuente para el informe y futuros filtros. */
enum class Kind { MANGA, ANIME, NOVEL, INFO }

data class Source(val name: String, val url: String, val kind: Kind)

// ═══════════════════════════════════════════════════════════════════════════
//  FUENTES — verificadas en vivo el 2026-08-07.
//  ✓ = añadida en esta tanda.  No incluyo URLs muertas (ver auditoría).
// ═══════════════════════════════════════════════════════════════════════════
private val SOURCES = listOf(
    // ── Manga ────────────────────────────────────────────────────────────────
    Source("LittleSurvival/copymanga-copy20", "https://raw.githubusercontent.com/LittleSurvival/copymanga-copy20/repo/index.min.json", Kind.MANGA),
    Source("Kareadita/tach-extension",        "https://raw.githubusercontent.com/Kareadita/tach-extension/repo/index.min.json", Kind.MANGA),
    Source("Nyora-Manga/nyora-mihon",         "https://raw.githubusercontent.com/Nyora-Manga/nyora-mihon/main/index.min.json", Kind.MANGA),

    // ── Anime ────────────────────────────────────────────────────────────────
    Source("Secozzi/aniyomi-extensions",      "https://raw.githubusercontent.com/Secozzi/aniyomi-extensions/refs/heads/repo/index.min.json", Kind.ANIME),
    Source("Claudemirovsky/cursedyomi-extensions", "https://raw.githubusercontent.com/Claudemirovsky/cursedyomi-extensions/repo/index.min.json", Kind.ANIME),
    Source("hollow/aniyomi-extensions-fr",    "https://codeberg.org/hollow/aniyomi-extensions-fr/media/branch/repo/index.min.json", Kind.ANIME),
    Source("salmanbappi/extensions-repo",     "https://raw.githubusercontent.com/salmanbappi/extensions-repo/main/index.min.json", Kind.ANIME),
    Source("punpunsx/aniyomi-extensions",     "https://raw.githubusercontent.com/punpunsx/aniyomi-extensions/repo/index.min.json", Kind.ANIME),

    // ✓ NUEVAS (verificadas hoy):
    Source("yuzono/anime-repo",               "https://raw.githubusercontent.com/yuzono/anime-repo/repo/index.min.json", Kind.ANIME),
    Source("Kholbi/aniyomi-extensions-revived","https://raw.githubusercontent.com/Kholbi/aniyomi-extensions-revived/repo/index.min.json", Kind.ANIME),

    // ── Novela / Texto ───────────────────────────────────────────────────────
    Source("wasu-code/novel-compat-shosetsu", "https://raw.githubusercontent.com/wasu-code/novel-compat-shosetsu/repo/index.min.json", Kind.NOVEL),
    Source("novelsourcery/extensions",        "https://raw.githubusercontent.com/novelsourcery/extensions/repo/index.min.json", Kind.NOVEL),
    Source("kitsumed/mihonyomi-extensions",   "https://raw.githubusercontent.com/kitsumed/mihonyomi-extensions/releases/index.min.json", Kind.NOVEL),

    // ── Informativo / otros ──────────────────────────────────────────────────
    Source("FelipeGFA/extensoes",             "https://raw.githubusercontent.com/FelipeGFA/extensoes/repo/index.min.json", Kind.INFO),
)

// Fuentes verificadas como MUERTAS hoy → no se incluyen (evita reintentos inútiles):
//   almightyhak/aniyomi-anime-repo (404), zosetsu-repo/tachi-repo (404),
//   yuzono/manga-repo (404), Sadwhy/aniyomi-extensions (404), Kohi-Den (inaccesible).
// Keiyoushi: su index.min.json solo trae 2 entradas informativas (migración a Mihon 0.20.1+).

private const val TIMEOUT_SECONDS = 30L
private const val RETRIES = 2
private const val USER_AGENT = "Mozilla/5.0 (compatible; KomizenBot/1.0)"

private val httpClient: HttpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build()

// ═══════════════════════════════════════════════════════════════════════════
//  Validación de schema: descarta entradas que no cumplen el contrato.
// ═══════════════════════════════════════════════════════════════════════════
private val REQUIRED = setOf("name", "pkg", "apk", "lang", "code", "version", "nsfw")

fun JsonObject.isValidExtension(): Boolean {
    val keys = this.keys
    return REQUIRED.all { it in keys } &&
        this["pkg"]?.jsonPrimitive?.contentOrNull != null &&
        this["pkg"]?.jsonPrimitive?.contentOrNull != ""
}

fun fetchJson(source: Source, attempt: Int = 1): JsonArray? {
    val request = HttpRequest.newBuilder()
        .uri(URI.create(source.url))
        .header("User-Agent", USER_AGENT)
        .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
        .GET()
        .build()
    return try {
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() == 200) {
            when (val element = Json.parseToJsonElement(response.body())) {
                is JsonArray -> element
                is JsonObject -> JsonArray(listOf(element))
                else -> { println("[WARN] ${source.name}: no es array ni object"); null }
            }
        } else {
            println("[ERR] ${source.name}: HTTP ${response.statusCode()} (${attempt}/${RETRIES})")
            retryOrNull(source, attempt)
        }
    } catch (e: Exception) {
        println("[ERR] ${source.name}: ${e.message} (${attempt}/${RETRIES})")
        retryOrNull(source, attempt)
    }
}

fun retryOrNull(source: Source, attempt: Int): JsonArray? {
    if (attempt < RETRIES) { Thread.sleep(2000); return fetchJson(source, attempt + 1) }
    println("[SKIP] ${source.name}: excluida tras $RETRIES fallos")
    return null
}

fun JsonObject.pkgKey(): String =
    this["pkg"]?.jsonPrimitive?.contentOrNull
        ?: "${this["name"]?.jsonPrimitive?.contentOrNull}_${this["id"]?.jsonPrimitive?.contentOrNull}"

fun main() {
    println("Komizen Extensions Generator (Kotlin) — fuentes verificadas")
    println("=".repeat(60))
    println("Fuentes configuradas: ${SOURCES.size}")
    println()

    val allEntries = mutableListOf<JsonObject>()
    val perSourceReport = mutableListOf<Triple<String, Kind, Int>>()
    var okCount = 0; var failCount = 0

    for (source in SOURCES) {
        print("Descargando ${source.name} ... ")
        val entries = fetchJson(source)
        if (entries != null) {
            // Validar schema y añadir solo las correctas
            val valid = entries.filterIsInstance<JsonObject>().filter { it.isValidExtension() }
            val dropped = entries.size - valid.size
            allEntries.addAll(valid)
            perSourceReport.add(Triple(source.name, source.kind, valid.size))
            println("OK: ${valid.size} válidas" + if (dropped > 0) " (+$dropped descartadas)" else "")
            okCount++
        } else {
            perSourceReport.add(Triple(source.name, source.kind, 0))
            failCount++
        }
        Thread.sleep(500)
    }

    println()
    println("Descargas exitosas: $okCount/${SOURCES.size} · fallidas: $failCount")
    println("Entradas brutas: ${allEntries.size}")

    // Deduplicar por pkg
    val seen = mutableSetOf<String>()
    val unique = mutableListOf<JsonObject>()
    for (entry in allEntries) {
        val key = entry.pkgKey()
        if (key !in seen) { seen.add(key); unique.add(entry) }
    }
    println("Entradas únicas (dedup): ${unique.size}")

    val prettyJson = Json { prettyPrint = true; prettyPrintIndent = "" }
    val minJson = Json { prettyPrint = false }
    val outputArray = JsonArray(unique)

    // 1. repo/ → array plano (Mihon moderno)
    val repoDir = File("repo").apply { mkdirs() }
    File(repoDir, "index.json").writeText(prettyJson.encodeToString(outputArray))
    File(repoDir, "index.min.json").writeText(minJson.encodeToString(outputArray))

    // 2. Raíz → array plano (Anizen/Kototoro/Komizen-AZ)
    File("array.json").writeText(prettyJson.encodeToString(outputArray))
    File("array.min.json").writeText(minJson.encodeToString(outputArray))

    // 3. Raíz → legacy store
    val legacyStore = buildJsonObject {
        put("name", "Komizen Extensions")
        put("website", "https://github.com/Rethubswiki/Multi-Extension-Komizen")
        put("extensions", outputArray)
    }
    File("index.json").writeText(prettyJson.encodeToString(legacyStore))
    File("index.min.json").writeText(minJson.encodeToString(legacyStore))

    // 4. Informe por fuente y por tipo
    println()
    println("Informe por fuente:")
    perSourceReport.forEach { (n, k, c) -> println("  [${k}] $n → $c") }
    println("Por tipo: " + Kind.entries.joinToString { k ->
        "$k=${unique.count { it["sources"] != null } .let { perSourceReport.filter { p -> p.second == k }.sumOf { p -> p.third } }}"
    })

    println()
    println("Generación completada.")
    exitProcess(if (failCount > 0) 1 else 0)
}

main()
