import java.io.File
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.swing.Timer


var lastAction: ActionPlanned? = null

class ActionPlanned(var commands: List<SnakeCommandDto> = emptyList()) {

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

    val tmpSims: MutableList<SimsInfo> = mutableListOf()

    @Volatile
    var sims: List<SimsInfo>? = null
    var requestTook: Long = 0

    @Volatile
    var logicTookMs: Long = 0

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
    val startLogic = System.currentTimeMillis()
    val myAction = doSimpleGuy()
    stats.logicTookMs = System.currentTimeMillis() - startLogic

    lastAction = myAction

    val startRequest = System.currentTimeMillis()
    currentWorldState = Api.move(myAction.commands)
    stats.requestTook = System.currentTimeMillis() - startRequest

    stats.successMoves++

    val waitNextTick = currentWorldState?.tickRemainMs?.plus(20L) ?: 200L
    log("actualStrategy waitNextTick=${waitNextTick}")
    logicThread.schedule({ actualStrategy() }, waitNextTick, TimeUnit.MILLISECONDS)
}

private fun doSimpleGuy(): ActionPlanned {
    val w = currentWorldState

    if (w == null) {
        return ActionPlanned(emptyList<SnakeCommandDto>())
    }

    val actionPlanned = ActionPlanned()

    val result = mutableListOf<SnakeCommandDto>()

    stats.tmpSims.clear()
    w.snakes.forEach { myShip ->

/*
        var attack: EnemyDto? = null
        if (myShip.attackCooldownMs == 0) {
            attack = w.enemies.filter { it.pos.distance(myShip.pos) < w.attackRange }.minByOrNull { it.health }
        }
*/

        // val target = targetToGo(w, myShip)
        //val target = bestAcc(w, myShip)
        //  val acceleration = target.copy().minus(myShip.pos).normalize().mul(w.maxAccel)
        /*    val acceleration = bestAcc(w, myShip)


                    result.add(
                        SnakeCommandDto(
                            acceleration = acceleration,
                            activateShield = myShip.shieldCooldownMs == 0 && w.enemies.any { it.pos.distance(myShip.pos) < 200 },
                            attack = attack?.pos?.toVec2(),
                            id = myShip.id
                        )
                    )*/

        result.add(
            SnakeCommandDto(
                id = myShip.id,
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
    actionPlanned.commands = result
    return actionPlanned

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
