// CortexDao.kt — Room DAO para consultas de genomas
package com.dreiz.kit.data.local.cortex

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CortexDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGenome(genome: GenomeEntity)

    @Query("SELECT * FROM cortex_memoria ORDER BY timestamp DESC")
    fun getAllGenomes(): Flow<List<GenomeEntity>>

    @Query("SELECT * FROM cortex_memoria WHERE name = :name LIMIT 1")
    suspend fun getGenomeByName(name: String): GenomeEntity?

    @Delete
    suspend fun purgeGenome(genome: GenomeEntity)
}
