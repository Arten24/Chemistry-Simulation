package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.Atom3D
import com.example.model.Bond3D
import com.example.model.Vector3D
import kotlin.math.*

/**
 * 3D Engine Composed rendering interface using perspective projection and depth sorting.
 */
@Composable
fun Chemistry3DCanvas(
    atoms: List<Atom3D>,
    bonds: List<Bond3D>,
    orbitalCloudType: String = "None", // "None", "1s", "2pz", "3dz2", "sp3_lobe"
    gasParticles: List<Vector3D> = emptyList(),
    gasBoxHalfWidth: Float = 150f,
    electroMode: String = "None", // "None", "Galvanic", "Electrolytic"
    electroElectronFlowSec: Float = 0f,
    modifier: Modifier = Modifier
) {
    // Spatial camera rotations controlled by drag gestures
    var yaw by remember { mutableStateOf(-0.5f) }   // horizontal rotation
    var pitch by remember { mutableStateOf(0.4f) }  // vertical rotation
    var zoom by remember { mutableStateOf(45f) }    // camera scaling focal factor

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Drag factors to control responsiveness
                    yaw += dragAmount.x * 0.007f
                    pitch = (pitch - dragAmount.y * 0.007f).coerceIn(-1.5f, 1.5f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Render depth sorted items
            val drawablesList = mutableListOf<DrawableEntity>()

            // 1. Project Atoms
            val projectedAtoms = atoms.map { atom ->
                val p = project(atom.position, width, height, zoom, yaw, pitch)
                ProjectedAtom(atom, p)
            }

            projectedAtoms.forEach { pa ->
                drawablesList.add(DrawableEntity.AtomEntity(pa.screenPos.z, pa))
            }

            // 2. Project Bonds
            bonds.forEach { bond ->
                val fromPa = projectedAtoms.firstOrNull { it.atom.id == bond.fromAtomId }
                val toPa = projectedAtoms.firstOrNull { it.atom.id == bond.toAtomId }
                if (fromPa != null && toPa != null) {
                    val midZ = (fromPa.screenPos.z + toPa.screenPos.z) / 2f
                    drawablesList.add(DrawableEntity.BondEntity(midZ, fromPa, toPa, bond))
                }
            }

            // 3. Project Wavefunction Orbital Clouds if applicable
            if (orbitalCloudType != "None") {
                val cloudPoints = generateOrbitalPoints(orbitalCloudType)
                cloudPoints.forEach { pt ->
                    val p = project(pt.pos, width, height, zoom, yaw, pitch)
                    drawablesList.add(DrawableEntity.OrbitalPointEntity(p.z, p, pt.color))
                }
            }

            // 4. Project Gas Bouncing Particles if in gas chamber mode
            if (gasParticles.isNotEmpty()) {
                // Add the 3D bounding box lines
                val boxLines = getBoxLines(gasBoxHalfWidth)
                boxLines.forEach { line ->
                    val p1 = project(line.start, width, height, zoom, yaw, pitch)
                    val p2 = project(line.end, width, height, zoom, yaw, pitch)
                    val midZ = (p1.z + p2.z) / 2f
                    drawablesList.add(DrawableEntity.BoxLineEntity(midZ, p1, p2))
                }

                // Add particles sorted by depth
                gasParticles.forEach { part ->
                    val p = project(part, width, height, zoom, yaw, pitch)
                    drawablesList.add(DrawableEntity.GasParticleEntity(p.z, p))
                }
            }

            // 5. Project Electrochemistry Details if active
            if (electroMode != "None") {
                addElectrochemistryDrawables(
                    mode = electroMode,
                    flowProgress = electroElectronFlowSec,
                    width = width,
                    height = height,
                    zoom = zoom,
                    yaw = yaw,
                    pitch = pitch,
                    outList = drawablesList
                )
            }

            // Depth Sort using Painter's Algorithm: Draw items furthest away (smallest Z / furthest inside) first
            // Note: with project() Z offsets, the larger the projection Z, the closer it is. So sorting descending means closer elements are drawn last!
            val sortedDrawables = drawablesList.sortedBy { it.depth }

            // Render all items in sorted depth order
            sortedDrawables.forEach { entity ->
                when (entity) {
                    is DrawableEntity.AtomEntity -> renderAtom(entity.projectedAtom)
                    is DrawableEntity.BondEntity -> renderBond(entity.bond, entity.from, entity.to)
                    is DrawableEntity.OrbitalPointEntity -> renderOrbitalPoint(entity.pos, entity.color)
                    is DrawableEntity.GasParticleEntity -> renderGasParticle(entity.pos)
                    is DrawableEntity.BoxLineEntity -> renderBoxLine(entity.p1, entity.p2)
                    is DrawableEntity.ElectroMetalPlateEntity -> renderMetalPlate(entity.p1, entity.p2, entity.p3, entity.p4, entity.color)
                    is DrawableEntity.ElectroElectronEntity -> renderElectron(entity.pos)
                    is DrawableEntity.ElectroBeakerEntity -> renderBeaker(entity.center, entity.width, entity.height, entity.liquidColor)
                }
            }
        }
    }
}

