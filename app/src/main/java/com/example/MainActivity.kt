package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.Chemistry3DCanvas
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ChemistryViewModel
import kotlinx.coroutines.launch

import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    private val viewModel: ChemistryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF111318) // Deep dark Geometric Balance background
                ) {
                    MainSimulationShell(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSimulationShell(viewModel: ChemistryViewModel) {
    val activeModule by viewModel.activeModule.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 640

    // Side navigation items
    val navigationItems = listOf(
        NavigationItem("Quantum Physics", "orbitals", Icons.Default.Lens),
        NavigationItem("Organic Replay", "mechanisms", Icons.Default.Biotech),
        NavigationItem("Gas Chamber", "gas_laws", Icons.Default.Thermostat),
        NavigationItem("Electrochem Lab", "electrochem", Icons.Default.FlashOn),
        NavigationItem("Periodic Table", "periodic_table", Icons.Default.Apps)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isTablet,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1D1B20), // Geometric Balance panel color
                modifier = Modifier.width(280.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFD0BCFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "AtomoCore Logo",
                            tint = Color(0xFF381E72),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AtomoCore",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "QUANTUM ARCHITECT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD0BCFF),
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFF49454F).copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                navigationItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null, tint = if (activeModule == item.id) Color(0xFFD0BCFF) else Color.LightGray) },
                        label = { Text(item.title, fontWeight = FontWeight.SemiBold, color = Color.White) },
                        selected = activeModule == item.id,
                        onClick = {
                            viewModel.activeModule.value = item.id
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFD0BCFF).copy(alpha = 0.12f),
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .testTag("nav_item_${item.id}")
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            val titleText = when (activeModule) {
                                "orbitals" -> "Quantum Orbitals Sandbox"
                                "mechanisms" -> "Reaction Mechanism Replayer"
                                "gas_laws" -> "Thermodynamic Gas Laws"
                                "electrochem" -> "Electrochemistry Laboratory"
                                "periodic_table" -> "Interactive Periodic Table"
                                else -> "Simulations Engine"
                            }
                            Text(
                                text = titleText,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = "AtomoCore v4.0 • Quantum Architect",
                                color = Color(0xFFD0BCFF),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                        }
                    },
                    navigationIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("menu_button")
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu Drawer", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFD0BCFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = "AtomoCore Logo",
                                    tint = Color(0xFF381E72),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF111318), // Pure theme dark
                        titleContentColor = Color.White
                    ),
                    modifier = Modifier.border(width = 1.dp, color = Color(0xFF49454F).copy(alpha = 0.4f))
                )
            },
            containerColor = Color(0xFF111318)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Large tablet navigation panel or default stacked drawer views
                Row(modifier = Modifier.fillMaxSize()) {
                    if (isTablet) {
                        // Persistent elegant sidebar
                        Column(
                            modifier = Modifier
                                .width(220.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF111318))
                                .border(width = 1.dp, color = Color(0xFF49454F).copy(alpha = 0.4f))
                                .padding(vertical = 16.dp)
                        ) {
                            navigationItems.forEach { item ->
                                val isSelected = activeModule == item.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.activeModule.value = item.id }
                                        .background(if (isSelected) Color(0xFFD0BCFF).copy(alpha = 0.08f) else Color.Transparent)
                                        .padding(horizontal = 20.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(item.icon, contentDescription = null, tint = if (isSelected) Color(0xFFD0BCFF) else Color.LightGray, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(item.title, color = if (isSelected) Color.White else Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Crossfade(
                            targetState = activeModule
                        ) { targetView ->
                            when (targetView) {
                                "orbitals" -> QuantumOrbitalsView(viewModel)
                                "mechanisms" -> ReactionMechanismView(viewModel)
                                "gas_laws" -> GasLawsView(viewModel)
                                "electrochem" -> ElectrochemistryView(viewModel)
                                "periodic_table" -> PeriodicTableView()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * MODULE 1: Atomic Wavefunction Orbitals & Hybridization view.
 */
@Composable
fun QuantumOrbitalsView(viewModel: ChemistryViewModel) {
    val level by viewModel.selectedOrbitalLevel.collectAsState()
    val type by viewModel.selectedOrbitalType.collectAsState()
    val showPhase by viewModel.showOrbitalPhaseColor.collectAsState()
    
    val atoms by viewModel.activeAtoms.collectAsState()
    val bonds by viewModel.activeBonds.collectAsState()

    val orbitalCloudString = when (type) {
        "s" -> if (level == 1) "1s" else "1s"
        "p" -> "2pz"
        "d" -> "3dz2"
        "sp3" -> "sp3_lobe"
        else -> "None"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxWidth()
                .background(Color(0xFF111318))
        ) {
            // Interactive 3D Orbitals cloud canvas
            Chemistry3DCanvas(
                atoms = atoms,
                bonds = bonds,
                orbitalCloudType = orbitalCloudString
            )

            // Dynamic float guide card
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                colors = CardDefaults.cardColors(containerColor = Color(0xCE1D1B20)),
                border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Interactive 3D Render", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("• Drag to rotate orbitals", color = Color.Gray, fontSize = 10.sp)
                    Text("• Double finger pinch to zoom", color = Color.Gray, fontSize = 10.sp)
                    if (type == "sp3") {
                        Text("• Hybridized Geometry: Tetrahedral", color = Color(0xFFD0BCFF), fontSize = 10.sp)
                    }
                }
            }
        }

        // Configuration Controllers Panel
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1B20)), // Theme container
            border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.6f))
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                item {
                    Text(
                        text = "Orbital Mathematical Sandbox",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Simulate real hydrogen-like wavefunctions |Ψ|² probability densities.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Orbital subshell selector buttons
                item {
                    Text("Select Orbital Subshell", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            SubshellBtn("s (Spherical)", "s"),
                            SubshellBtn("p (Dumbbell)", "p"),
                            SubshellBtn("d (Double Dumbbell)", "d"),
                            SubshellBtn("sp³ (Hybrid VSEPR)", "sp3")
                        ).forEach { item ->
                            Button(
                                onClick = {
                                    viewModel.selectedOrbitalType.value = item.key
                                    viewModel.updateOrbitalAtoms()
                                },
                                mod = Modifier.weight(1f).testTag("subshell_${item.key}"),
                                isSelected = type == item.key
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Wavefunction descriptions
                item {
                    val desc = when (type) {
                        "s" -> "S Orbitals: Spherically symmetrical, coordinate l=0. Possesses no nodal planes. Radial density peaks closest to nucleus."
                        "p" -> "P Orbitals (2p_z): Lobe geometry, l=1 with a spatial node at the XY coordinate plane. Distinct wave phases (Red/Cyan) denote phase sign switch (+/-)."
                        "d" -> "D Orbitals (3d_z²): l=2. Includes a central toroid donut belt around equatorial plane and axial dumbbell elongation."
                        "sp3" -> "sp³ Hybrid Methane: Linear combination mixing one s and three p wave-functions outputs four directed, highly sharp lobes pointing in tetrahedral angles (109.5°)."
                        else -> ""
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)), // Geometric balance theme grey card
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(desc, color = Color.LightGray, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Aufbau Principle filling visualization
                item {
                    AufbauFillingSimulator()
                }
            }
        }
    }
}

/**
 * Animated Aufbau Principle box diagram.
 */
@Composable
fun AufbauFillingSimulator() {
    var filledElectrons by remember { mutableStateOf(2) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2B2930), RoundedCornerShape(12.dp)) // Theme subcard background
            .border(width = 1.dp, color = Color(0xFF49454F).copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Aufbau Subshell Filling", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Hund's Rule & Pauli Exclusion Principle", color = Color.Gray, fontSize = 10.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (filledElectrons > 0) filledElectrons-- }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White)
                }
                Text("$filledElectrons e⁻", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                IconButton(onClick = { if (filledElectrons < 10) filledElectrons++ }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Energy levels layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 1s orbital box
            OrbitalBox(
                label = "1s",
                electrons = filledElectrons.coerceIn(0, 2)
            )

            // 2s orbital box
            OrbitalBox(
                label = "2s",
                electrons = (filledElectrons - 2).coerceIn(0, 2)
            )

            // 2p subshells (three boxes)
            val pElectrons = (filledElectrons - 4).coerceAtLeast(0)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Apply Hund's rule: fill singly first, then pair
                val p1 = if (pElectrons >= 4) 2 else if (pElectrons >= 1) 1 else 0
                val p2 = if (pElectrons >= 5) 2 else if (pElectrons >= 2) 1 else 0
                val p3 = if (pElectrons >= 6) 2 else if (pElectrons >= 3) 1 else 0

                OrbitalBox(label = "2p_x", electrons = p1)
                OrbitalBox(label = "2p_y", electrons = p2)
                OrbitalBox(label = "2p_z", electrons = p3)
            }
        }
    }
}

