class PlainArray3DBoolean internal constructor(
    @JvmField val cellsWidth: Int,
    @JvmField val cellsHeight: Int,
    @JvmField val cellsDepth: Int,
    init: (Int) -> Boolean
) {

    // Copy constructor
    constructor(sourceArray: PlainArray3DBoolean) :
            this(
                sourceArray.cellsWidth,
                sourceArray.cellsHeight,
                sourceArray.cellsDepth,
                { sourceArray.array[it] }
            )

    @JvmField
    val array: BooleanArray

    init {
        array = BooleanArray(cellsWidth * cellsHeight * cellsDepth, init)
    }

    inline operator fun get(x: Int, y: Int, z: Int): Boolean? {
        return if (!inBounds(x, y, z)) null else array[(z * cellsHeight + y) * cellsWidth + x]
    }

    inline fun getFast(x: Int, y: Int, z: Int): Boolean {
        return array[(z * cellsHeight + y) * cellsWidth + x]
    }

    fun add(x: Int, y: Int, z: Int, value: Boolean) {
        if (!inBounds(x, y, z)) return
        array[(z * cellsHeight + y) * cellsWidth + x] = value
    }

    operator fun set(x: Int, y: Int, z: Int, value: Boolean) {
        if (!inBounds(x, y, z)) return
        array[(z * cellsHeight + y) * cellsWidth + x] = value
    }

    fun setFast(x: Int, y: Int, z: Int, value: Boolean) {
        array[(z * cellsHeight + y) * cellsWidth + x] = value
    }

    inline fun inBounds(x: Int, y: Int, z: Int): Boolean {
        return x in 0 until cellsWidth && y in 0 until cellsHeight && z in 0 until cellsDepth
    }

    inline fun fori(block: (x: Int, y: Int, z: Int, v: Boolean) -> Unit) {
        for (z in 0 until cellsDepth) {
            for (y in 0 until cellsHeight) {
                for (x in 0 until cellsWidth) {
                    block(x, y, z, getFast(x, y, z))
                }
            }
        }
    }

    fun get(pos: Point3D): Boolean? {
        return get(pos.x.toInt(), pos.y.toInt(), pos.z.toInt())
    }

    fun getFast(pos: Point3D): Boolean {
        return getFast(pos.x, pos.y, pos.z)
    }

    fun setFast(pos: Point3D, value: Boolean) {
        setFast(pos.x, pos.y, pos.z, value)
    }

    fun copyFrom(other: PlainArray3DBoolean) {
        System.arraycopy(other.array, 0, array, 0, array.size)
    }

    fun setAll(newVal: Boolean) {
        array.fill(newVal)
    }

    fun getPositionFromIndex(index: Int): Point3D {
        val x = index % cellsWidth
        val y = (index / cellsWidth) % cellsHeight
        val z = index / (cellsWidth * cellsHeight)
        return Point3D(x, y, z)
    }
}