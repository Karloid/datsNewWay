import java.lang.Math.*

class Point2DF {
    var x: Double
    var y: Double

    inline val intX: Int
        get() = x.toInt()

    inline val intY: Int
        get() = y.toInt()

    val roundX: Int
        get() = round(x).toInt()

    val roundY: Int
        get() = round(y).toInt()

    val fx: Float
        get() = x.toFloat()

    val fy: Float
        get() = y.toFloat()

    constructor(x: Int, y: Int, `val`: Double) {
        this.x = x.toDouble()
        this.y = y.toDouble()
    }

    /*    override fun getDimen(dimenIndex: Int): Double {
            return if (dimenIndex % 2 == 0) {
                x
            } else {
                y
            }
        }

        override fun kdDistance(other: KDTree.KDValue): Double {
            if (other !is Point2DF) {
                return 0.0
            }
            return distance(other)
        }*/

    override fun toString(): String {
        return "(" + x.f() +
                ", " + y.f() +")"
    }



    constructor(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    internal constructor(x: Float, y: Float) {
        this.x = x.toDouble()
        this.y = y.toDouble()
    }

    fun getDistanceTo(x: Double, y: Double): Double {
        return getDistance(this.x, this.y, x, y)
    }

    /*   public static double getDistance(double x1, double y1, double x2, double y2) {
           double dx = x1 - x2;
           double dy = y1 - y2;
           return util.FastMath.hypot(dx, dy);
       }
   */
    fun getDistanceTo(point: Point2DF): Double {
        return getDistanceTo(point.x, point.y)
    }


    inline fun plus(x: Double, y: Double): Point2DF {
        this.x += x
        this.y += y
        return this
    }

    constructor() {
        x = 0.0
        y = 0.0
    }

    constructor(v: Point2DF) {
        this.x = v.x
        this.y = v.y
    }

    constructor(angle: Double) {
        this.x = cos(angle)
        this.y = sin(angle)
    }

    constructor(x: Int, y: Int) {
        this.x = x.toDouble()
        this.y = y.toDouble()
    }

    constructor(pos: Point2D) : this(pos.x, pos.y)

    fun copy(): Point2DF {
        return Point2DF(this)
    }

    operator fun minus(v: Point2DF): Point2DF {
        return Point2DF(x - v.x, y - v.y)
    }

    inline fun minus(dx: Double, dy: Double): Point2DF {
        x -= dx
        y -= dy
        return this
    }

    inline fun mul(f: Double): Point2DF {
        x *= f
        y *= f
        return this
    }

    inline fun length(): Double {
        //        return hypot(x, y);
        return hypot(x, y)
    }

    inline fun distance(v: Point2DF): Double {

        //        return hypot(x - v.x, y - v.y);
        return hypot(x - v.x, y - v.y)
    }

    inline fun sqDistance(v: Point2DF): Double {
        val tx = x - v.x
        val ty = y - v.y
        return tx * tx + ty * ty
    }

    inline fun sqDistance(x: Double, y: Double): Double {
        val tx = this.x - x
        val ty = this.y - y
        return tx * tx + ty * ty
    }

    inline fun squareLength(): Double {
        return x * x + y * y
    }

    inline fun reverse(): Point2DF {
        return Point2DF(-x, -y)
    }

    inline fun normalize(): Point2DF {
        val length = this.length()
        return if (length == 0.0) {
            Point2DF(0.0, 0.0)
        } else {
            Point2DF(x / length, y / length)
        }
    }

    inline fun length(length: Double): Point2DF {
        val currentLength = this.length()
        return if (currentLength == 0.0) {
            this
        } else {
            this.mul(length / currentLength)
        }
    }


    inline fun down(): Point2DF {
        return add(DOWN)
    }

    inline fun up(): Point2DF {
        return add(UP)
    }

    inline fun left(): Point2DF {
        return add(LEFT)
    }

    inline fun right(): Point2DF {
        return add(RIGHT)
    }

    inline fun down(mul: Double): Point2DF {
        return add(DOWN, mul)
    }

    inline fun up(mul: Double): Point2DF {
        return add(UP, mul)
    }

    inline fun left(mul: Double): Point2DF {
        return add(LEFT, mul)
    }

    inline fun right(mul: Double): Point2DF {
        return add(RIGHT, mul)
    }


    inline fun leftPerpendicular(): Point2DF {
        return Point2DF(y, -x)
    }

    inline fun rightPerpendicular(): Point2DF {
        return Point2DF(-y, x)
    }

    inline fun dotProduct(vector: Point2DF): Double {
        return x * vector.x + y * vector.y
    }

    inline fun angle(): Float {
        //return Math.atan2(y, x);
        return atan2(y, x).toFloat()
    }

    inline fun nearlyEqual(potentialIntersectionPoint: Point2DF, epsilon: Double): Boolean {
        return abs(x - potentialIntersectionPoint.x) < epsilon && abs(y - potentialIntersectionPoint.y) < epsilon
    }

    inline fun rotate(angle: Point2DF): Point2DF {
        val newX = angle.x * x - angle.y * y
        val newY = angle.y * x + angle.x * y
        return Point2DF(newX, newY)
    }

    inline fun rotateBack(angle: Point2DF): Point2DF {
        val newX = angle.x * x + angle.y * y
        val newY = angle.x * y - angle.y * x
        return Point2DF(newX, newY)
    }

    inline operator fun div(f: Double): Point2DF {
        return Point2DF(x / f, y / f)
    }


    inline operator fun plus(point: Point2DF): Point2DF {
        return plus(point.x, point.y)
    }

    inline fun rotate(angle: Double): Point2DF {

        val x1 = (this.x * cos(angle) - this.y * sin(angle)).toFloat()

        val y1 = (this.x * sin(angle) + this.y * cos(angle)).toFloat()

        return Point2DF(x1.toDouble(), y1.toDouble())
    }

    /*
        inline fun dirTo(pos: Point2DF): Direction {
            return when {
                pos.x - 1 == x -> Direction.RIGHT
                pos.x + 1 == x -> Direction.LEFT
                pos.y + 1 == y -> Direction.DOWN
                pos.y - 1 == y -> Direction.UP
                else -> {
                    debugLog { "unable to find dir for $this $pos" }
                    Direction.UP
                }
            }
        }

        inline fun applyDir(direction: Direction): Point2DF {
            return this.plus(getPointByDir(direction))
        }*/

    inline fun abs(): Point2DF {
        if (x < 0) {
            x *= -1
        }
        if (y < 0) {
            y *= -1
        }

        return this
    }

    inline fun max(): Double {
        return max(x, y)
    }

    fun noRoundCopy(): Point2DF {
        return Point2DF(intX, intY)
    }

    operator fun component1(): Double {
        return x
    }

    operator fun component2(): Double {
        return y
    }

    fun set(point2D: Point2DF): Point2DF {
        x = point2D.x
        y = point2D.y
        return this
    }

    fun set(x: Double, y: Double): Point2DF {
        this.x = x
        this.y = y
        return this
    }


    fun set(x: Int, y: Int): Point2DF {
        this.x = x.toDouble()
        this.y = y.toDouble()
        return this
    }


    fun toVec2(): Point2D {
        return Point2D(roundX, roundY)
    }

    companion object {

        /*
                inline fun getPointByDir(direction: Direction): Point2DF {
                    return when (direction) {
                        Direction.LEFT -> LEFT
                        Direction.UP -> UP
                        Direction.RIGHT -> RIGHT
                        Direction.DOWN -> DOWN
                    }
                }
        */


/*
        fun angle(x: Double, y: Double): Float {
            return atan2(y.toFloat().toDouble(), x.toFloat().toDouble())
        }
*/


        fun getDistance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
            val dx = x1 - x2
            val dy = y1 - y2
            return sqrt(dx * dx + dy * dy)
        }

        fun vector(fromX: Double, fromY: Double, toX: Double, toY: Double): Point2DF {
            return Point2DF(toX - fromX, toY - fromY)
        }


        val STUB: Point2DF = Point2DF(0, 0)
        val ZERO = Point2DF(0, 0)
        val UP = Point2DF(0, 1)
        val RIGHT = Point2DF(1, 0)
        val DOWN = Point2DF(0, -1)
        val LEFT = Point2DF(-1, 0)

    }

    @Throws(java.io.IOException::class)
            /*    fun writeTo(stream: java.io.OutputStream) {
                    StreamUtil.writeFloat(stream, x.toFloat())
                    StreamUtil.writeFloat(stream, y.toFloat())
                }*/

    fun add(d: Double): Point2DF {
        x += d
        y += d
        return this
    }

    fun add(d: Point2DF): Point2DF {
        x += d.x
        y += d.y
        return this
    }

    fun add(d: Point2DF, mul: Double): Point2DF {
        x += d.x * mul
        y += d.y * mul
        return this
    }

    fun set(point2D: Point2D): Point2DF {
        x = point2D.x.toDouble()
        y = point2D.y.toDouble()
        return this
    }

    fun toVec2NoRound():Point2D {
        return Point2D(x.toInt(), y.toInt())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Point2DF

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }

}

private fun Double.f(): String {
    // format 2 decimals
    return String.format("%.2f", this)
}
