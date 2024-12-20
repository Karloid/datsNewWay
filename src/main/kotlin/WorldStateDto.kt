/**
 * {
 *   "anomalies": [
 *     {
 *       "effectiveRadius": 0,
 *       "id": "string",
 *       "radius": 0,
 *       "strength": 0,
 *       "velocity": {
 *         "x": 1.2,
 *         "y": 1.2
 *       },
 *       "x": 1,
 *       "y": 1
 *     }
 *   ],
 *   "attackCooldownMs": 1000,
 *   "attackDamage": 10,
 *   "attackExplosionRadius": 10,
 *   "attackRange": 10,
 *   "bounties": [
 *     {
 *       "points": 100,
 *       "radius": 10,
 *       "x": 1,
 *       "y": 1
 *     }
 *   ],
 *   "enemies": [
 *     {
 *       "health": 100,
 *       "killBounty": 10,
 *       "shieldLeftMs": 5000,
 *       "status": "alive",
 *       "velocity": {
 *         "x": 1.2,
 *         "y": 1.2
 *       },
 *       "x": 1,
 *       "y": 1
 *     }
 *   ],
 *   "mapSize": {
 *     "x": 1,
 *     "y": 1
 *   },
 *   "maxAccel": 1,
 *   "maxSpeed": 10,
 *   "name": "player1",
 *   "points": 100,
 *   "reviveTimeoutSec": 2,
 *   "shieldCooldownMs": 10000,
 *   "shieldTimeMs": 5000,
 *   "transportRadius": 5,
 *   "transports": [
 *     {
 *       "anomalyAcceleration": {
 *         "x": 1.2,
 *         "y": 1.2
 *       },
 *       "attackCooldownMs": 0,
 *       "deathCount": 0,
 *       "health": 100,
 *       "id": "00000000-0000-0000-0000-000000000000",
 *       "selfAcceleration": {
 *         "x": 1.2,
 *         "y": 1.2
 *       },
 *       "shieldCooldownMs": 0,
 *       "shieldLeftMs": 0,
 *       "status": "alive",
 *       "velocity": {
 *         "x": 1.2,
 *         "y": 1.2
 *       },
 *       "x": 1,
 *       "y": 1
 *     }
 *   ],
 *   "wantedList": [
 *     {
 *       "health": 100,
 *       "killBounty": 10,
 *       "shieldLeftMs": 5000,
 *       "status": "alive",
 *       "velocity": {
 *         "x": 1.2,
 *         "y": 1.2
 *       },
 *       "x": 1,
 *       "y": 1
 *     }
 *   ]
 * }
 */


data class WorldStateDto(
    val mapSize: List<Int>,
    val name: String,
    val points: Int,
    val fences: List<List<Int>>,
    val snakes: List<SnakeDto>,
    val enemies: List<EnemyDto>,
    val food: List<FoodDto>,
    val specialFood: SpecialFoodDto,
    val turn: Int,
    val reviveTimeoutSec: Int,
    val tickRemainMs: Int,
    val errors: List<Any>
) {
    private var _mapSizePoint: Point3D? = null
    val mapSizePoint: Point3D
        get() = _mapSizePoint ?: Point3D(mapSize[0], mapSize[1], mapSize[2]).also { _mapSizePoint = it }
    private var _fencesPoints: MutableList<Point3D>? = null
    val fencesPoints: List<Point3D>
        get() {
            if (_fencesPoints == null) {
                val new = ArrayList<Point3D>()
                fences.forEach { coords ->
                    new.add(Point3D(coords[0], coords[1], coords[2]))
                }
                _fencesPoints = new
            }
            return _fencesPoints!!
        }
}

data class SnakeDto(
    val id: String,
    val direction: List<Int>,
    val oldDirection: List<Int>,
    val geometry: List<List<Int>>,
    val deathCount: Int,
    val status: String,
    val reviveRemainMs: Int
) {
    private var _directionPoint: Point3D? = null
    val directionPoint: Point3D
        get() = _directionPoint ?: Point3D(direction[0], direction[1], direction[2]).also { _directionPoint = it }

    private var _oldDirectionPoint: Point3D? = null
    val oldDirectionPoint: Point3D
        get() = _oldDirectionPoint ?: Point3D(oldDirection[0], oldDirection[1], oldDirection[2]).also { _oldDirectionPoint = it }

    private var _geometryPoints: MutableList<Point3D>? = null
    val geometryPoints: List<Point3D>
        get() {
            if (_geometryPoints == null) {
                _geometryPoints = mutableListOf()
                geometry.forEach { coords ->
                    _geometryPoints?.add(Point3D(coords[0], coords[1], coords[2]))
                }
            }
            return _geometryPoints!!
        }
}

data class EnemyDto(
    val geometry: List<List<Int>>,
    val status: String,
    val kills: Int
) {
    private var _geometryPoints: MutableList<Point3D>? = null
    val geometryPoints: List<Point3D>
        get() {
            if (_geometryPoints == null) {
                _geometryPoints = mutableListOf()
                geometry.forEach { coords ->
                    _geometryPoints?.add(Point3D(coords[0], coords[1], coords[2]))
                }
            }
            return _geometryPoints!!
        }
}

data class FoodDto(
    val c: List<Int>,
    val points: Int
) {
    private var _cPoint: Point3D? = null
    val cPoint: Point3D
        get() = _cPoint ?: Point3D(c[0], c[1], c[2]).also { _cPoint = it }
}

data class SpecialFoodDto(
    val golden: List<List<Int>>,
    val suspicious: List<List<Int>>
) {

    private var _goldenPoints: MutableList<Point3D>? = null
    val goldenPoints: List<Point3D>
        get() {
            if (_goldenPoints == null) {
                _goldenPoints = mutableListOf()
                golden.forEach { coords ->
                    _goldenPoints?.add(Point3D(coords[0], coords[1], coords[2]))
                }
            }
            return _goldenPoints!!
        }

    private var _goldenPointsSet: MutableSet<Point3D>? = null
    val goldenPointsSet: Set<Point3D>
        get() {
            if (_goldenPointsSet == null) {
                _goldenPointsSet = mutableSetOf()
                golden.forEach { coords ->
                    _goldenPointsSet?.add(Point3D(coords[0], coords[1], coords[2]))
                }
            }
            return _goldenPointsSet!!
        }

    private var _suspiciousPointsSet: MutableSet<Point3D>? = null
    val suspiciousPointsSet: Set<Point3D>
        get() {
            if (_suspiciousPointsSet == null) {
                _suspiciousPointsSet = mutableSetOf()
                suspicious.forEach { coords ->
                    _suspiciousPointsSet?.add(Point3D(coords[0], coords[1], coords[2]))
                }
            }
            return _suspiciousPointsSet!!
        }

    private var _suspiciousPoints: MutableList<Point3D>? = null
    val suspiciousPoints: List<Point3D>
        get() {
            if (_suspiciousPoints == null) {
                _suspiciousPoints = mutableListOf()
                suspicious.forEach { coords ->
                    _suspiciousPoints?.add(Point3D(coords[0], coords[1], coords[2]))
                }
            }
            return _suspiciousPoints!!
        }
}

data class CommandsDto(
    val snakes: List<SnakeCommandDto>
)

data class SnakeCommandDto(
    val id: String,
    val direction: List<Int>,
) {
}