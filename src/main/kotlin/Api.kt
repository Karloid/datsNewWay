import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


//val BASE_URL = "https://games-test.datsteam.dev"
var BASE_URL = "https://games.datsteam.dev"

val worldsResp = ArrayDeque<WorldStateDto>()

object Api {

    val gson = Gson()

    // pretty print gson
    val gsonPretty = GsonBuilder().setPrettyPrinting().create()

    val okHttpClient = OkHttpClient()


    /**
     *
     * get https://games-test.datsteam.dev/rounds/zombidef
     * {
     * "gameName": "defense",
     * "now": "2021-01-01T00:00:00Z",
     * "rounds": [
     * {
     * "duration": 60,
     * "endAt": "2021-01-01T00:00:00Z",
     * "name": "Round 1",
     * "repeat": 1,
     * "startAt": "2021-01-01T00:00:00Z",
     * "status": "active"
     * }
     * ]
     * }
     */

    fun getRounds(): RoundsDto {
        throttle()


        val path = "/rounds/snake3d"

        if (REPEAT_MODE) {
            // find files with this path in name in folder response and answer with it
            val files = File("responses").listFiles()
            val escapedPath = path.replace("/", "_")
            val file = files?.firstOrNull { it.name.contains(escapedPath) }
            if (file != null) {
                val body = file.readText()
                val result = gson.fromJson(body, RoundsDto::class.java) ?: throw Exception("Failed to parse response body=$body")
                return result
            }
        }

        val request = okhttp3.Request.Builder()
            .url("$BASE_URL$path")
            .get()
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Auth-Token", apiToken)
            .build()

        val call = okHttpClient.newCall(request)

        val response = call.execute()

        if (response.isSuccessful.not()) {
            throw Exception("Failed to execute request response=$response body=${response.body?.string()}")
        }

        val body = response.body?.string()

        val result = gson.fromJson(body, RoundsDto::class.java) ?: throw Exception("Failed to parse response response=$response body=$body")

      //  saveResponse(path, body)
        return result
    }

    private fun saveResponse(path: String, body: String?) {
        body ?: return

        if (!SAVE_RESPONSES) {
            return
        }

        val fileName = System.currentTimeMillis().toString() + "-" + path.replace("/", "_") + ".txt"
        val file = File("responses/$fileName")
        file.writeText(body)
        

    }

    val lastRequest = ArrayDeque<Long>()

    /**
     * we should not fire more than 4 request per second
     */
    private fun throttle() {
        val currentTs = System.currentTimeMillis()

        // remove requests older than 1 second

        while (lastRequest.isNotEmpty() && currentTs - lastRequest.first() > 1000) {
            lastRequest.removeFirst()
        }

        if (lastRequest.size < 3) {
            lastRequest.add(currentTs)
            return
        }

        val sleepTime = 1000 - (currentTs - lastRequest.first())

        log("throttle sleep for $sleepTime")
        Thread.sleep(sleepTime)

        lastRequest.add(System.currentTimeMillis())
    }


    fun move(transports: List<SnakeCommandDto>): WorldStateDto {

        throttle()
        val path = "/play/snake3d/player/move"

        if (REPEAT_MODE) {
            if (worldsResp.isEmpty()) {
                // read responses from folder and add to queue
                // ignore files with _req in name
                val escapedPath = path.replace("/", "_")
                val files = File("responses").listFiles()?.toList()?.sortedBy { it.absolutePath }
                files?.forEach { file ->
                    if (file.name.contains(escapedPath) && file.name.contains("_req").not()) {
                        val body = file.readText()
                        val result = gson.fromJson(body, WorldStateDto::class.java) ?: throw Exception("Failed to parse response body=$body")
                        worldsResp.add(result)
                    }
                }
            }

            return worldsResp.removeFirst()
        }

        val json = gson.toJson(CommandsDto(transports))

        saveResponse(path+"_req", json)

        //  log(json)

        val body = okhttp3.RequestBody.create("application/json".toMediaTypeOrNull(), json)


        val request = okhttp3.Request.Builder()
            .url("$BASE_URL$path")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Auth-Token", apiToken)
            .build()

        val call = okHttpClient.newCall(request)

        val response = call.execute()

        if (response.isSuccessful.not()) {
            throw Exception("Failed to execute request response=$response body=${response.body?.string()}")
        }

        val bodyResponse = response.body?.string()


        //  log(bodyResponse)
        saveResponse(path, bodyResponse)
        
        val result = gson.fromJson(bodyResponse, WorldStateDto::class.java) ?: throw Exception("Failed to parse response response=$response body=$bodyResponse")

        //  log(result)

        return result

    }
}


data class RoundsDto(
    var gameName: String,
    var now: String,
    var rounds: List<Round>
) {

    fun getNowAsLong(): Long {
        val stringDate = now
        return toMillis(stringDate)
    }
}

data class Round(
    var duration: Int,
    var endAt: String,
    var name: String,
    var repeat: Int,
    var startAt: String,
    var status: String
) {

    fun getEndAsLong(): Long {
        val stringDate = endAt
        return toMillis(stringDate)
    }

    fun getStartAsLong(): Long {
        val stringDate = startAt
        return toMillis(stringDate)
    }

    override fun toString(): String {
        return "Round( name='$name' duration=$duration, startAt=${getStartAsLong().toDate()} endAt=${getEndAsLong().toDate()}, repeat=$repeat, , status='$status')"
    }
}


private fun toMillis(stringDate: String) = LocalDateTime.parse(stringDate, DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneOffset.UTC).toEpochMilli()


fun Long.toDate(): String {
    return LocalDateTime.ofEpochSecond(this / 1000, 0, ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)
}