/**
 * 3D Project equations.
 */
private fun project(vec: Vector3D, width: Float, height: Float, zoom: Float, yaw: Float, pitch: Float): Vector3D {
    // 1. Rotate Y (Yaw)
    var v = vec.rotateY(yaw)
    // 2. Rotate X (Pitch)
    v = v.rotateX(pitch)

    // Perspective parameters
    val cameraDistance = 350f
    val zOffset = (v.z + cameraDistance).coerceAtLeast(1f)
    val scale = (cameraDistance / zOffset) * zoom

    val screenX = width / 2f + v.x * scale
    val screenY = height / 2f - v.y * scale // Invert vertical direction
    return Vector3D(screenX, screenY, v.z) // Screen X, Y saved in x,y; Rotated depth saved in z
}

/**
 * Class representing dynamic drawable entities sorted for Painter's Algorithm.
 */
sealed class DrawableEntity(val depth: Float) {
    class AtomEntity(z: Float, val projectedAtom: ProjectedAtom) : DrawableEntity(z)
    class BondEntity(z: Float, val from: ProjectedAtom, val to: ProjectedAtom, val bond: Bond3D) : DrawableEntity(z)
    class OrbitalPointEntity(z: Float, val pos: Vector3D, val color: Color) : DrawableEntity(z)
    class GasParticleEntity(z: Float, val pos: Vector3D) : DrawableEntity(z)
    class BoxLineEntity(z: Float, val p1: Vector3D, val p2: Vector3D) : DrawableEntity(z)
    
    // Electrochemistry specific primitives
    class ElectroMetalPlateEntity(z: Float, val p1: Vector3D, val p2: Vector3D, val p3: Vector3D, val p4: Vector3D, val color: Color) : DrawableEntity(z)
    class ElectroElectronEntity(z: Float, val pos: Vector3D) : DrawableEntity(z)
    class ElectroBeakerEntity(z: Float, val center: Vector3D, val width: Float, val height: Float, val liquidColor: Color) : DrawableEntity(z)
}

data class ProjectedAtom(val atom: Atom3D, val screenPos: Vector3D)
data class OrbitalPoint(val pos: Vector3D, val color: Color)
data class BoxLine(val start: Vector3D, val end: Vector3D)

/**
 * Generate highly precise mathematical electron density clouds.
 */
