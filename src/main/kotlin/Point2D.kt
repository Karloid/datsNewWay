import kotlin.math.roundToInt

data class Point3D(
    var x: Int,
    var y: Int,
    var z: Int
) {

    fun copy(): Point3D {
        return Point3D(x, y, z)
    }

    fun mul(d: Double): Point3D {
        x = (d * x).roundToInt()
        y = (d * y).roundToInt()
        z = (d * z).roundToInt()
        return this
    }

    fun toList(): List<Int> {
        return listOf(x, y, z)
    }

}

class Point2D {
    var x: Int = 0
    var y: Int = 0

    val isNull: Boolean
        get() = this.x or this.y == 0

    @JvmOverloads
    constructor(x: Int = 0, y: Int = x) {
        this.x = x
        this.y = y
    }

    constructor(vect: Point2D) {
        this.x = vect.x
        this.y = vect.y
    }

    constructor(from: Point2D, to: Point2D) : this(to.x - from.x, to.y - from.y)

    override fun equals(other: Any?): Boolean {
        if (other == null || !(other is Point2D)) {
            return false
        }
        return this.x == other.x && this.y == other.y
    }

    operator fun set(x: Int, y: Int): Point2D {
        this.x = x
        this.y = y
        return this
    }

    fun set(a: Point2D): Point2D {
        this.x = a.x
        this.y = a.y
        return this
    }

    fun add(a: Point2D): Point2D {
        this.x += a.x
        this.y += a.y
        return this
    }

    fun sub(a: Point2D): Point2D {
        this.x -= a.x
        this.y -= a.y
        return this
    }

    fun mult(a: Int): Point2D {
        this.x *= a
        this.y *= a
        return this
    }

    operator fun div(a: Int): Point2D {
        this.x /= a
        this.y /= a
        return this
    }

    fun negate(): Point2D {
        this.x = -this.x
        this.y = -this.y
        return this
    }

    /*
        fun normalize(): Point2D {
            if (isNull) return this

            val absx = Math.abs(this.x)
            val absy = Math.abs(this.y)
            if (absx > absy) {
                this.x /= absx
                this.y = 0
            } else if (absx < absy) {
                this.x = 0
                this.y /= absy
            } else {
                this.x /= absx
                this.y /= absy
            }
            return this
        }
    */


    fun manhattanDistance(): Int {
        return Math.abs(x) + Math.abs(y)
    }

    fun manhattanDistance(a: Point2D): Int {
        return Math.abs(this.x - a.x) + Math.abs(this.y - a.y)
    }

    fun tchebychevDistance(): Int {
        return Math.max(x, y)
    }

    fun tchebychevDistance(a: Point2D): Int {
        return Math.max(Math.abs(this.x - a.x), Math.abs(this.y - a.y))
    }

    fun euclidianDistance2(): Double {
        return (x * x + y * y).toDouble()
    }

    fun euclidianDistance2(a: Point2D): Double {
        return Math.pow((this.x - a.x).toDouble(), 2.0) + Math.pow((this.y - a.y).toDouble(), 2.0)
    }

    fun euclidianDistance(): Double {
        return Math.sqrt(euclidianDistance())
    }

    fun euclidianDistance(a: Point2D): Double {
        return Math.sqrt(euclidianDistance2(a))
    }

    override fun toString(): String {
        return "[$x:$y]"
    }

    inline operator fun plus(point: Point2D): Point2D {
        return plus(point.x, point.y)
    }

    inline fun plus(x: Int, y: Int): Point2D {
        this.x += x
        this.y += y
        return this
    }


    fun copy(): Point2D {
        return Point2D(x, y)
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        return result
    }

    fun abs() {
        x = kotlin.math.abs(x)
        y = kotlin.math.abs(y)
    }

    fun inRange(pos: Point2D, distance: Int): Boolean {
        return inRange(pos, distance.toDouble())
    }

    fun inRange(pos: Point2D, distance: Double): Boolean {
        return euclidianDistance2(pos) <= distance * distance
    }

    fun isZero(): Boolean {
        return x == 0 && y == 0
    }

    fun length(): Double {
        return Math.sqrt((x * x + y * y).toDouble())
    }

    fun hsymmetric(): Point2D {
        x = -x
        return this
    }

    fun vsymmetric(): Point2D {
        y = -y
        return this
    }

    /*
        fun toFloat(): Point2DF {
            return Point2DF(x, y)
        }
    */

    fun angle(targetPos: Point2D): Double {
        return Math.atan2((targetPos.y - y).toDouble(), (targetPos.x - x).toDouble())
    }

    fun mult(a: Double) {
        x = (x * a).toInt()
        y = (y * a).toInt()
    }

    fun toFloat(): Point2DF {
        return Point2DF(x.toDouble(), y.toDouble())

    }

    fun neighbors(): List<Point2D> {
        return listOf(
            Point2D(x + 1, y),
            Point2D(x - 1, y),
            Point2D(x, y + 1),
            Point2D(x, y - 1)
        )
    }

    fun neighborTo(pos: Point2D): Boolean {
        val deltaX = Math.abs(x - pos.x)
        // diagonal is not neighbor
        val deltaY = Math.abs(y - pos.y)
        return (deltaX == 1 && deltaY == 0) || (deltaX == 0 && deltaY == 1) || (deltaX == 0 && deltaY == 0)
    }

    fun diagonalNeighborTo(pos: Point2D): Boolean {
        val deltaX = Math.abs(x - pos.x)
        // diagonal is not neighbor
        val deltaY = Math.abs(y - pos.y)
        return neighborTo(pos) || (deltaX == 1 && deltaY == 1)
    }

    /*
        fun rotate(d: Double): Point2D {
            val newX = x * Math.cos(d) - y * Math.sin(d)
            val newY = x * Math.sin(d) + y * Math.cos(d)
            x = newX.toInt()
            y = newY.toInt()
            return this
        }
    */


    companion object {

        fun add(a: Point2D, b: Point2D): Point2D {
            return Point2D(a).add(b)
        }

        fun sub(a: Point2D, b: Point2D): Point2D {
            return Point2D(a).sub(b)
        }

        fun mult(a: Point2D, b: Int): Point2D {
            return Point2D(a).mult(b)
        }

        fun div(a: Point2D, b: Int): Point2D {
            return Point2D(a).div(b)
        }

        val ZERO: Point2D = Point2D(0, 0)

        val UP = Point2D(0, -1)
        val RIGHT = Point2D(1, 0)
        val DOWN = Point2D(0, 1)
        val LEFT = Point2D(-1, 0)

        /*
                inline fun getPointByDir(direction: Direction): Point2D {
                    return when (direction) {
                        Direction.LEFT -> LEFT
                        Direction.UP -> UP
                        Direction.RIGHT -> RIGHT
                        Direction.DOWN -> DOWN
                    }
                }
        */

        fun allDirections(): List<Point2D> {
            return listOf(
                UP, RIGHT, DOWN, LEFT
            )
        }

    }
}
