class PlainArray<T> internal constructor(@JvmField val cellsWidth: Int, @JvmField val cellsHeight: Int, init: (Int) -> T) {

    // copy constructor
    constructor(sourceArray: PlainArray<T>) :
            this(
                sourceArray.cellsWidth,
                sourceArray.cellsHeight,
                { sourceArray.array[it] })

    @JvmField
    val array: Array<T>

    init {
        array = Array(cellsWidth * cellsHeight, init as (Int) -> Any) as Array<T>
    }

    inline internal operator fun get(x: Int, y: Int): T? {
        return if (!inBounds(x, y)) {
            null
        } else {
            array[y * cellsWidth + x]
        }
    }

    inline fun getFast(x: Int, y: Int): T {
        return array[y * cellsWidth + x]
    }

    internal fun add(x: Int, y: Int, `val`: T) {
        if (!inBounds(x, y)) {
            return
        }
        array[y * cellsWidth + x] = `val`
    }

    internal operator fun set(x: Int, y: Int, `val`: T) {
        if (!inBounds(x, y)) {
            return
        }
        array[y * cellsWidth + x] = `val`
    }

    internal fun setFast(x: Int, y: Int, `val`: T) {
        array[y * cellsWidth + x] = `val`
    }

    inline fun inBounds(x: Int, y: Int): Boolean {
        return !(x < 0 || x >= cellsWidth || y < 0 || y >= cellsHeight)
    }

    inline fun fori(block: (x: Int, y: Int, v: T) -> Unit) {
        for (y in 0 until cellsHeight) {
            for (x in 0 until cellsWidth) {
                block(x, y, getFast(x, y))
            }
        }
    }

    fun get(pos: Point2D): T? {
        return get(pos.x.toInt(), pos.y.toInt())
    }

    fun getFast(pos: Point2D): T {
        return getFast(pos.x, pos.y)
    }

    fun setFast(pos: Point2D, value: T) {
        setFast(pos.x, pos.y, value)
    }

    fun copyFrom(other: PlainArray<T>) {
        System.arraycopy(other.array, 0, array, 0, array.size)
    }

    fun setAll(newVal: T) {
        repeat(array.size) {
            array[it] = newVal
        }
    }

    fun getPositionFromIndex(index: Int): Point2D {
        return Point2D(index % cellsWidth, index / cellsHeight)
    }
}
