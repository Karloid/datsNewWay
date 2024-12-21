import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.swing.Timer
import kotlin.collections.ArrayDeque


var lastAction: ActionPlanned? = null

data class ActionPlanned(var commands: List<SnakeCommandDto> = emptyList()) {

}

lateinit var state: State
lateinit var ui: Ui

@Volatile
var apiToken = ""


@Volatile
var SAVE_RESPONSES = false

@Volatile
var REPEAT_MODE = false

@Volatile
var DRAW_UI = false

val logicThread = Executors.newSingleThreadScheduledExecutor()


val stats = Stats()

class Stats {


    @Volatile
    var requestStateTook: Long = 0
    val tmpSims: MutableList<SimsInfo> = mutableListOf()

    @Volatile
    var sims: List<SimsInfo>? = null

    var tmpLogicToDraw: LogicToDraw? = null
    var logicToDraw: LogicToDraw? = null
    var requestTook: Long = 0

    @Volatile
    var logicTookMs: Long = 0

    @Volatile
    var badMoves: Long = 0

    @Volatile
    var successMoves = 0
}


@Volatile
var currentWorldState: WorldStateDto? = null

fun main(args: Array<String>) {
    logicThread.execute {
        try {
            apiToken = args.first()
            BASE_URL = args[1]
            DRAW_UI = args.contains("draw_ui")
            REPEAT_MODE = args.contains("repeat_mode")
            SAVE_RESPONSES = args.contains("save_responses")
            realMain()
        } catch (e: Throwable) {
            e.printStackTrace()
            logicThread.shutdown()
        }
    }

    logicThread.awaitTermination(Int.MAX_VALUE.toLong(), TimeUnit.DAYS)
}

private fun realMain() {
    println("Hello World!")
    drawUi()

    //loadState()

    globalLoop()
}

private fun globalLoop() {
    runCatching { doLoop() }
        .onFailure {
            logicThread.schedule({ globalLoop() }, 1, TimeUnit.SECONDS)
            System.err.println("doLoop failed " + it.stackTraceToString())
            return
        }
}

fun doLoop() {
    /*val roundsInfo = Api.getRounds()

    val rounds = roundsInfo.rounds.sortedBy { it.getStartAsLong() }
    val activeRound = rounds.firstOrNull { it.status == "active" }
*/
    actualStrategy()
}

fun actualStrategy() {
    var startRequest = System.currentTimeMillis()
    currentWorldState = Api.move(transports = emptyList())
    stats.requestStateTook = System.currentTimeMillis() - startRequest
    log("stateMove= tickRemainMs=${currentWorldState?.tickRemainMs} requestStateTook=${stats.requestStateTook}")
    val stateTick = currentWorldState?.turn
    val startLogic = System.currentTimeMillis()
    val myAction = doSimpleGuy()
    stats.logicTookMs = System.currentTimeMillis() - startLogic

    log("actualStrategy logicTookMs=${stats.logicTookMs} myAction=$myAction")

    lastAction = myAction

    startRequest = System.currentTimeMillis()
    currentWorldState = Api.move(myAction.commands, throttleEnabled = false)
    stats.requestTook = System.currentTimeMillis() - startRequest
    val actualTick = currentWorldState?.turn
    if (actualTick != stateTick) {
        log("ERROR tick mismatch stateTick=$stateTick (took=${stats.requestStateTook}) tick=$actualTick(took=${stats.requestTook})")
        stats.badMoves++
    } else {
        stats.successMoves++
    }

    val waitNextTick = currentWorldState?.tickRemainMs?.plus(100L) ?: 200L
    log("actualStrategy waitNextTick=${waitNextTick} tickRemainMs=${currentWorldState?.tickRemainMs}")
    logicThread.schedule({ globalLoop() }, waitNextTick, TimeUnit.MILLISECONDS)
}

var mapPoints = PlainArray3D(0, 0, 0, { Point3D(0, 0, 0) })
var mapBooleans = PlainArray3DBoolean(0, 0, 0, { false })
var mapInts1 = PlainArray3DInt(0, 0, 0, { 0 })
var mapInts2 = PlainArray3DInt(0, 0, 0, { 0 })