@Composable
fun OrbitalBox(label: String, electrons: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(45.dp)
                .background(Color(0xFF111318), RoundedCornerShape(4.dp))
                .border(width = 1.dp, color = Color(0xFFD0BCFF).copy(alpha = 0.4f), shape = RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (electrons >= 1) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Spin Up", tint = Color(0xFFD0BCFF), modifier = Modifier.size(18.dp))
                }
                if (electrons >= 2) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Spin Down", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

/**
 * MODULE 2: Organic Reaction Mechanism Media Player.
 */
@Composable
fun ReactionMechanismView(viewModel: ChemistryViewModel) {
    val selected by viewModel.selectedMechanism.collectAsState()
    val isPlaying by viewModel.isPlayingMechanism.collectAsState()
    val progress by viewModel.mechanismProgress.collectAsState()

    val atoms by viewModel.activeAtoms.collectAsState()
    val bonds by viewModel.activeBonds.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxWidth()
                .background(Color(0xFF111318))
        ) {
            // Draw interactive Molecular deformity Mechanism frame
            Chemistry3DCanvas(
                atoms = atoms,
                bonds = bonds
            )

            // Dynamic float Walden umbrella inversion labels
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                colors = CardDefaults.cardColors(containerColor = Color(0xCE1D1B20)),
                border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Dynamic Walden Umbrella deformation",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text("• Transition State: ${(progress * 100).toInt()}% completed", color = Color.Gray, fontSize = 9.sp)
                    if (progress > 0.45f && progress < 0.55f) {
                        Text("• PLANAR SP² INTERMEDIATE STRETCH!", color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        }

        // Controlling seek Timeline
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1B20)),
            border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Mechanism type selection tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("SN2", "E2", "Aldol").forEach { option ->
                        Button(
                            onClick = {
                                viewModel.selectedMechanism.value = option
                                viewModel.mechanismProgress.value = 0f
                                viewModel.updateMechanismCoordinates(option, 0f)
                            },
                            mod = Modifier.weight(1f).testTag("mech_$option"),
                            isSelected = selected == option
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Playback timeline controller deck
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.mechanismProgress.value = 0f }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Restart", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    FloatingActionButton(
                        onClick = { viewModel.isPlayingMechanism.value = !isPlaying },
                        containerColor = Color(0xFFD0BCFF),
                        modifier = Modifier.size(52.dp).testTag("play_pause_fab")
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color(0xFF381E72)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(onClick = { viewModel.mechanismProgress.value = 1.0f }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Skip End", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrub slider timeline
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("0%", color = Color.Gray, fontSize = 10.sp)
                    Slider(
                        value = progress,
                        onValueChange = {
                            viewModel.isPlayingMechanism.value = false
                            viewModel.mechanismProgress.value = it
                            viewModel.updateMechanismCoordinates(selected, it)
                        },
                        modifier = Modifier.weight(1f).testTag("timeline_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFD0BCFF),
                            activeTrackColor = Color(0xFFD0BCFF)
                        )
                    )
                    Text("100%", color = Color.Gray, fontSize = 10.sp)
                }

                // Mathematical organic mechanism notes
                Text(
                    text = "Mechanism Pedagogic explanation:",
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )

                val infoText = when (selected) {
                    "SN2" -> "S_N2 Bimolecular Nucleophilic Substitution: Hydroxide or Chloride backside attack initiates planar compression of three Hydrogens. Forms a temporary pentavalent transition-state followed by umbrella inversion as Bromide leaves."
                    "E2" -> "E2 Bimolecular Elimination: Strongly coordinated base abstracts an alpha carbon Hydrogen while electrons collapse to generate a Carbon=Carbon double bond, initiating bromide leaving group depart synchronously."
                    "Aldol" -> "Aldol Reaction pathway: Enolate nucleophile attacks carbonyl positive electrophilic carbon center, elongating carbonyl C=O bonds and forming a new C-C skeleton connector."
                    else -> ""
                }
                Text(infoText, color = Color.Gray, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}

/**
 * MODULE 3: Thermodynamic Gas Laws Particle Simulator view.
 */
@Composable
fun GasLawsView(viewModel: ChemistryViewModel) {
    val temp by viewModel.gasTempKelvin.collectAsState()
    val pressure by viewModel.gasPressureAtm.collectAsState()
    val moles by viewModel.gasMoleCount.collectAsState()
    val equation by viewModel.gasEquationType.collectAsState()

    val particles by viewModel.gasParticles.collectAsState()

    // Calculated box boundaries
    val calculatedVolume = if (equation == "Ideal") {
        (moles * 0.0821f * temp) / pressure
    } else {
        val a = 1.36f
        val b = 0.0318f
        ((moles * 0.0821f * temp) / pressure) - (moles * b) + (a * moles / 10f)
    }
    
    val halfBound = (calculatedVolume * 6f).coerceIn(45f, 150f)

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxWidth()
                .background(Color(0xFF111318))
        ) {
            // Glowing kinetic bounding box canvas
            Chemistry3DCanvas(
                atoms = emptyList(),
                bonds = emptyList(),
                gasParticles = particles,
                gasBoxHalfWidth = halfBound
            )

            // Kinetic metrics dashboard
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                colors = CardDefaults.cardColors(containerColor = Color(0xCE1D1B20)),
                border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Kinetic Gas Properties", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("• Derived Volume: ${String.format("%.2f", calculatedVolume)} Liters", color = Color(0xFFD0BCFF), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    // Mean velocity equation representation: v = sqrt(3RT/M)
                    val r = 8.314f
                    val rmsVelocity = sqrt((3f * r * temp) / 0.004f) // helium molar mass 4g/mol
                    Text("• RMS speed (v_rms): ${rmsVelocity.toInt()} m/s", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }

        // Controllers Panel
        Card(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1B20)),
            border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.6f))
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PV=nRT Thermal Engine", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        
                        // Thermodynamics equation toggle ideal vs Van Der Waals
                        Row {
                            Button(
                                onClick = { viewModel.gasEquationType.value = "Ideal" },
                                isSelected = equation == "Ideal",
                                mod = Modifier.height(30.dp).padding(end = 4.dp)
                            )
                            Button(
                                onClick = { viewModel.gasEquationType.value = "VanDerWaals" },
                                isSelected = equation == "VanDerWaals",
                                mod = Modifier.height(30.dp)
                            )
                        }
                    }
                    Text(
                        text = if (equation == "Ideal") "Ideal: PV = nRT" else "Real: (P + a(n/V)²)(V - nb) = nRT",
                        color = Color(0xFFD0BCFF),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Controls: Temperature Slider
                item {
                    Text("Temperature (T): ${temp.toInt()} Kelvin", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = temp,
                        onValueChange = { viewModel.gasTempKelvin.value = it },
                        valueRange = 100f..1000f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFD0BCFF), activeTrackColor = Color(0xFFD0BCFF))
                    )
                }

                // Controls: Pressure Slider
                item {
                    Text("Pressure (P): ${String.format("%.1f", pressure)} atm", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = pressure,
                        onValueChange = { viewModel.gasPressureAtm.value = it },
                        valueRange = 0.5f..10f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFD0BCFF), activeTrackColor = Color(0xFFD0BCFF))
                    )
                }

                // Controls: mole count
                item {
                    Text("Moles (n): ${String.format("%.1f", moles)} mol", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = moles,
                        onValueChange = { viewModel.gasMoleCount.value = it },
                        valueRange = 1f..5f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFD0BCFF), activeTrackColor = Color(0xFFD0BCFF))
                    )
                }
            }
        }
    }
}

