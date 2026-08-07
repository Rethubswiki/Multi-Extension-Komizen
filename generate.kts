# Komizen Extensions Generator - Kotlin 2.4.20-Beta2
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

data class Source(val name: String, val url: String)

private val SOURCES = listOf(
    Source("LittleSurvival/copymanga-copy20", "https://raw.githubusercontent.com/LittleSurvival/copymanga-copy20/repo/index.min.json"),
    Source("Kareadita/tach-extension", "https://raw.githubusercontent.com/Kareadita/tach-extension/repo/index.min.json"),
    Source("Secozzi/aniyomi-extensions", "https://raw.githubusercontent.com/Secozzi/aniyomi-extensions/refs/heads/repo/index.min.json"),
    Source("Claudemirovsky/cursedyomi-extensions", "https://raw.githubusercontent.com/Claudemirovsky/cursedyomi-extensions/repo/index.min.json"),
    Source("hollow/aniyomi-extensions-fr", "https://codeberg.org/hollow/aniyomi-extensions-fr/media/branch/repo/index.min.json"),
    Source("wasu-code/novel-compat-shosetsu", "https://raw.githubusercontent.com/wasu-code/novel-compat-shosetsu/repo/index.min.json"),
    Source("novelsourcery/extensions", "https://raw.githubusercontent.com/novelsourcery/extensions/repo/index.min.json"),
    Source("FelipeGFA/extensoes", "https://raw.githubusercontent.com/FelipeGFA/extensoes/repo/index.min.json"),
    Source("salmanbappi/extensions-repo", "https://raw.githubusercontent.com/salmanbappi/extensions-repo/main/index.min.json"),
    Source("Nyora-Manga/nyora-mihon", "https://raw.githubusercontent.com/Nyora-Manga/nyora-mihon/main/index.min.json"),
    Source("punpunsx/aniyomi-extensions", "https://raw.githubusercontent.com/punpunsx/aniyomi-extensions/repo/index.min.json"),
    Source("kitsumed/mihonyomi-extensions", "https://raw.githubusercontent.com/kitsumed/mihonyomi-extensions/releases/index.min.json"),
)

private const val TIMEOUT_SECONDS = 30L
private const val RETRIES = 2
private const val USER_AGENT = "Mozilla/5.0 (compatible; KomizenBot/1.0)"

private val httpClient: HttpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build()

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
                else -> {
                    println("  [WARN] ${source.name}: respuesta no es array ni object")
                    null
                }
            }
        } else {
            println("  [ERR] ${source.name}: HTTP ${response.statusCode()} (intento $attempt/$RETRIES)")
            retryOrNull(source, attempt)
        }
    } catch (e: Exception) {
        println("  [ERR] ${source.name}: ${e.message} (intento $attempt/$RETRIES)")
        retryOrNull(source, attempt)
    }
}

fun retryOrNull(source: Source, attempt: Int): JsonArray? {
    if (attempt < RETRIES) {
        Thread.sleep(2000)
        return fetchJson(source, attempt + 1)
    }
    println("  [SKIP] ${source.name}: excluida tras $RETRIES fallos")
    return null
}

fun JsonObject.pkgKey(): String {
    return this["pkg"]?.jsonPrimitive?.contentOrNull
        ?: "${this["name"]?.jsonPrimitive?.contentOrNull}_${this["id"]?.jsonPrimitive?.contentOrNull}"
}

fun main() {
    println("Komizen Extensions Generator (Kotlin)")
    println("=".repeat(50))
    println("Fuentes configuradas: ${SOURCES.size}")
    println()

    val allEntries = mutableListOf<JsonObject>()
    var okCount = 0
    var failCount = 0

    for (source in SOURCES) {
        println("Descargando ${source.name}...")
        val entries = fetchJson(source)
        if (entries != null) {
            entries.filterIsInstance<JsonObject>().forEach { allEntries.add(it) }
            println("  [OK] ${source.name}: ${entries.size} entradas")
            okCount++
        } else {
            failCount++
        }
        Thread.sleep(500)
    }

    println()
    println("Descargas exitosas: $okCount/${SOURCES.size}")
    println("Descargas fallidas: $failCount/${SOURCES.size}")
    println("Total entradas (bruto): ${allEntries.size}")

    val seen = mutableSetOf<String>()
    val unique = mutableListOf<JsonObject>()
    for (entry in allEntries) {
        val key = entry.pkgKey()
        if (key !in seen) {
            seen.add(key)
            unique.add(entry)
        }
    }

    println("Total entradas (deduplicado): ${unique.size}")

    val prettyJson = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }
    val minJson = Json { prettyPrint = false }
    val outputArray = JsonArray(unique)

    // ─── 1. repo/ → array plano (formato moderno) ─────────────────────────────
    val repoDir = File("repo").apply { mkdirs() }
    File(repoDir, "index.json").writeText(prettyJson.encodeToString(outputArray))
    File(repoDir, "index.min.json").writeText(minJson.encodeToString(outputArray))
    println("Guardado: ${repoDir}/index.json (${File(repoDir, "index.json").length()} bytes)")
    println("Guardado: ${repoDir}/index.min.json (${File(repoDir, "index.min.json").length()} bytes)")

    // ─── 2. Raíz → array plano (Anizen, Kototoro, apps modernas) ─────────────
    File("array.json").writeText(prettyJson.encodeToString(outputArray))
    File("array.min.json").writeText(minJson.encodeToString(outputArray))
    println("Guardado: array.json (${File("array.json").length()} bytes)")
    println("Guardado: array.min.json (${File("array.min.json").length()} bytes)")

    // ─── 3. Raíz → legacy store (apps antiguas) ─────────────────────────────
    val legacyStore = buildJsonObject {
        put("name", "Komizen Extensions")
        put("website", "https://github.com/Rethubswiki/Multi-Extension-Komizen")
        put("extensions", outputArray)
    }
    File("index.json").writeText(prettyJson.encodeToString(legacyStore))
    File("index.min.json").writeText(minJson.encodeToString(legacyStore))
    println("Guardado: index.json (legacy) (${File("index.json").length()} bytes)")
    println("Guardado: index.min.json (legacy) (${File("index.min.json").length()} bytes)")

    println()
    println("Generación completada.")

    exitProcess(if (failCount > 0) 1 else 0)
}

main()
