package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudioDao {
    @Query("SELECT * FROM audio_tracks ORDER BY createdAt DESC")
    fun getAllTracks(): Flow<List<AudioTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: AudioTrackEntity): Long

    @Update
    suspend fun updateTrack(track: AudioTrackEntity)

    @Query("DELETE FROM audio_tracks WHERE id = :id")
    suspend fun deleteTrackById(id: Long)

    @Query("SELECT * FROM noise_profiles ORDER BY createdAt DESC")
    fun getAllNoiseProfiles(): Flow<List<NoiseProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoiseProfile(profile: NoiseProfileEntity): Long

    @Query("DELETE FROM noise_profiles WHERE id = :id")
    suspend fun deleteNoiseProfileById(id: Long)
}

@Database(
    entities = [AudioTrackEntity::class, NoiseProfileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StudioDatabase : RoomDatabase() {
    abstract fun studioDao(): StudioDao

    companion object {
        @Volatile
        private var INSTANCE: StudioDatabase? = null

        fun getInstance(context: Context): StudioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudioDatabase::class.java,
                    "sonicforge_studio.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class StudioRepository(private val dao: StudioDao) {
    val allTracks: Flow<List<AudioTrackEntity>> = dao.getAllTracks()
    val allNoiseProfiles: Flow<List<NoiseProfileEntity>> = dao.getAllNoiseProfiles()

    suspend fun saveTrack(track: AudioTrackEntity): Long = dao.insertTrack(track)
    suspend fun updateTrack(track: AudioTrackEntity) = dao.updateTrack(track)
    suspend fun deleteTrack(id: Long) = dao.deleteTrackById(id)

    suspend fun saveNoiseProfile(profile: NoiseProfileEntity): Long = dao.insertNoiseProfile(profile)
    suspend fun deleteNoiseProfile(id: Long) = dao.deleteNoiseProfileById(id)
}