/**
 * MODULE 4: Electrochemistry View.
 */
@Composable
fun ElectrochemistryView(viewModel: ChemistryViewModel) {
    val cellMode by viewModel.selectedElectroCell.collectAsState()
    val progress by viewModel.electroProgressSec.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxWidth()
                .background(Color(0xFF111318))
        ) {
            // Interactive Electro cell simulator canvas
            Chemistry3DCanvas(
                atoms = emptyList(),
                bonds = emptyList(),
                electroMode = cellMode,
                electroElectronFlowSec = progress
            )

            // Voltage and physical descriptions details card
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                colors = CardDefaults.cardColors(containerColor = Color(0xCE1D1B20)),
                border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val label = if (cellMode == "Galvanic") "Zn/Cu Galvanic Cell" else "H₂O Electrolyzer"
                    val volt = if (cellMode == "Galvanic") "Potential: +1.10 Volts (Spontaneous)" else "Turgid potential: -1.23 Volts (Forced)"
                    Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(volt, color = Color(0xFFD0BCFF), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    if (cellMode == "Galvanic") {
                        Text("• Anode: Zn → Zn²⁺ + 2e⁻ (Oxidation)", color = Color.Gray, fontSize = 10.sp)
                        Text("• Cathode: Cu²⁺ + 2e⁻ → Cu (Reduction)", color = Color.Gray, fontSize = 10.sp)
                    } else {
                        Text("• Cathode: 2H₂O + 2e⁻ → H₂ + 2OH⁻", color = Color.Gray, fontSize = 10.sp)
                    }
                }
            }
        }

        // Cell Controller Panel
        Card(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1B20)),
            border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Cell Configuration Selector", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.selectedElectroCell.value = "Galvanic" },
                        mod = Modifier.weight(1f).testTag("cell_galvanic"),
                        isSelected = cellMode == "Galvanic"
                    )
                    Button(
                        onClick = { viewModel.selectedElectroCell.value = "Electrolytic" },
                        mod = Modifier.weight(1f).testTag("cell_electrolytic"),
                        isSelected = cellMode == "Electrolytic"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Theory and chemical reactions:", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                val theoryDesc = if (cellMode == "Galvanic") {
                    "Galvanic Cell demonstrates spontaneous redox chemical energy conversion to electric power. Sulfate ions migrate inside salt bridge tube, counterbalancing progressive positive charge accumulated in anode Zinc beaker."
                } else {
                    "Electrolytic Cells utilize external electricity boundaries to drive non-spontaneous reactions. Applying bias voltage over carbon plates decomposes water molecules, liberating hydrogen bubbles at negative terminal."
                }
                Text(theoryDesc, color = Color.LightGray, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/**
 * MODULE 5: Full Periodic Table chemical database explorer.
 */
@Composable
fun PeriodicTableView() {
    var selectedElement by remember { mutableStateOf<ChemicalElement?>(PeriodicTable.getByNum(6)) }

    Row(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Left Column periodic table grid
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight()
                .padding(end = 12.dp)
        ) {
            Text("Complete HSC Element Database", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Click an element to project analytical parameters", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(bottom = 10.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(PeriodicTable.elementsList) { element ->
                    val isSelected = selectedElement?.atomicNumber == element.atomicNumber
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(
                                color = if (isSelected) Color(0xFFD0BCFF).copy(alpha = 0.25f) else Color(0xFF1D1B20),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.2.dp,
                                color = if (isSelected) Color(0xFFD0BCFF) else element.color.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedElement = element }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(element.symbol, color = element.color, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            Text(element.atomicNumber.toString(), color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Right Column side panel detail card
        Card(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1B20)),
            border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.6f))
        ) {
            selectedElement?.let { element ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Element visual shield badge
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(element.color.copy(alpha = 0.15f))
                            .border(2.dp, element.color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(element.symbol, color = element.color, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(element.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Electrons Configuration: ${element.shellConfigurationString}", color = Color(0xFFD0BCFF), fontSize = 11.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color(0xFF49454F).copy(alpha = 0.5f))

                    // Detail properties table
                    PropRow("Atomic Mass", "${element.mass} u")
                    PropRow("Atomic Number", "${element.atomicNumber}")
                    PropRow("Valence e⁻", "${element.valenceElectrons}")
                    PropRow("Group Index", "${element.Group}")
                    PropRow("Period Index", "${element.Period}")
                    PropRow("Electronegativity", "${element.electronegativity}")
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("CPK Convention Visual Code:", color = Color.Gray, fontSize = 10.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                Box(modifier = Modifier.size(12.dp).background(element.color, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Element Color matches ${element.colorHex}", color = Color.LightGray, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PropRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Standard utility styled button configuration helper.
 */
@Composable
fun Button(
    onClick: () -> Unit,
    isSelected: Boolean,
    mod: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit = {}
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF2B2930),
            contentColor = if (isSelected) Color(0xFF381E72) else Color.White
        ),
        modifier = mod.clip(RoundedCornerShape(8.dp)),
        content = content
    )
}

@Composable
fun Button(
    onClick: () -> Unit,
    isSelected: Boolean,
    text: String,
    mod: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        isSelected = isSelected,
        mod = mod
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

data class NavigationItem(val title: String, val id: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
data class SubshellBtn(val label: String, val key: String)