private fun generateOrbitalPoints(orbitalType: String): List<OrbitalPoint> {
    val pointsList = mutableListOf<OrbitalPoint>()
    val samples = 350
    val random = java.util.Random(42)

    when (orbitalType) {
        "1s" -> {
            // Highly concentrated spherical distribution
            for (i in 0 until samples) {
                // Spherical sampling based on charge distribution probability exp(-r)
                val u = random.nextDouble()
                val r = -ln(1.0 - u) * 45f // Probability radius
                val theta = random.nextDouble() * PI
                val phi = random.nextDouble() * 2 * PI
                
                val x = (r * sin(theta) * cos(phi)).toFloat()
                val y = (r * sin(theta) * sin(phi)).toFloat()
                val z = (r * cos(theta)).toFloat()
                
                // Color representing orbital wave-function phase (S is fully positive phase: soft blue)
                pointsList.add(OrbitalPoint(Vector3D(x, y, z), Color(0x6640C4FF)))
            }
        }
        "2pz" -> {
            // Dumbbell lobe distribution representing pz spherical harmonics: Y_1^0 = cos(theta)
            for (i in 0 until samples) {
                val theta = random.nextDouble() * PI
                val cosTheta = cos(theta)
                val prob = cosTheta * cosTheta // Wave intensity probability
                if (random.nextDouble() < prob) {
                    val r = random.nextDouble() * 90f + 10f
                    val phi = random.nextDouble() * 2 * PI
                    
                    val x = (r * sin(theta) * cos(phi)).toFloat()
                    val y = (r * sin(theta) * sin(phi)).toFloat()
                    val z = (r * cos(theta)).toFloat()
                    
                    // Wave function color phase: cyan for positive top lobe, coral-red for negative bottom lobe
                    val col = if (z >= 0) Color(0x7F00E5FF) else Color(0x7FFF0D0D)
                    pointsList.add(OrbitalPoint(Vector3D(x, y, z), col))
                }
            }
        }
        "3dz2" -> {
            // Mathematical donut and dumbbell axial combination: 3(cos(theta)^2) - 1
            for (i in 0 until samples) {
                val theta = random.nextDouble() * PI
                val cosTheta = cos(theta)
                val psiValue = 3f * cosTheta.toFloat() * cosTheta.toFloat() - 1f
                val prob = (psiValue * psiValue).coerceAtMost(1f)
                
                if (random.nextDouble() < prob) {
                    val r = random.nextDouble() * 110f + 10f
                    val phi = random.nextDouble() * 2 * PI
                    
                    val x = (r * sin(theta) * cos(phi)).toFloat()
                    val y = (r * sin(theta) * sin(phi)).toFloat()
                    val z = (r * cos(theta)).toFloat()
                    
                    // Color positive lobes (z dumbbell) blue, toroid belt red
                    val col = if (psiValue > 0) Color(0x8F2979FF) else Color(0x8FFFF303)
                    pointsList.add(OrbitalPoint(Vector3D(x, y, z), col))
                }
            }
        }
        "sp3_lobe" -> {
            // Highly directional dynamic sp3 lobe: s + px + py + pz combination pointing at tetrahedral direction
            // Directional unit vector: (1, 1, 1).normalized()
            val dirX = 1f / sqrt(3f)
            val dirY = 1f / sqrt(3f)
            val dirZ = 1f / sqrt(3f)

            for (i in 0 until samples) {
                val theta = random.nextDouble() * PI
                val phi = random.nextDouble() * 2 * PI
                
                val vx = sin(theta) * cos(phi)
                val vy = sin(theta) * sin(phi)
                val vz = cos(theta)
                
                // Alignment dot product
                val dot = (vx * dirX + vy * dirY + vz * dirZ).toFloat()
                
                // sp3 shape is (s + px_plus_py_plus_pz) which translates to (1 + sqrt(3)*dot)
                val psi = (0.5f + 1.2f * dot)
                val prob = (psi * psi).coerceAtMost(2f) / 2f
                
                if (random.nextDouble() < prob) {
                    val r = (random.nextDouble() * 95f + 5f) * abs(psi)
                    val x = (r * vx).toFloat()
                    val y = (r * vy).toFloat()
                    val z = (r * vz).toFloat()
                    
                    val col = if (psi > 0) Color(0x9E3D5AFE) else Color(0x9EFF1744)
                    pointsList.add(OrbitalPoint(Vector3D(x, y, z), col))
                }
            }
        }
    }
    return pointsList
}

/**
 * Generate 3D bounding box wireframe lines.
 */
private fun getBoxLines(half: Float): List<BoxLine> {
    return listOf(
        // Bottom frame
        BoxLine(Vector3D(-half, -half, -half), Vector3D(half, -half, -half)),
        BoxLine(Vector3D(half, -half, -half), Vector3D(half, -half, half)),
        BoxLine(Vector3D(half, -half, half), Vector3D(-half, -half, half)),
        BoxLine(Vector3D(-half, -half, half), Vector3D(-half, -half, -half)),
        
        // Top frame
        BoxLine(Vector3D(-half, half, -half), Vector3D(half, half, -half)),
        BoxLine(Vector3D(half, half, -half), Vector3D(half, half, half)),
        BoxLine(Vector3D(half, half, half), Vector3D(-half, half, half)),
        BoxLine(Vector3D(-half, half, half), Vector3D(-half, half, -half)),
        
        // Vertical pillars
        BoxLine(Vector3D(-half, -half, -half), Vector3D(-half, half, -half)),
        BoxLine(Vector3D(half, -half, -half), Vector3D(half, half, -half)),
        BoxLine(Vector3D(half, -half, half), Vector3D(half, half, half)),
        BoxLine(Vector3D(-half, -half, half), Vector3D(-half, half, half))
    )
}

/**
 * Build Galvanic and Electrolytic labs wireframes in 3D.
 */
