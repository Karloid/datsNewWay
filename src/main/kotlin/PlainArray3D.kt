class PlainArray3D<T> internal constructor(
    @JvmField val cellsWidth: Int,
    @JvmField val cellsHeight: Int,
    @JvmField val cellsDepth: Int,
    init: (Int) -> T
) {

    // Copy constructor
    constructor(sourceArray: PlainArray3D<T>) :
            this(
                sourceArray.cellsWidth,
                sourceArray.cellsHeight,
                sourceArray.cellsDepth,
                { sourceArray.array[it] }
            )

    @JvmField
    val array: Array<T>

    init {
        array = Array(cellsWidth * cellsHeight * cellsDepth, init as (Int) -> Any) as Array<T>
    }

    inline internal operator fun get(x: Int, y: Int, z: Int): T? {
        return if (!inBounds(x, y, z)) null else array[(z * cellsHeight + y) * cellsWidth + x]
    }

    inline fun getFast(x: Int, y: Int, z: Int): T {
        return array[(z * cellsHeight + y) * cellsWidth + x]
    }

    internal fun add(x: Int, y: Int, z: Int, `val`: T) {
        if (!inBounds(x, y, z)) return
        array[(z * cellsHeight + y) * cellsWidth + x] = `val`
    }

    internal operator fun set(x: Int, y: Int, z: Int, `val`: T) {
        if (!inBounds(x, y, z)) return
        array[(z * cellsHeight + y) * cellsWidth + x] = `val`
    }

    internal fun setFast(x: Int, y: Int, z: Int, `val`: T) {
        array[(z * cellsHeight + y) * cellsWidth + x] = `val`
    }

    inline fun inBounds(x: Int, y: Int, z: Int): Boolean {
        return !(x < 0 || x >= cellsWidth || y < 0 || y >= cellsHeight || z < 0 || z >= cellsDepth)
    }

    inline fun fori(block: (x: Int, y: Int, z: Int, v: T) -> Unit) {
        for (z in 0 until cellsDepth) {
            for (y in 0 until cellsHeight) {
                for (x in 0 until cellsWidth) {
                    block(x, y, z, getFast(x, y, z))
                }
            }
        }
    }

    fun get(pos: Point3D): T? {
        return get(pos.x.toInt(), pos.y.toInt(), pos.z.toInt())
    }

    fun getFast(pos: Point3D): T {
        return getFast(pos.x, pos.y, pos.z)
    }

    fun setFast(pos: Point3D, value: T) {
        setFast(pos.x, pos.y, pos.z, value)
    }

    fun copyFrom(other: PlainArray3D<T>) {
        System.arraycopy(other.array, 0, array, 0, array.size)
    }

    fun setAll(newVal: T) {
        repeat(array.size) {
            array[it] = newVal
        }
    }

    fun getPositionFromIndex(index: Int): Point3D {
        val x = index % cellsWidth
        val y = (index / cellsWidth) % cellsHeight
        val z = index / (cellsWidth * cellsHeight)
        return Point3D(x, y, z)
    }
}