private fun doSimpleGuy(): ActionPlanned {
    val w = currentWorldState

    if (w == null) {
        return ActionPlanned(emptyList())
    }
    initMapsIfNeeded(w)

    val actionPlanned = ActionPlanned()

    val result = mutableListOf<SnakeCommandDto>()

    stats.tmpSims.clear()
    if (DRAW_UI) {
        stats.tmpLogicToDraw = LogicToDraw()
    }
    val claimedFood = mutableSetOf<FoodDto>()
    w.snakes.forEach { mySnake ->
        if (mySnake.status == "dead") {
            return@forEach
        }
        var accessMap = calcAccessMap(mapInts1, mySnake)
        var bestFood = findBestFood(accessMap, mySnake, claimedFood)

        if (bestFood == null) {
            log("WARNING no food found for snake ${mySnake.id} myHead=${mySnake.head} retry with maxDist=70")
            accessMap = calcAccessMap(mapInts1, mySnake, maxDist = 70)
            bestFood = findBestFood(accessMap, mySnake, claimedFood)
        }

        if (bestFood != null) {
            claimedFood += bestFood
            val pathToFood = findPath(mySnake, bestFood.cPoint, accessMap)
            val firstStepLocation = pathToFood.first()
            val firstStep = firstStepLocation.copy().minus(mySnake.head)
            result.add(SnakeCommandDto(id = mySnake.id, direction = firstStep.toList()))

            stats.tmpLogicToDraw?.paths?.add(pathToFood)
            stats.tmpLogicToDraw?.foods?.add(bestFood)
            stats.tmpLogicToDraw?.targetPoints?.add(
                TargetPoints(
                    from = mySnake.head,
                    to = bestFood.cPoint,
                )
            )
            return@forEach
        }
        log("WARNING no food found for snake ${mySnake.id} myHead=${mySnake.head}")

        val possibleDirs = mutableListOf<Point3D>()
        mySnake.head.forAllDirections(mapPoints) { neigbor ->
            var hasHeadInIt = false
            neigbor.forAllDirections(mapPoints) { neighborPoint ->
                hasHeadInIt = w.enemies.any() { it.geometryPoints.firstOrNull() == neighborPoint } ||
                        w.snakes.any() { it != mySnake && it.geometryPoints.firstOrNull() == neighborPoint }
                hasHeadInIt
            }
            if (hasHeadInIt.not()) {
                possibleDirs.add(neigbor.copy().minus(mySnake.head))
            }
            false
        }
        if (possibleDirs.isNotEmpty()) {
            val random = possibleDirs.random()
            log("WARNING no food found for snake ${mySnake.id} myHead=${mySnake.head} go to random safe=$random allPossible=${possibleDirs}")
            result.add(
                SnakeCommandDto(
                    id = mySnake.id,
                    direction = random.toList()
                )
            )
        } else {
            log("ERROR no food found for snake ${mySnake.id} myHead=${mySnake.head} GO TO RANDOM UNSAFE")
            result.add(
                SnakeCommandDto(
                    id = mySnake.id,
                    direction = listOf(
                        Point3D(1, 0, 0),
                        Point3D(0, 1, 0),
                        Point3D(0, 0, 1),
                        Point3D(1, 0, 0).mul(-1.0),
                        Point3D(0, 1, 0).mul(-1.0),
                        Point3D(0, 0, 1).mul(-1.0),
                    ).random().toList()
                )
            )
        }
    }

    stats.sims = ArrayList(stats.tmpSims)
    stats.logicToDraw = stats.tmpLogicToDraw
    actionPlanned.commands = result
    return actionPlanned

}

fun findPath(fromSnake: SnakeDto, to: Point3D, accessMap: PlainArray3DInt): List<Point3D> {

    /*findPathFood(to, accessMap, emptyList()).let { (path, points) ->
        log("found path points=$points path=$path")
        return path.reversed()
    }*/

    return findPath(fromSnake.head, to, accessMap)
}

fun findPath(fromSnake: Point3D, to: Point3D, accessMap: PlainArray3DInt): List<Point3D> {
    val w = currentWorldState ?: throw IllegalStateException("worldState is null")
    val path = mutableListOf<Point3D>()

    var currentPoint = to

    while (currentPoint != fromSnake) {
        path.add(currentPoint)

        val myValue = accessMap.getFast(currentPoint)
        // check all neighboars for lower value

        var newValue: Int = myValue

        currentPoint.forAllDirections(mapPoints) { p ->
            newValue = accessMap.getFast(p)
            if (newValue < myValue) {
                currentPoint = p
                true
            } else {
                false
            }
        }

        if (newValue >= myValue) {
            throw IllegalStateException("can't find path myValue=$myValue newValue=${newValue} path=${path.reverse()}")
        }
    }

    path.reverse()

    return path
}

// bad
fun findPathFood(to: Point3D, accessMap: PlainArray3DInt, totalPath:List<Point3D>): Pair<List<Point3D>, Int> {
    val w = currentWorldState ?: throw IllegalStateException("worldState is null")
    val path = mutableListOf<Point3D>()

    val currentPoint = to
    val myValue = accessMap.getFast(currentPoint)
    val food = (w.allEntities.getFast(currentPoint) as? FoodDto)?.points ?: 0

    if (myValue == 0) {
        return totalPath to food
    }


    var bestPath: Pair<List<Point3D>, Int>? = null

    currentPoint.forAllDirections(mapPoints) { p ->
        val newValue = accessMap.getFast(p)
        if (newValue < myValue) {
            var candidate = findPathFood(p, accessMap, totalPath)
            candidate = candidate.first + p to candidate.second + food
            if (bestPath == null || candidate.second > bestPath!!.second) {
                bestPath = candidate
            }
        }
        false
    }

    return bestPath!!
}

