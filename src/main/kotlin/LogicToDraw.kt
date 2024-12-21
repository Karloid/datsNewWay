class LogicToDraw {
    val foods = mutableListOf<FoodDto>()
    val targetPoints = mutableListOf<TargetPoints>()
    val paths = mutableListOf<List<Point3D>>()
}

class TargetPoints(
    val from:Point3D,
    val to:Point3D
) {

}
