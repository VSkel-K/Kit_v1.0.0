// CortexDatabase.kt — Room Database singleton
package com.dreiz.kit.data.local.cortex

import android.content.Context
import androidx.room.*

@Database(entities = [GenomeEntity::class], version = 1, exportSchema = false)
abstract class CortexDatabase : RoomDatabase() {
    abstract fun cortexDao(): CortexDao
    companion object {
        @Volatile private var INSTANCE: CortexDatabase? = null
        fun getDatabase(context: Context): CortexDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, CortexDatabase::class.java, "cortex_sanctuary_db")
                    .build().also { INSTANCE = it }
            }
    }
}
