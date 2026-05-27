package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.ChemistryDatabase
import com.example.database.ChemistryRepository
import com.example.database.SavedCompound
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ChemistryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChemistryRepository

    // Active screen navigation token
    var activeModule = MutableStateFlow("orbitals") // "orbitals", "mechanisms", "gas_laws", "electrochem", "periodic_table"

    // --- MODULE 1: ATOMIC ORBITALS STATE ---
    val orbitalLevels = listOf(1, 2, 3)
    val selectedOrbitalLevel = MutableStateFlow(1)
    val selectedOrbitalType = MutableStateFlow("s")  // "s", "p", "d", "sp3"
    val showOrbitalPhaseColor = MutableStateFlow(true)

    // --- MODULE 2: REACTION MECHANISM REPLAY STATE ---
    val selectedMechanism = MutableStateFlow("SN2") // "SN2", "Aldol", "E2"
    val isPlayingMechanism = MutableStateFlow(false)
    val mechanismProgress = MutableStateFlow(0f) // 0.0f to 1.0f

    // --- MODULE 3: GAS LAWS EXPERIENCES ---
    val gasTempKelvin = MutableStateFlow(300f) // 100K - 1000K
    val gasPressureAtm = MutableStateFlow(1.0f) // 0.5 to 10.0 atm
    val gasMoleCount = MutableStateFlow(2.0f) // 1.0 to 5.0 mol
    val gasVolumeLiters = MutableStateFlow(25f) // derived iteratively
    val gasEquationType = MutableStateFlow("Ideal") // "Ideal", "VanDerWaals"

    private val _gasParticles = MutableStateFlow<List<Vector3D>>(emptyList())
    val gasParticles: StateFlow<List<Vector3D>> = _gasParticles.asStateFlow()

    // --- MODULE 4: ELECTROCHEMISTRY LAB STATE ---
    val selectedElectroCell = MutableStateFlow("Galvanic") // "Galvanic", "Electrolytic"
    val eCellVoltage = MutableStateFlow(1.10f) // Volts
    val isElectroActive = MutableStateFlow(true)
    val electroProgressSec = MutableStateFlow(0f)

    // Database access flow integration
    val savedCompounds: StateFlow<List<SavedCompound>>

    // Dynamic molecule projection coordinates for Module Visualizers
    private val _activeAtoms = MutableStateFlow<List<Atom3D>>(emptyList())
    val activeAtoms: StateFlow<List<Atom3D>> = _activeAtoms.asStateFlow()

    private val _activeBonds = MutableStateFlow<List<Bond3D>>(emptyList())
    val activeBonds: StateFlow<List<Bond3D>> = _activeBonds.asStateFlow()

    // Job trackers for real-time physics simulators
    private var collisionEngineJob: Job? = null
    private var mechanismScrubberJob: Job? = null
    private var electrochemLoopJob: Job? = null

    init {
        val database = ChemistryDatabase.getDatabase(application)
        repository = ChemistryRepository(database.chemistryDao())
        
        savedCompounds = repository.allCompounds.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Generate default elements and presets on run
        viewModelScope.launch {
            repository.ensureInitialPresets()
        }

        // Keep orbital preview aligned initially
        updateOrbitalAtoms()
        
        // Start loops
        startPhysicsLoops()
    }

    private fun startPhysicsLoops() {
        // 1. Gas Laws Engine loop (60 FPS atomic particle bouncing update)
        collisionEngineJob?.cancel()
        collisionEngineJob = viewModelScope.launch(Dispatchers.Default) {
            val random = java.util.Random()
            var particles = List(180) {
                Vector3D(
                    (random.nextFloat() - 0.5f) * 120f,
                    (random.nextFloat() - 0.5f) * 120f,
                    (random.nextFloat() - 0.5f) * 120f
                )
            }
            var velocities = List(180) {
                Vector3D(
                    (random.nextFloat() - 0.5f) * 10f,
                    (random.nextFloat() - 0.5f) * 10f,
                    (random.nextFloat() - 0.5f) * 10f
                )
            }

            while (true) {
                val temp = gasTempKelvin.value
                val equation = gasEquationType.value
                
                // Adjust velocities according to Root Mean Square molecular physical velocity: v = sqrt(3RT / M)
                val speedFactor = sqrt(temp / 300f) * 2f

                // Gas laws mathematical volume boundary calculation
                // For Ideal: V = nRT / P. For simplified UI view, let's derive box height
                val calculatedVolume = if (equation == "Ideal") {
                    (gasMoleCount.value * 0.0821f * temp) / gasPressureAtm.value
                } else {
                    // Van Der Waals correction constants for inert Gas
                    // (P + a* (n/V)^2 ) * (V - n*b) = nRT
                    val a = 1.36f // pressure correction
                    val b = 0.0318f // volume volume correction
                    val n = gasMoleCount.value
                    // Approximate solving for cubic root volume projection:
                    ((n * 0.0821f * temp) / gasPressureAtm.value) - (n * b) + (a * n / 10f)
                }
                
                // Set boundary bounds derived from math volume
                val halfBound = (calculatedVolume * 6f).coerceIn(40f, 150f)
                
                particles = particles.mapIndexed { idx, p ->
                    var vel = velocities[idx]
                    
                    // Update state positions
                    var nx = p.x + vel.x * speedFactor * 0.1f
                    var ny = p.y + vel.y * speedFactor * 0.1f
                    var nz = p.z + vel.z * speedFactor * 0.1f

                    // Bounding wall elastic collision detection
                    if (nx < -halfBound) { nx = -halfBound; vel = vel.copy(x = -vel.x) }
                    if (nx > halfBound) { nx = halfBound; vel = vel.copy(x = -vel.x) }
                    
                    if (ny < -halfBound) { ny = -halfBound; vel = vel.copy(y = -vel.y) }
                    if (ny > halfBound) { ny = halfBound; vel = vel.copy(y = -vel.y) }
                    
                    if (nz < -halfBound) { nz = -halfBound; vel = vel.copy(z = -vel.z) }
                    if (nz > halfBound) { nz = halfBound; vel = vel.copy(z = -vel.z) }

                    // Apply updated speeds
                    velocities = velocities.mapIndexed { vIdx, v -> if (vIdx == idx) vel else v }

                    Vector3D(nx, ny, nz)
                }
                
                _gasParticles.value = particles
                delay(16) // tick rate at 60 FPS
            }
        }

        // 2. Reaction Mechanism animation player pipeline
        mechanismScrubberJob?.cancel()
        mechanismScrubberJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                if (isPlayingMechanism.value) {
                    var progress = mechanismProgress.value + 0.005f
                    if (progress > 1.05f) {
                        progress = 0f
                    }
                    mechanismProgress.value = progress.coerceIn(0f, 1f)
                    
                    // Project dynamic 3D molecular coordinates matching progress
                    updateMechanismCoordinates(selectedMechanism.value, mechanismProgress.value)
                }
                delay(20)
            }
        }

        // 3. Electrochemistry electron circulation flow tick rate
        electrochemLoopJob?.cancel()
        electrochemLoopJob = viewModelScope.launch(Dispatchers.Default) {
            var timeAcc = 0f
            while (true) {
                if (isElectroActive.value) {
                    timeAcc += 0.05f
                    electroProgressSec.value = timeAcc
                }
                delay(40)
            }
        }
    }

    // Dynamic Orbital Renderer setup coordinates
    fun updateOrbitalAtoms() {
        val lvl = selectedOrbitalLevel.value
        val type = selectedOrbitalType.value
        val carbon = PeriodicTable.getByNum(6)
        
        when (type) {
            "s" -> {
                _activeAtoms.value = listOf(Atom3D(1, carbon, Vector3D(0f, 0f, 0f), hybridization = HybridizationState.Unhybridized))
                _activeBonds.value = emptyList()
            }
            "p" -> {
                _activeAtoms.value = listOf(Atom3D(1, carbon, Vector3D(0f, 0f, 0f), hybridization = HybridizationState.Unhybridized))
                _activeBonds.value = emptyList()
            }
            "d" -> {
                _activeAtoms.value = listOf(
                    Atom3D(1, carbon, Vector3D(0f, 0f, 0f), hybridization = HybridizationState.Unhybridized)
                )
                _activeBonds.value = emptyList()
            }
            "sp3" -> {
                // Interactive 3D Hybridization Sandbox bonds mapping
                val listAtoms = mutableListOf<Atom3D>()
                val listBonds = mutableListOf<Bond3D>()
                
                // Methane Core
                listAtoms.add(Atom3D(1, carbon, Vector3D(0f, 0f, 0f), label = "C", hybridization = HybridizationState.SP3))
                
                // Four Hydrogen bond vectors calculated based on VSEPR 109.5
                val hElement = PeriodicTable.getByNum(1)
                val sp3Vectors = HybridizationState.SP3.bondVectors
                
                sp3Vectors.forEachIndexed { idx, dirVector ->
                    val hPos = dirVector * 80f
                    listAtoms.add(Atom3D(idx + 2, hElement, hPos, label = "H"))
                    listBonds.add(Bond3D(1, idx + 2, order = 1))
                }
                _activeAtoms.value = listAtoms
                _activeBonds.value = listBonds
            }
        }
    }

    /**
     * Compute Walden Inversion dynamics for SN2 and transition states.
     */
    fun updateMechanismCoordinates(mechanism: String, progress: Float) {
        val carbon = PeriodicTable.getByNum(6)
        val hElement = PeriodicTable.getByNum(1)
        val clElement = PeriodicTable.getByNum(17) // Nucleophile
        val brElement = PeriodicTable.getByNum(35) // Leaving group

        val atomsList = mutableListOf<Atom3D>()
        val bondsList = mutableListOf<Bond3D>()

        when (mechanism) {
            "SN2" -> {
                // Nucleophile Cl- attacks from left (-X), Leaving group Br- exits on right (+X)
                // Transition state occurs exactly at progress = 0.5 where Central carbon is planar with Hydrogens (sp2 planar)
                
                // X coordinates of Cl and Br dynamically projected
                val clX = -130f + progress * 80f              // Moving inside and coordinates overlap bonding range
                val brX = if (progress < 0.5f) {
                    75f + progress * 20f
                } else {
                    75f + progress * 150f                      // Rapid detachment of bromide ion
                }

                // Central carbon coordinates with slight Walden inversion movement
                // Moves slightly forward to make a planar inversion
                val cX = -10f + progress * 20f

                atomsList.add(Atom3D(1, carbon, Vector3D(cX, 0f, 0f), label = "C", scaleFactor = 1.1f))
                
                // Nucleophile Cl (Green)
                if (progress < 0.95f) {
                    atomsList.add(Atom3D(2, clElement, Vector3D(clX, 0f, 0f), label = "Cl"))
                    if (progress < 0.5f) {
                        bondsList.add(Bond3D(1, 2, order = 1, isHydrogenBond = true)) // Weak oncoming coordination
                    } else {
                        bondsList.add(Bond3D(1, 2, order = 1)) // Fully bonded chloromethane
                    }
                }
                
                // Leaving group Br (Red-Brown)
                if (progress < 0.99f) {
                    atomsList.add(Atom3D(3, brElement, Vector3D(brX, 0f, 0f), label = "Br"))
                    if (progress < 0.5f) {
                        bondsList.add(Bond3D(1, 3, order = 1)) // Bonded initially
                    } else {
                        bondsList.add(Bond3D(1, 3, order = 1, isHydrogenBond = true)) // Detaching weak bond
                    }
                }

                // Three Hydrogen peripheral coordinate rotations transitioning from left-facing pyramid to right-facing pyramid
                // Inversion scaling angle factor
                val angleFactor = cos(progress * Math.PI).toFloat() // 1.0 -> 0.0 -> -1.0
                
                // Hydrogen 1 (axial plane)
                atomsList.add(Atom3D(4, hElement, Vector3D(cX + angleFactor * 25f, 50f, 0f), label = "H"))
                bondsList.add(Bond3D(1, 4))
                
                // Hydrogen 2 (pointing out front)
                atomsList.add(Atom3D(5, hElement, Vector3D(cX + angleFactor * 25f, -25f, 43.3f), label = "H"))
                bondsList.add(Bond3D(1, 5))
                
                // Hydrogen 3 (pointing backend)
                atomsList.add(Atom3D(6, hElement, Vector3D(cX + angleFactor * 25f, -25f, -43.3f), label = "H"))
                bondsList.add(Bond3D(1, 6))

                _activeAtoms.value = atomsList
                _activeBonds.value = bondsList
            }
            "E2" -> {
                // Elimination Reaction: Base OH- grabs Hydrogen, C=C double bond forms, Leaving Br- exits
                val Base_H_fraction = progress.coerceIn(0f, 1f)
                
                // Carbon 1 (left) and Carbon 2 (right)
                val c1X = -40f
                val c2X = 40f
                atomsList.add(Atom3D(1, carbon, Vector3D(c1X, 0f, 0f), label = "C1", scaleFactor = 1.15f))
                atomsList.add(Atom3D(2, carbon, Vector3D(c2X, 0f, 0f), label = "C2", scaleFactor = 1.15f))

                // Carbon double bond dynamic order configuration transitions from single to double
                val bondOrder = if (progress < 0.5f) 1 else 2
                bondsList.add(Bond3D(1, 2, order = bondOrder))

                // Hydrogen to be abstracted dynamically lifts up closer to imaginary Base OH
                val hyY = 50f + Base_H_fraction * 70f
                val hyX = c1X - Base_H_fraction * 20f
                atomsList.add(Atom3D(3, hElement, Vector3D(hyX, hyY, 0f), label = "H+"))
                
                if (progress < 0.6f) {
                    bondsList.add(Bond3D(1, 3, order = 1)) // Bonded to C1 early on
                }

                // Base oxygen (Red)
                val baseOxygen = PeriodicTable.getByNum(8)
                val boY = 150f - (1f - Base_H_fraction) * 30f
                atomsList.add(Atom3D(4, baseOxygen, Vector3D(c1X - 35f, boY, 0f), label = "OH-", scaleFactor = 1.1f))
                if (progress > 0.4f) {
                    bondsList.add(Bond3D(4, 3, order = 1, isHydrogenBond = true)) // Water forming
                }

                // Br leaving group on Carbon 2 (Red-Brown)
                val brY = -50f - Base_H_fraction * 100f
                atomsList.add(Atom3D(5, brElement, Vector3D(c2X + 25f, brY, 0f), label = "Br-"))
                if (progress < 0.6f) {
                    bondsList.add(Bond3D(2, 5, order = 1))
                }

                // Static peripheral Hydrogens
                atomsList.add(Atom3D(6, hElement, Vector3D(c1X - 30f, -40f, 35f), label = "H"))
                bondsList.add(Bond3D(1, 6))
                
                atomsList.add(Atom3D(7, hElement, Vector3D(c2X + 30f, 40f, 35f), label = "H"))
                bondsList.add(Bond3D(2, 7))

                _activeAtoms.value = atomsList
                _activeBonds.value = bondsList
            }
            "Aldol" -> {
                // Condensation pathway joining carbon nucleophile enolate and aldehyde carbonyl
                val offsetFraction = progress * 45f
                
                // Reactant 1: Enolate ion (Carbon nucleophile)
                atomsList.add(Atom3D(1, carbon, Vector3D(-50f + offsetFraction, 0f, 0f), label = "C1", scaleFactor = 1.1f))
                val oxygen = PeriodicTable.getByNum(8)
                atomsList.add(Atom3D(2, oxygen, Vector3D(-50f + offsetFraction, 50f, 0f), label = "O1"))
                bondsList.add(Bond3D(1, 2, order = 2))

                // Reactant 2: Carbonyl carbon acceptor electrophile
                atomsList.add(Atom3D(3, carbon, Vector3D(50f - offsetFraction, 0f, 0f), label = "C2", scaleFactor = 1.1f))
                atomsList.add(Atom3D(4, oxygen, Vector3D(50f - offsetFraction, -50f, 0f), label = "O2"))
                
                val c2o2BondOrder = if (progress > 0.5f) 1 else 2
                bondsList.add(Bond3D(3, 4, order = c2o2BondOrder))

                // Aldol alpha-carbon coordination bond forms
                if (progress > 0.5f) {
                    bondsList.add(Bond3D(1, 3, order = 1, isHydrogenBond = (progress < 0.75f)))
                }

                // Add hydrogen boundaries
                atomsList.add(Atom3D(5, hElement, Vector3D(-90f + offsetFraction, -20f, 0f), label = "H"))
                bondsList.add(Bond3D(1, 5))
                atomsList.add(Atom3D(6, hElement, Vector3D(90f - offsetFraction, 20f, 0f), label = "H"))
                bondsList.add(Bond3D(3, 6))

                _activeAtoms.value = atomsList
                _activeBonds.value = bondsList
            }
        }
    }

    override fun onCleared() {
        collisionEngineJob?.cancel()
        mechanismScrubberJob?.cancel()
        electrochemLoopJob?.cancel()
        super.onCleared()
    }
}