private fun addElectrochemistryDrawables(
    mode: String,
    flowProgress: Float,
    width: Float,
    height: Float,
    zoom: Float,
    yaw: Float,
    pitch: Float,
    outList: MutableList<DrawableEntity>
) {
    if (mode == "Galvanic") {
        // Galvanic has Zinc electrode on left, Copper on right
        val leftBeakerCenter = Vector3D(-110f, -40f, 0f)
        val rightBeakerCenter = Vector3D(110f, -40f, 0f)
        
        val pLeft = project(leftBeakerCenter, width, height, zoom, yaw, pitch)
        val pRight = project(rightBeakerCenter, width, height, zoom, yaw, pitch)
        
        // Dynamic water fills
        outList.add(DrawableEntity.ElectroBeakerEntity(-40f, pLeft, 100f, 130f, Color(0x3D00B0FF)))
        outList.add(DrawableEntity.ElectroBeakerEntity(-40f, pRight, 100f, 130f, Color(0x3D2979FF)))
        
        // Left plate Zinc
        val zincPlate1 = Vector3D(-120f, -60f, 10f)
        val zincPlate2 = Vector3D(-100f, -60f, -10f)
        val zincPlate3 = Vector3D(-100f, 40f, -10f)
        val zincPlate4 = Vector3D(-120f, 40f, 10f)
        
        val pz1 = project(zincPlate1, width, height, zoom, yaw, pitch)
        val pz2 = project(zincPlate2, width, height, zoom, yaw, pitch)
        val pz3 = project(zincPlate3, width, height, zoom, yaw, pitch)
        val pz4 = project(zincPlate4, width, height, zoom, yaw, pitch)
        
        outList.add(DrawableEntity.ElectroMetalPlateEntity(pz1.z, pz1, pz2, pz3, pz4, Color(0xFFA0A6AD))) // Grey Zinc
        
        // Right plate Copper
        val copPlate1 = Vector3D(100f, -60f, 10f)
        val copPlate2 = Vector3D(120f, -60f, -10f)
        val copPlate3 = Vector3D(120f, 40f, -10f)
        val copPlate4 = Vector3D(100f, 40f, 10f)
        
        val pc1 = project(copPlate1, width, height, zoom, yaw, pitch)
        val pc2 = project(copPlate2, width, height, zoom, yaw, pitch)
        val pc3 = project(copPlate3, width, height, zoom, yaw, pitch)
        val pc4 = project(copPlate4, width, height, zoom, yaw, pitch)
        
        outList.add(DrawableEntity.ElectroMetalPlateEntity(pc1.z, pc1, pc2, pc3, pc4, Color(0xFFE57373))) // Copper Red
        
        // Salt bridge wire: Arch between left beaker and right beaker
        val saltBridgePoints = (0..10).map { i ->
            val fraction = i / 10f
            val x = -100f + fraction * 200f
            val y = -20f + sin(fraction * PI.toFloat()) * 50f
            Vector3D(x, y, 0f)
        }
        for (i in 0 until saltBridgePoints.size - 1) {
            val p1 = project(saltBridgePoints[i], width, height, zoom, yaw, pitch)
            val p2 = project(saltBridgePoints[i+1], width, height, zoom, yaw, pitch)
            outList.add(DrawableEntity.BoxLineEntity((p1.z + p2.z)/2f, p1, p2))
        }

        // Electron flow in wire (flowing from Zinc to Copper)
        val wirePoints = (0..10).map { i ->
            val fraction = i / 10f
            val x = -110f + fraction * 220f
            val y = 40f + sin(fraction * PI.toFloat()) * 60f
            Vector3D(x, y, 0f)
        }
        for (i in 0 until wirePoints.size - 1) {
            val p1 = project(wirePoints[i], width, height, zoom, yaw, pitch)
            val p2 = project(wirePoints[i+1], width, height, zoom, yaw, pitch)
            outList.add(DrawableEntity.BoxLineEntity((p1.z + p2.z)/2f, p1, p2))
        }

        // Floating glowing electron along current flow
        val eIndex = (flowProgress * 5).toInt() % (wirePoints.size)
        val currentElectronPos = wirePoints[eIndex.coerceIn(0, wirePoints.size - 1)]
        val pe = project(currentElectronPos, width, height, zoom, yaw, pitch)
        outList.add(DrawableEntity.ElectroElectronEntity(pe.z, pe))
    } else {
        // Electrolytic water electrolysis
        val beakerCenter = Vector3D(0f, -40f, 0f)
        val pb = project(beakerCenter, width, height, zoom, yaw, pitch)
        
        // Single central filled beaker
        outList.add(DrawableEntity.ElectroBeakerEntity(-40f, pb, 180f, 130f, Color(0x3D00E5FF)))
        
        // Two inert carbon electrodes side-by-side
        val plateL1 = Vector3D(-45f, -60f, 0f)
        val plateL2 = Vector3D(-35f, -60f, 0f)
        val plateL3 = Vector3D(-35f, 40f, 0f)
        val plateL4 = Vector3D(-45f, 40f, 0f)
        
        val pl1 = project(plateL1, width, height, zoom, yaw, pitch)
        val pl2 = project(plateL2, width, height, zoom, yaw, pitch)
        val pl3 = project(plateL3, width, height, zoom, yaw, pitch)
        val pl4 = project(plateL4, width, height, zoom, yaw, pitch)
        outList.add(DrawableEntity.ElectroMetalPlateEntity(pl1.z, pl1, pl2, pl3, pl4, Color(0xFF37474F))) // Blue grey charcoal
        
        val plateR1 = Vector3D(35f, -60f, 0f)
        val plateR2 = Vector3D(45f, -60f, 0f)
        val plateR3 = Vector3D(45f, 40f, 0f)
        val plateR4 = Vector3D(35f, 40f, 0f)
        
        val pr1 = project(plateR1, width, height, zoom, yaw, pitch)
        val pr2 = project(plateR2, width, height, zoom, yaw, pitch)
        val pr3 = project(plateR3, width, height, zoom, yaw, pitch)
        val pr4 = project(plateR4, width, height, zoom, yaw, pitch)
        outList.add(DrawableEntity.ElectroMetalPlateEntity(pr1.z, pr1, pr2, pr3, pr4, Color(0xFF37474F))) // Carbon cathode

        // Electron floating indicators
        val cycle = (flowProgress * 30).coerceIn(0f, 1000f) % 60f
        // Cathode generating hydrogen gas bubbles (left)
        val bubbleY = -60f + cycle * 1.5f
        if (bubbleY < 10f) {
            val bubV = Vector3D(-40f, bubbleY, 5f)
            val pBub = project(bubV, width, height, zoom, yaw, pitch)
            outList.add(DrawableEntity.ElectroElectronEntity(pBub.z, pBub))
        }
    }
}

