package lv.zole.core.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "deals")
data class DealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchId: String,
    val number: Int,
    val playedAt: Long,
    /** Contract name, or null when everyone passed (pule). */
    val contract: String?,
    /** Seat index of the declarer, or null. */
    val declarer: Int?,
    val outcome: String,
    val declarerPoints: Int,
    val delta0: Int,
    val delta1: Int,
    val delta2: Int,
)

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val difficulty: String,
    val deals: Int,
    val total0: Int,
    val total1: Int,
    val total2: Int,
)

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeal(deal: DealEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMatch(match: MatchEntity)

    @Query("SELECT * FROM deals ORDER BY playedAt ASC")
    fun deals(): Flow<List<DealEntity>>

    @Query("SELECT * FROM matches ORDER BY startedAt ASC")
    fun matches(): Flow<List<MatchEntity>>

    @Query("SELECT COUNT(*) FROM deals WHERE matchId = :matchId AND number = :number")
    suspend fun dealCount(matchId: String, number: Int): Int
}

@Database(entities = [DealEntity::class, MatchEntity::class], version = 1, exportSchema = false)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun history(): HistoryDao

    companion object {
        fun create(context: Context): HistoryDatabase =
            Room.databaseBuilder(context, HistoryDatabase::class.java, "history.db").build()
    }
}
