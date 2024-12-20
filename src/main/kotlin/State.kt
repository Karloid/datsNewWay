import java.io.File

data class State(var currentRound: String? = "") {
    fun save() {
        File("state.json").writeText(Api.gson.toJson(this))
    }
}