/**
 * Rendering calls on Canvas DrawScope.
 */
private fun DrawScope.renderAtom(pa: ProjectedAtom) {
    val radius = if (pa.atom.isLonePair) 8f else (24f * pa.atom.scaleFactor)
    val baseCol = if (pa.atom.isLonePair) Color(0xFFFFFF00) else pa.atom.element.color
    
    // Solid circle core representing atomic nucleon mass
    drawCircle(
        color = baseCol,
        radius = radius,
        center = Offset(pa.screenPos.x, pa.screenPos.y)
    )
    
    // Accent gloss ring for real dynamic visual high-fidelity 3D aesthetic
    drawCircle(
        color = Color.White.copy(alpha = 0.5f),
        radius = radius * 0.45f,
        center = Offset(pa.screenPos.x - radius * 0.3f, pa.screenPos.y - radius * 0.3f)
    )

    // Dark outline boundary
    drawCircle(
        color = Color(0xFF121212),
        radius = radius,
        center = Offset(pa.screenPos.x, pa.screenPos.y),
        style = Stroke(width = 2f)
    )
}

private fun DrawScope.renderBond(bond: Bond3D, from: ProjectedAtom, to: ProjectedAtom) {
    val p1 = Offset(from.screenPos.x, from.screenPos.y)
    val p2 = Offset(to.screenPos.x, to.screenPos.y)
    val color = if (bond.isHydrogenBond) Color(0xFF03A9F4) else Color(0xFFCFD8DC)
    
    when (bond.order) {
        1 -> {
            drawLine(
                color = color,
                start = p1,
                end = p2,
                strokeWidth = 9f,
                cap = StrokeCap.Round
            )
            // Black segment outline borders
            drawLine(
                color = Color(0xFF1E1E1E),
                start = p1,
                end = p2,
                strokeWidth = 11f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = p1,
                end = p2,
                strokeWidth = 7f,
                cap = StrokeCap.Round
            )
        }
        2 -> {
            // Draw dual bonds shifted slightly sideways
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val len = sqrt(dx * dx + dy * dy)
            if (len > 0) {
                val nx = -dy / len * 6f
                val ny = dx / len * 6f
                
                drawLine(Color(0xFF1E1E1E), p1 + Offset(nx, ny), p2 + Offset(nx, ny), 9f, StrokeCap.Round)
                drawLine(color, p1 + Offset(nx, ny), p2 + Offset(nx, ny), 5f, StrokeCap.Round)
                
                drawLine(Color(0xFF1E1E1E), p1 - Offset(nx, ny), p2 - Offset(nx, ny), 9f, StrokeCap.Round)
                drawLine(color, p1 - Offset(nx, ny), p2 - Offset(nx, ny), 5f, StrokeCap.Round)
            }
        }
        3 -> {
            // Draw triple bond
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val len = sqrt(dx * dx + dy * dy)
            if (len > 0) {
                val nx = -dy / len * 9f
                val ny = dx / len * 9f
                
                // Central line
                drawLine(Color(0xFF1E1E1E), p1, p2, 9f, StrokeCap.Round)
                drawLine(color, p1, p2, 4f, StrokeCap.Round)
                
                // Outer lines
                drawLine(Color(0xFF1E1E1E), p1 + Offset(nx, ny), p2 + Offset(nx, ny), 9f, StrokeCap.Round)
                drawLine(color, p1 + Offset(nx, ny), p2 + Offset(nx, ny), 4f, StrokeCap.Round)
                
                drawLine(Color(0xFF1E1E1E), p1 - Offset(nx, ny), p2 - Offset(nx, ny), 9f, StrokeCap.Round)
                drawLine(color, p1 - Offset(nx, ny), p2 - Offset(nx, ny), 4f, StrokeCap.Round)
            }
        }
    }
}