inline fun <E> List<E>.fastForEach(action: (E) -> Unit) {
    repeat(size) { index ->
        action(get(index))
    }
}

fun findBestFood(accessMap: PlainArray3DInt, mySnake: SnakeDto, claimedFood: MutableSet<FoodDto>): FoodDto? {
    val w = currentWorldState ?: throw IllegalStateException("worldState is null")

    var closestFood: FoodDto? = null
    var bestPointsPerDistance = 0f
    w.food.forEach { f ->
        if (claimedFood.contains(f)) {
            return@forEach
        }
        val distance = accessMap.getFast(f.cPoint)
        if (distance == Int.MAX_VALUE) {
            return@forEach
        }

        var passableCellsCount = 0

        f.cPoint.forAllDirections(mapPoints) { p ->
            val entity = w.allEntities.getFast(p)
            if (entity == null || entity is FoodDto) {
                passableCellsCount++
            }
            false
        }

        val pointsPerDistance = f.points / distance.toFloat()

        if (passableCellsCount >= 2 && pointsPerDistance > bestPointsPerDistance) {
            closestFood = f
            bestPointsPerDistance = pointsPerDistance
        }
    }
    return closestFood
}

fun calcAccessMap(accessMap: PlainArray3DInt, mySnake: SnakeDto, maxDist: Int = 40): PlainArray3DInt {
    // do dejkstra from mySnake head and fill accessMap
    val w = currentWorldState ?: throw IllegalStateException("worldState is null")
    val allEntities = w.allEntities

    accessMap.setAll(Int.MAX_VALUE)

    val visited = mapBooleans
    visited.setAll(false)
    accessMap.setFast(mySnake.head, 0)

    val queue = ArrayDeque<Point3D>()
    queue.add(mySnake.head)

    // implement loop, check there is no entity in path
    while (queue.isNotEmpty()) {
        val currentPoint = queue.removeFirst()

        if (visited.getFast(currentPoint)) {
            continue
        }
        val currentValue = accessMap.getFast(currentPoint)
        visited.setFast(currentPoint, true)

        if (currentValue >= maxDist) {
            continue
        }

        currentPoint.forAllDirections(mapPoints) { p ->
            if (visited.getFast(p).not()) {
                val entity = allEntities.getFast(p)
                if (entity == null || entity is FoodDto) {
                    var hasHeadInIt = false
                    if (currentValue == 0) {
                        p.forAllDirections(mapPoints) { neighborPoint ->
                            hasHeadInIt = w.enemies.any() { it.geometryPoints.firstOrNull() == neighborPoint } ||
                                    w.snakes.any() { it != mySnake && it.geometryPoints.firstOrNull() == neighborPoint }
                            hasHeadInIt
                        }
                    }

                    if (hasHeadInIt) {
                        visited.setFast(p, true)
                    } else {
                        accessMap.setFast(p, currentValue + 1)
                        queue.add(p)
                    }
                }
            }
            false
        }
    }
    return accessMap
}

private fun initMapsIfNeeded(w: WorldStateDto) {
    if (mapPoints.cellsWidth != w.mapSizePoint.x) {
        mapPoints = PlainArray3D(w.mapSizePoint.x, w.mapSizePoint.y, w.mapSizePoint.z, { Point3D(0, 0, 0) })
        mapPoints.fori { x, y, z, v ->
            v.x = x
            v.y = y
            v.z = z
        }

        mapBooleans = PlainArray3DBoolean(w.mapSizePoint.x, w.mapSizePoint.y, w.mapSizePoint.z, { false })
        mapInts1 = PlainArray3DInt(w.mapSizePoint.x, w.mapSizePoint.y, w.mapSizePoint.z, { Int.MAX_VALUE })
        mapInts2 = PlainArray3DInt(w.mapSizePoint.x, w.mapSizePoint.y, w.mapSizePoint.z, { Int.MAX_VALUE })
    }
}

data class AccAndScore(val acc: Point3D, val score: Double = 0.0)

class SimsInfo(val best: AccAndScore, val allVariants: List<AccAndScore>)

fun loadState() {
    File("state.json").takeIf { it.exists() }?.let {
        val parsedState = Api.gson.fromJson(it.readText(), State::class.java)
        state = parsedState
    } ?: run {
        state = State()
    }
}

fun drawUi() {
    if (!DRAW_UI) {
        return
    }
    ui = Ui()
    ui.setup()
    uiLoop()
}

fun uiLoop() {
    val timer = Timer(32) { e ->
        // Code to execute after delay
        if (currentWorldState == null) {
            return@Timer
        }
        ui.redraw()
    }
    timer.isRepeats = true
    timer.start()
}


var dateFormat = SimpleDateFormat("HH:mm:ss.SSS")

fun log(s: String?) {
    println(dateFormat.format(Date()) + " *${currentWorldState?.turn} " + s)
}
