package com.example.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_compounds")
data class SavedCompound(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val formula: String,
    val description: String,
    val structureJson: String, // Represents Atoms and Bonds serialized
    val isPreset: Boolean = false,
    val lastViewedTimestamp: Long = System.currentTimeMillis()
)

@Dao
interface ChemistryDao {
    @Query("SELECT * FROM saved_compounds ORDER BY lastViewedTimestamp DESC")
    fun getAllSavedCompounds(): Flow<List<SavedCompound>>

    @Query("SELECT * FROM saved_compounds WHERE isPreset = 1")
    fun getPresetCompounds(): Flow<List<SavedCompound>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompound(compound: SavedCompound)

    @Query("DELETE FROM saved_compounds WHERE id = :id")
    suspend fun deleteCompoundById(id: Int)

    @Query("SELECT COUNT(*) FROM saved_compounds")
    suspend fun getCount(): Int
}

@Database(entities = [SavedCompound::class], version = 1, exportSchema = false)
abstract class ChemistryDatabase : RoomDatabase() {
    abstract fun chemistryDao(): ChemistryDao

    companion object {
        @Volatile
        private var INSTANCE: ChemistryDatabase? = null

        fun getDatabase(context: Context): ChemistryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChemistryDatabase::class.java,
                    "chemistry_sim_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Repository implementation for clean access to SQLite stored compounds and structures.
 */
class ChemistryRepository(private val chemistryDao: ChemistryDao) {
    val allCompounds: Flow<List<SavedCompound>> = chemistryDao.getAllSavedCompounds()
    val presets: Flow<List<SavedCompound>> = chemistryDao.getPresetCompounds()

    suspend fun insert(compound: SavedCompound) {
        chemistryDao.insertCompound(compound)
    }

    suspend fun deleteById(id: Int) {
        chemistryDao.deleteCompoundById(id)
    }

    suspend fun ensureInitialPresets() {
        if (chemistryDao.getCount() == 0) {
            // Populate typical molecules
            listOf(
                SavedCompound(
                    name = "Methane",
                    formula = "CH₄",
                    description = "Tetrahedral organic compound illustrating sp³ hybridization and 109.5° bond angles.",
                    structureJson = "CH4",
                    isPreset = true
                ),
                SavedCompound(
                    name = "Ethylene",
                    formula = "C₂H₄",
                    description = "Planar organic molecule with carbon-carbon double bond, sp² hybridization and 120° bond angles.",
                    structureJson = "C2H4",
                    isPreset = true
                ),
                SavedCompound(
                    name = "Acetylene",
                    formula = "C₂H₂",
                    description = "Linear hydrocarbon containing a carbon-carbon triple bond with sp hybridization.",
                    structureJson = "C2H2",
                    isPreset = true
                ),
                SavedCompound(
                    name = "Sulfur Hexafluoride",
                    formula = "SF₆",
                    description = "Octahedral coordination molecule representing sp³d² hybridization and 90° axial/equatorial bonds.",
                    structureJson = "SF6",
                    isPreset = true
                )
            ).forEach { chemistryDao.insertCompound(it) }
        }
    }
}
