package com.example.model

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Immutable 3D vector representing coordinates or directions in simulator space.
 */
data class Vector3D(
    val x: Float,
    val y: Float,
    val z: Float
) {
    operator fun plus(other: Vector3D) = Vector3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3D) = Vector3D(x - other.x, y - other.y, z - other.z)
    operator fun times(factor: Float) = Vector3D(x * factor, y * factor, z * factor)
    
    fun dot(other: Vector3D): Float = x * other.x + y * other.y + z * other.z
    fun cross(other: Vector3D): Vector3D = Vector3D(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )
    
    fun magnitude(): Float = sqrt(x * x + y * y + z * z)
    fun normalized(): Vector3D {
        val mag = magnitude()
        return if (mag == 0f) this else times(1f / mag)
    }

    /**
     * Rotate around the X-axis by the given pitch angle (in radians).
     */
    fun rotateX(angleRad: Float): Vector3D {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        return Vector3D(
            x = x,
            y = y * cosA - z * sinA,
            z = y * sinA + z * cosA
        )
    }

    /**
     * Rotate around the Y-axis by the given yaw angle (in radians).
     */
    fun rotateY(angleRad: Float): Vector3D {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        return Vector3D(
            x = x * cosA + z * sinA,
            y = y,
            z = -x * sinA + z * cosA
        )
    }

    /**
     * Rotate around the Z-axis by the given roll angle (in radians).
     */
    fun rotateZ(angleRad: Float): Vector3D {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        return Vector3D(
            x = x * cosA - y * sinA,
            y = x * sinA + y * cosA,
            z = z
        )
    }
}

/**
 * Sealed representation of atomic orbital shapes and geometries.
 */
sealed class OrbitalType {
    abstract val label: String
    abstract val energyLevel: Int

    data class S(override val energyLevel: Int) : OrbitalType() {
        override val label: String = "${energyLevel}s"
    }

    data class P(override val energyLevel: Int, val direction: PDirection) : OrbitalType() {
        override val label: String = "${energyLevel}p_${direction.name.lowercase()}"
        enum class PDirection { X, Y, Z }
    }

    data class D(override val energyLevel: Int, val subshell: DSubshell) : OrbitalType() {
        override val label: String = "${energyLevel}d_${subshell.name.lowercase()}"
        enum class DSubshell { XY, XZ, YZ, X2Y2, Z2 }
    }

    data class F(override val energyLevel: Int, val index: Int) : OrbitalType() {
        override val label: String = "${energyLevel}f_${index}"
    }
}

/**
 * Represent orbital hybridization states with VSEPR bond angles.
 */
sealed class HybridizationState(
    val name: String,
    val geometry: String,
    val bondAngle: Float, // in degrees
    val bondVectors: List<Vector3D> // relative unit-length vectors for bonds
) {
    object Unhybridized : HybridizationState(
        name = "S/P Pure",
        geometry = "Spherical/Orthogonal",
        bondAngle = 90.0f,
        bondVectors = listOf(Vector3D(1f, 0f, 0f), Vector3D(0f, 1f, 0f), Vector3D(0f, 0f, 1f))
    )

    object SP : HybridizationState(
        name = "sp",
        geometry = "Linear",
        bondAngle = 180.0f,
        bondVectors = listOf(
            Vector3D(1f, 0f, 0f),
            Vector3D(-1f, 0f, 0f)
        )
    )

    object SP2 : HybridizationState(
        name = "sp²",
        geometry = "Trigonal Planar",
        bondAngle = 120.0f,
        bondVectors = listOf(
            Vector3D(0f, 1f, 0f),
            Vector3D(0.866f, -0.5f, 0f),
            Vector3D(-0.866f, -0.5f, 0f)
        )
    )

    object SP3 : HybridizationState(
        name = "sp³",
        geometry = "Tetrahedral",
        bondAngle = 109.47f,
        bondVectors = listOf(
            Vector3D(0f, 1.0f, 0f),
            Vector3D(0.816f, -0.333f, 0.471f),
            Vector3D(-0.408f, -0.333f, 0.816f),
            Vector3D(-0.408f, -0.333f, -0.816f)
        )
    )

    object SP3D : HybridizationState(
        name = "sp³d",
        geometry = "Trigonal Bipyramidal",
        bondAngle = 120.0f, // 120 degrees equatorial, 90 degrees axial
        bondVectors = listOf(
            Vector3D(0f, 0f, 1f),   // Axial top
            Vector3D(0f, 0f, -1f),  // Axial bottom
            Vector3D(1f, 0f, 0f),   // Equatorial 1
            Vector3D(-0.5f, 0.866f, 0f), // Equatorial 2
            Vector3D(-0.5f, -0.866f, 0f) // Equatorial 3
        )
    )

    object SP3D2 : HybridizationState(
        name = "sp³d²",
        geometry = "Octahedral",
        bondAngle = 90.0f,
        bondVectors = listOf(
            Vector3D(1f, 0f, 0f), Vector3D(-1f, 0f, 0f),
            Vector3D(0f, 1f, 0f), Vector3D(0f, -1f, 0f),
            Vector3D(0f, 0f, 1f), Vector3D(0f, 0f, -1f)
        )
    )
}

/**
 * Standard CPK coloring hex definitions and chemical element constants.
 */
