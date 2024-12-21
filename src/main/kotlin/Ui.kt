import java.awt.Color
import java.awt.Graphics
import javax.swing.*
import kotlin.math.roundToInt


val CANVAS_SIZE = 1024


class Ui {
    private var activeZ: Int = 0

    val frame = JFrame("DatsNewWay")
    val infoLabel = JTextArea("Info")

    val canvasPanel: JPanel = object : JPanel() {
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            g.color = Color.WHITE
            g.fillRect(0, 0, this.width, this.height) // Draw white background

            val w = currentWorldState
            val a = lastAction

            if (w == null) {
                g.color = Color.RED
                g.fillOval(50, 50, (200 * Math.random()).toInt(), 200) // Draw red circle
                return
            }

            val k = 1024.0 / w.mapSize.max()

            val size = (1 * k).coerceAtLeast(1.0).roundToInt()

            w.fencesPoints.forEach {
                val baseColor = Color.BLACK
                g.color = baseColor
                if (it.z != activeZ && false) {
                    return@forEach
                }
                g.fillRect((it.x * k).toInt(), (it.y * k).toInt(), size, size)
            }

            w.enemies.forEach { en ->
                g.color = Color.RED
                en.geometryPoints.forEach { p ->
                    g.fillRect((p.x * k).toInt(), (p.y * k).toInt(), size, size)
                }
            }

            val maxFood = w.food.maxOf { it.points }
            val minFood = w.food.minOf { if (it.points == 0) Int.MAX_VALUE else it.points }
            val foodDelta = maxFood - minFood
            w.food.forEach { f ->
                val isGolden = w.specialFood.goldenPointsSet.contains(f.cPoint)
                val isSus = w.specialFood.goldenPointsSet.contains(f.cPoint)
                val strength = ((f.points - minFood - 0.0) / foodDelta).coerceAtLeast(0.1)
                g.color = when {
                    isGolden -> Color.PINK.withAlpha(strength)
                    isSus -> Color.CYAN
                    else -> Color.ORANGE.withAlpha(strength)
                }

                g.fillRect((f.cPoint.x * k).toInt(), (f.cPoint.y * k).toInt(), size, size)
            }

            w.snakes.forEach { myShip ->
                myShip.geometryPoints.forEachIndexed { index, p ->
                    g.color = if (index == 0) Color.GREEN else Color.BLUE
                    g.fillRect((p.x * k).toInt(), (p.y * k).toInt(), size, size)
                }
            }

            stats.logicToDraw?.let { logicToDraw ->

                logicToDraw.targetPoints.fastForEach { targetPoints ->
                    g.color = Color.BLUE

                    val from = targetPoints.from
                    val to = targetPoints.to

                    g.drawOval((from.x * k).toInt() - 3, (from.y * k).toInt() - 3, 6, 6)
                    g.drawOval((to.x * k).toInt() - 3, (to.y * k).toInt() - 3, 6, 6)

                    g.drawLine(
                        (from.x * k).toInt(),
                        (from.y * k).toInt(),
                        (to.x * k).toInt(),
                        (to.y * k).toInt(),
                    )
                }

                logicToDraw.paths.fastForEach { path ->
                    g.color = Color.BLUE.withAlpha(0.5)
                    path.fastForEach { p ->
                        g.fillRect((p.x * k).toInt(), (p.y * k).toInt(), size, size)
                    }
                }
            }


            val sims = stats.sims

            sims?.forEach { sims ->

                sims.allVariants.forEach { variant ->
                    // draw acc
                    g.color = Color.MAGENTA
                }
            }
        }
    }

    fun setup() {
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setSize(CANVAS_SIZE + 200, CANVAS_SIZE)
        frame.layout = null

        canvasPanel.setBounds(0, 0, CANVAS_SIZE, CANVAS_SIZE)

        val button1 = JButton("Button 1")
        val button2 = JButton("Button 2")
        val slider = JSlider(JSlider.HORIZONTAL, 0, 200, 0)
        slider.addChangeListener {
            activeZ = slider.value
            redraw()
        }

        button1.setBounds(CANVAS_SIZE, 20, 100, 30) // Align buttons to the right
        button2.setBounds(CANVAS_SIZE, 100, 100, 30)
        slider.setBounds(CANVAS_SIZE, 55, 200, 30)

        frame.add(canvasPanel)
        frame.add(button1)
        frame.add(button2)
        frame.add(slider)

        // add label under button 2 with with 200 and 200 height

        infoLabel.setBounds(CANVAS_SIZE + 3, button2.y + button2.height, 200, 1024 - (button2.y + button2.height))
        infoLabel.setEditable(false); // Make it non-editable
        infoLabel.setOpaque(false);   // Make background transparent
        infoLabel.setFocusable(false); // Do not focus
        infoLabel.setWrapStyleWord(true); // Wrap words
        infoLabel.setLineWrap(true); // Enable line wrap
        infoLabel.setBorder(BorderFactory.createEmptyBorder()); // Remove border
        frame.add(infoLabel)


        frame.isVisible = true

        redraw()
    }

    fun redraw() {
        canvasPanel.repaint()

        val minFood = currentWorldState?.food?.minOf { if (it.points == 0) Int.MAX_VALUE else it.points }

        infoLabel.text = "tick:${currentWorldState?.turn} points=${currentWorldState?.points} logicMs:${stats.logicTookMs} z:${activeZ} map:${currentWorldState?.mapSize}\n" +
                "requestTook: state=${stats?.requestStateTook} move=${stats.requestTook} success/bad moves=${stats.successMoves}/${stats.badMoves}\n" +
                "fences:${currentWorldState?.fences?.size}\n" +
                "foodCount:${currentWorldState?.food?.size} max:${currentWorldState?.food?.maxOf { it.points }} min:${minFood}\n" +
                "paths:${
                    stats.logicToDraw?.paths?.mapIndexed { index, it ->
                        val pathSize = it.size
                        val points = stats.logicToDraw?.foods?.getOrNull(index)?.points
                        val pPerDist = points?.let { it / pathSize.toFloat() }
                        "\nl:" + pathSize + " " + it.last().toList() + " p:" + points + " perDist:${pPerDist}"
                    }
                }\n"
    }
}

private fun Color.withAlpha(alpha: Double): Color {
    return Color(this.red, this.green, this.blue, (alpha * 255).toInt())
}
