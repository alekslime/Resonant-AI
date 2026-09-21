package com.resonant.app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.net.UnknownHostException

/*
 * Everything the in-app "Server setup" screen needs that isn't UI: turning what a
 * person typed into a usable address, and asking the server whether it is really
 * there and really has the model. Kept out of the screen so it can be unit tested
 * without a device.
 */

private const val DEFAULT_OLLAMA_PORT = 11434
private const val CHECK_TIMEOUT_MS = 5_000

/**
 * Turns whatever was typed into a base URL the client can use, or null if it can't
 * be one. Forgiving on purpose, because this gets typed on a phone keyboard in a hurry:
 *
 *   "192.168.1.50"              -> "http://192.168.1.50:11434"
 *   "192.168.1.50:11434"        -> "http://192.168.1.50:11434"
 *   " http://host:8080/api/ "   -> "http://host:8080/api"
 *   "https://ollama.example.com" -> unchanged (no port forced on https)
 *
 * A bare host gets Ollama's default port, since that is what `ollama serve` uses.
 */
internal fun normalizeBaseUrl(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null

    val withScheme = if ("://" in trimmed) trimmed else "http://$trimmed"
    val uri = try {
        URI(withScheme)
    } catch (e: URISyntaxException) {
        return null
    }

    val scheme = uri.scheme?.lowercase() ?: return null
    if (scheme != "http" && scheme != "https") return null
    val host = uri.host ?: return null

    val port = when {
        uri.port != -1 -> uri.port
        scheme == "http" -> DEFAULT_OLLAMA_PORT
        else -> -1
    }
    val path = uri.rawPath.orEmpty().trimEnd('/')

    return buildString {
        append(scheme).append("://").append(host)
        if (port != -1) append(':').append(port)
        append(path)
    }
}

/** Model names from Ollama's `GET /api/tags` body, e.g. ["llama3.2:latest"]. */
internal fun parseModelNames(json: String): List<String> {
    val models = JSONObject(json).optJSONArray("models") ?: return emptyList()
    return (0 until models.length()).mapNotNull { i ->
        models.optJSONObject(i)?.optString("name")?.takeIf { it.isNotEmpty() }
    }
}

/**
 * Whether [wanted] is among [installed]. Ollama lists "llama3.2" as "llama3.2:latest",
 * so a name typed without a tag matches its `:latest` entry.
 */
internal fun modelIsInstalled(installed: List<String>, wanted: String): Boolean {
    val name = wanted.trim()
    if (name.isEmpty()) return false
    return installed.any {
        it.equals(name, ignoreCase = true) ||
            (':' !in name && it.equals("$name:latest", ignoreCase = true))
    }
}

/** Outcome of [checkOllamaConnection]. */
internal sealed interface ConnectionCheck {
    /** The server answered. [modelInstalled] says whether the wanted model is on it. */
    data class Reachable(val installed: List<String>, val modelInstalled: Boolean) : ConnectionCheck
    data class Unreachable(val reason: Throwable) : ConnectionCheck
}

/**
 * Asks the server at [baseUrl] which models it has. Cheap and read-only — it does not
 * load a model or generate anything, so it is safe to run on every "Test" tap.
 */
internal suspend fun checkOllamaConnection(baseUrl: String, model: String): ConnectionCheck =
    withContext(Dispatchers.IO) {
        try {
            val connection = (URL("$baseUrl/api/tags").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CHECK_TIMEOUT_MS
                readTimeout = CHECK_TIMEOUT_MS
            }
            try {
                val status = connection.responseCode
                if (status != HttpURLConnection.HTTP_OK) {
                    ConnectionCheck.Unreachable(OllamaHttpException(status, null))
                } else {
                    val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val installed = parseModelNames(body)
                    ConnectionCheck.Reachable(installed, modelIsInstalled(installed, model))
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            ConnectionCheck.Unreachable(e)
        } catch (e: JSONException) {
            ConnectionCheck.Unreachable(e)
        }
    }

/**
 * One line for the setup screen. Unlike [spokenErrorFor] this is read by a sighted
 * person setting things up, so it names the likely cause rather than what to tap next.
 */
internal fun describeCheck(result: ConnectionCheck, model: String): String = when (result) {
    is ConnectionCheck.Reachable -> when {
        result.modelInstalled -> "Connected. Model \"${model.trim()}\" is installed."
        result.installed.isEmpty() ->
            "Connected, but the server has no models. On the server run: ollama pull ${model.trim()}"
        else ->
            "Connected, but \"${model.trim()}\" isn't installed. On the server run: " +
                "ollama pull ${model.trim()}\nInstalled: ${result.installed.joinToString(", ")}"
    }
    is ConnectionCheck.Unreachable -> describeFailure(result.reason)
}

internal fun describeFailure(e: Throwable): String = when {
    e is OllamaHttpException -> "The server answered with HTTP ${e.status}."
    e is ConnectException || e is NoRouteToHostException || e is UnknownHostException ->
        "Couldn't reach that address. Check it, and that the phone and the server are on the same Wi-Fi."
    // Timing out usually means something is silently dropping the packets: a firewall,
    // or Ollama listening only on the server itself (the default) instead of the network.
    e is SocketTimeoutException ->
        "No answer in time. Is Ollama running with OLLAMA_HOST=0.0.0.0, and is the " +
            "firewall allowing port $DEFAULT_OLLAMA_PORT?"
    e is JSONException -> "Something answered, but it doesn't look like Ollama."
    e is IOException && e.message?.contains("Cleartext", ignoreCase = true) == true ->
        "This build blocks plain HTTP. Use an https address, or a debug build."
    else -> "Couldn't connect (${e.javaClass.simpleName}: ${e.message ?: "no details"})."
}