data class ChemicalElement(
    val atomicNumber: Int,
    val symbol: String,
    val name: String,
    val mass: Double,
    val valenceElectrons: Int,
    val Group: Int,
    val Period: Int,
    val electronegativity: Double,
    val shellConfigurationString: String,
    val colorHex: String
) {
    val color: Color
        get() = Color(android.graphics.Color.parseColor(colorHex))
}

/**
 * Full pre-filled Periodic Table repository to cover the HSC chemistry database.
 */
object PeriodicTable {
    val elementsList = listOf(
        ChemicalElement(1, "H", "Hydrogen", 1.008, 1, 1, 1, 2.22, "1s¹", "#FFFFFF"),
        ChemicalElement(2, "He", "Helium", 4.0026, 2, 18, 1, 0.0, "1s²", "#D9FFFF"),
        ChemicalElement(3, "Li", "Lithium", 6.94, 1, 1, 2, 0.98, "[He] 2s¹", "#CC80FF"),
        ChemicalElement(4, "Be", "Beryllium", 9.0122, 2, 2, 2, 1.57, "[He] 2s²", "#C2FF00"),
        ChemicalElement(5, "B", "Boron", 10.81, 3, 13, 2, 2.04, "[He] 2s² 2p¹", "#FFB5B5"),
        ChemicalElement(6, "C", "Carbon", 12.011, 4, 14, 2, 2.55, "[He] 2s² 2p²", "#333333"), // Dark Slate Grey for Carbon
        ChemicalElement(7, "N", "Nitrogen", 14.007, 5, 15, 2, 3.04, "[He] 2s² 2p³", "#3050F8"), // Intense Blue
        ChemicalElement(8, "O", "Oxygen", 15.999, 6, 16, 2, 3.44, "[He] 2s² 2p⁴", "#FF0D0D"), // Red Hot
        ChemicalElement(9, "F", "Fluorine", 18.998, 7, 17, 2, 3.98, "[He] 2s² 2p⁵", "#90E050"),
        ChemicalElement(10, "Ne", "Neon", 20.180, 8, 18, 2, 0.0, "[He] 2s² 2p⁶", "#B3E3F5"),
        ChemicalElement(11, "Na", "Sodium", 22.990, 1, 1, 3, 0.93, "[Ne] 3s¹", "#AB5CF2"),
        ChemicalElement(12, "Mg", "Magnesium", 24.305, 2, 2, 3, 1.31, "[Ne] 3s²", "#8AFF00"),
        ChemicalElement(13, "Al", "Aluminum", 26.982, 3, 13, 3, 1.61, "[Ne] 3s² 3p¹", "#BFA6A6"),
        ChemicalElement(14, "Si", "Silicon", 28.085, 4, 14, 3, 1.9, "[Ne] 3s² 3p²", "#F0C8A0"),
        ChemicalElement(15, "P", "Phosphorus", 30.974, 5, 15, 3, 2.19, "[Ne] 3s² 3p³", "#FF8000"), // Orange
        ChemicalElement(16, "S", "Sulfur", 32.06, 6, 16, 3, 2.58, "[Ne] 3s² 3p⁴", "#FFFF30"), // Yellow
        ChemicalElement(17, "Cl", "Chlorine", 35.45, 7, 17, 3, 3.16, "[Ne] 3s² 3p⁵", "#1FF01F"), // Green
        ChemicalElement(18, "Ar", "Argon", 39.948, 8, 18, 3, 0.0, "[Ne] 3s² 3p⁶", "#80E1E1"),
        ChemicalElement(19, "K", "Potassium", 39.098, 1, 1, 4, 0.82, "[Ar] 4s¹", "#8F40D4"),
        ChemicalElement(20, "Ca", "Calcium", 40.078, 2, 2, 4, 1.0, "[Ar] 4s²", "#3DFF00"),
        ChemicalElement(26, "Fe", "Iron", 55.845, 8, 8, 4, 1.83, "[Ar] 3d⁶ 4s²", "#E06633"),
        ChemicalElement(29, "Cu", "Copper", 63.546, 11, 11, 4, 1.9, "[Ar] 3d¹⁰ 4s¹", "#C88033"),
        ChemicalElement(30, "Zn", "Zinc", 65.38, 2, 12, 4, 1.65, "[Ar] 3d¹⁰ 4s²", "#7D80B0"),
        ChemicalElement(35, "Br", "Bromine", 79.904, 7, 17, 4, 2.96, "[Ar] 3d¹⁰ 4s² 4p⁵", "#A62929")
    )

    fun getByNum(number: Int) = elementsList.firstOrNull { it.atomicNumber == number } ?: elementsList[5] // Carbon fallback
    fun getBySymbol(symbol: String) = elementsList.firstOrNull { it.symbol.lowercase() == symbol.lowercase() } ?: elementsList[5]
}

/**
 * 3D Render representation of an Atom instance at coordinate projection runtime.
 */
data class Atom3D(
    val id: Int,
    val element: ChemicalElement,
    var position: Vector3D, // Real 3D physical position
    val label: String = element.symbol,
    val hybridization: HybridizationState = HybridizationState.Unhybridized,
    val isLonePair: Boolean = false,
    val scaleFactor: Float = 1.0f
)

/**
 * Bond representation.
 */
data class Bond3D(
    val fromAtomId: Int,
    val toAtomId: Int,
    val order: Int = 1, // 1 for single, 2 for double, 3 for triple
    val isHydrogenBond: Boolean = false
)