private fun DrawScope.renderOrbitalPoint(pos: Vector3D, color: Color) {
    drawCircle(
        color = color,
        radius = 4.5f,
        center = Offset(pos.x, pos.y)
    )
}

private fun DrawScope.renderGasParticle(pos: Vector3D) {
    // Dynamic collision point glow
    drawCircle(
        color = Color(0xFF00FFCC),
        radius = 5.5f,
        center = Offset(pos.x, pos.y)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.8f),
        radius = 2f,
        center = Offset(pos.x - 1f, pos.y - 1f)
    )
}

private fun DrawScope.renderBoxLine(p1: Vector3D, p2: Vector3D) {
    drawLine(
        color = Color(0x3DFFFFFF),
        start = Offset(p1.x, p1.y),
        end = Offset(p2.x, p2.y),
        strokeWidth = 2.5f
    )
}

private fun DrawScope.renderMetalPlate(
    p1: Vector3D, p2: Vector3D, p3: Vector3D, p4: Vector3D,
    color: Color
) {
    val path = Path().apply {
        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        lineTo(p4.x, p4.y)
        close()
    }
    drawPath(path, color)
    drawPath(path, Color(0xFF1A1F29), style = Stroke(width = 2.5f))
}

private fun DrawScope.renderElectron(pos: Vector3D) {
    drawCircle(
        color = Color(0xFFFFFF00),
        radius = 7f,
        center = Offset(pos.x, pos.y)
    )
    drawCircle(
        color = Color(0xFFFF9100),
        radius = 4f,
        center = Offset(pos.x, pos.y)
    )
}

private fun DrawScope.renderBeaker(
    center: Vector3D,
    width: Float,
    height: Float,
    liquidColor: Color
) {
    val left = center.x - width / 2f
    val right = center.x + width / 2f
    val bottom = center.y + height / 2f
    val top = center.y - height / 2f
    
    // Render liquid body inside beaker
    val liquidTop = center.y + height * 0.1f
    val liquidPath = Path().apply {
        moveTo(left + 3f, liquidTop)
        lineTo(right - 3f, liquidTop)
        lineTo(right - 3f, bottom - 3f)
        lineTo(left + 3f, bottom - 3f)
        close()
    }
    drawPath(liquidPath, liquidColor)

    // Draw glass beaker outline border
    val beakerOutline = Path().apply {
        moveTo(left, top)
        lineTo(left, bottom)
        lineTo(right, bottom)
        lineTo(right, top)
    }
    drawPath(beakerOutline, Color(0x5EFFFFFF), style = Stroke(width = 4.5f))
    
    drawPath(beakerOutline, Color(0x1FCE93D8)) // light purple tint
}
