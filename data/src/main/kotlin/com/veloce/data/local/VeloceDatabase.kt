package com.veloce.data.local
import android.content.Context
import androidx.room.*

@Database(entities = [LocationPointEntity::class, SessionEntity::class], version = 1, exportSchema = true)
abstract class VeloceDatabase : RoomDatabase() {
    abstract fun trackingDao(): TrackingDao
    companion object {
        fun create(context: Context): VeloceDatabase = Room.databaseBuilder(
            context.applicationContext, VeloceDatabase::class.java, "veloce.db",
        ).setJournalMode(JournalMode.WRITE_AHEAD_LOGGING).build()
    }
}
