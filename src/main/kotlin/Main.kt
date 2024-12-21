import java.io.File
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
    while (true) {
        runCatching { doLoop() }
            .onFailure {
                logicThread.schedule({ globalLoop() }, 3, TimeUnit.SECONDS)
                System.err.println("doLoop failed " + it.stackTraceToString())
                return
            }
    }
}

fun doLoop() {
    val roundsInfo = Api.getRounds()

    val rounds = roundsInfo.rounds.sortedBy { it.getStartAsLong() }
    val activeRound = rounds.firstOrNull { it.status == "active" }


    actualStrategy()
}

fun actualStrategy() {
    var startRequest = System.currentTimeMillis()
    currentWorldState = Api.move(transports = emptyList())
    stats.requestStateTook = System.currentTimeMillis() - startRequest
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
        log("ERROR actualStrategy tick mismatch tick=$actualTick stateTick=$stateTick")
        stats.badMoves++
    } else {
        stats.successMoves++
    }

    val waitNextTick = currentWorldState?.tickRemainMs?.plus(20L) ?: 200L
    log("actualStrategy waitNextTick=${waitNextTick}")
    logicThread.schedule({ actualStrategy() }, waitNextTick, TimeUnit.MILLISECONDS)
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
    w.snakes.forEach { mySnake ->
        if (mySnake.status == "dead") {
            return@forEach
        }
        val accessMap = calcAccessMap(mapInts1, mySnake)

        val closestFood = findClosestFood(accessMap, mySnake)

        if (closestFood != null) {
            val pathToFood = findPath(mySnake, closestFood.cPoint, accessMap)
            val firstStepLocation = pathToFood.first()
            val firstStep = firstStepLocation.copy().minus(mySnake.head)
            result.add(SnakeCommandDto(id = mySnake.id, direction = firstStep.toList()))

            stats.tmpLogicToDraw?.paths?.add(pathToFood)
            stats.tmpLogicToDraw?.targetPoints?.add(
                TargetPoints(
                    from = mySnake.head,
                    to = closestFood.cPoint,
                )
            )
            return@forEach
        }

        /*
                var attack: EnemyDto? = null
                if (mySnake.attackCooldownMs == 0) {
                    attack = w.enemies.filter { it.pos.distance(mySnake.pos) < w.attackRange }.minByOrNull { it.health }
                }
        */

        // val target = targetToGo(w, mySnake)
        //val target = bestAcc(w, mySnake)
        //  val acceleration = target.copy().minus(mySnake.pos).normalize().mul(w.maxAccel)
        /*    val acceleration = bestAcc(w, mySnake)


                    result.add(
                        SnakeCommandDto(
                            acceleration = acceleration,
                            activateShield = mySnake.shieldCooldownMs == 0 && w.enemies.any { it.pos.distance(mySnake.pos) < 200 },
                            attack = attack?.pos?.toVec2(),
                            id = mySnake.id
                        )
                    )*/
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

    stats.sims = ArrayList(stats.tmpSims)
    stats.logicToDraw = stats.tmpLogicToDraw
    actionPlanned.commands = result
    return actionPlanned

}

fun findPath(fromSnake: SnakeDto, to: Point3D, accessMap: PlainArray3DInt): List<Point3D> {
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

inline fun <E> List<E>.fastForEach(action: (E) -> Unit) {
    repeat(size) { index ->
        action(get(index))
    }
}

fun findClosestFood(accessMap: PlainArray3DInt, mySnake: SnakeDto): FoodDto? {
    val w = currentWorldState ?: throw IllegalStateException("worldState is null")

    var closestFood: FoodDto? = null
    var closestFoodDistance = Int.MAX_VALUE
    w.food.forEach { f ->
        val distance = accessMap.getFast(f.cPoint)

        if (distance < closestFoodDistance) {
            closestFood = f
            closestFoodDistance = distance
        }
    }
    return closestFood
}

fun calcAccessMap(accessMap: PlainArray3DInt, mySnake: SnakeDto, maxDist: Int = 60): PlainArray3DInt {
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
                    accessMap.setFast(p, currentValue + 1)
                    queue.add(p)
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

fun bestAcc(w: WorldStateDto, mySnake: SnakeDto): Point3D {
    /*
        val count = 16
        val accelerations = (0..count).map { index ->
            val acc = Point2DF(1.0, 0.0).rotate((Math.PI * 2) / count.toDouble() * index).mul(w.maxAccel)
            acc
        }

        val SIM_TIME = 16000
        val TICKS = ((SIM_TIME / 1000f) * TICKS_PER_SECOND).toInt()
        val worldCenter = w.mapSizePoint.copy().mul(0.5)
        val variants = accelerations.map { acc ->

            val variantPoses = mutableListOf<Point2DF>()
            var score = 0.0

            var pos = mySnake.geometryPoints.first().copy()
            var velocity = mySnake.velocity.copy()

            val bountiesPicked = mutableMapOf<Point2DF, BountyDto>()
            val enemiesHit = mutableMapOf<Point2DF, EnemyDto>()
            val anomalisHit = mutableMapOf<Point2DF, AnomalyDto>()

            var outOfBounds = false

            for (i in 0 until TICKS) {
                variantPoses.add(pos.copy())
                pos = pos.copy().plus(velocity.copy().mul(1.0 / TICKS_PER_SECOND))
                velocity = velocity.copy().plus(acc.copy().mul(1.0 / TICKS_PER_SECOND))
                if (velocity.length() > w.maxSpeed) {
                    velocity = velocity.normalize().mul(w.maxSpeed)
                }

                // put picked bounties, by radius

                w.bounties.forEach {
                    if (it.pos.sqDistance(pos) < it.radius * it.radius) {
                        bountiesPicked[it.pos] = it
                    }
                }

                w.enemies.forEach { enemy ->
                    if (enemy.pos.sqDistance(pos) < 10 * 10) {
                        enemiesHit[enemy.pos] = enemy
                    }
                }
                w.anomalies.forEach { anomaly ->
                    if (anomaly.pos.sqDistance(pos) < anomaly.radius * anomaly.radius) {
                        anomalisHit[anomaly.pos] = anomaly
                    }
                }

                // out ofBounds
                outOfBounds = outOfBounds || pos.x < 0 || pos.y < 0 || pos.x > w.mapSize.x || pos.y > w.mapSize.y
            }

            score = bountiesPicked.values.sumByDouble { it.points }

            enemiesHit.values.forEach { en ->
                if (en.health >= mySnake.health) {
                    score -= w.points * 0.05
                }
            }

            anomalisHit.values.forEach { an ->
                score -= w.points * 0.05
            }

            if (outOfBounds) {
                score -= w.points * 0.05
            }

            score -= variantPoses.last().distance(worldCenter) / w.mapSize.max().toDouble()

            AccAndScore(acc, score, variantPoses, bountiesPicked)
        }

        val best = variants.maxByOrNull { it.score }!!
        stats.tmpSims.add(
            SimsInfo(
                best = best,
                allVariants = variants,
            )
        )
        return best.acc
    */
    return null!!
}

class SimsInfo(val best: AccAndScore, val allVariants: List<AccAndScore>) {

}

/*
private fun targetToGo(w: WorldStateDto, myShip: TransportDto): Point2DF {
    val closestBounty = w.bounties.minByOrNull { it.pos.sqDistance(myShip.pos) }?.pos
    val target = closestBounty ?: w.mapSize.copy().mul(0.5)
    return target
}
*/

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


fun log(s: String?) {
    println(Date().toString() + " " + s)
}
