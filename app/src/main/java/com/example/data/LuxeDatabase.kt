package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "luxe_notifications")
data class LuxeNotification(
    @PrimaryKey val id: String,
    val recipientRole: String, // "CUSTOMER", "STORE_OWNER", "SUPER_ADMIN"
    val recipientEmail: String,
    val title: String,
    val message: String,
    val type: String, // Notification type tag (e.g. "ORDER_STATUS", "LOW_STOCK")
    val category: String, // "Orders", "Inquiries", "Systems", "Marketing"
    val timestamp: Long,
    val isRead: Int = 0, // 0 for false, 1 for true
    val targetScreen: String? = null // For navigation click-through
)

@Dao
interface LuxeNotificationDao {
    @Query("SELECT * FROM luxe_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<LuxeNotification>>

    @Query("SELECT * FROM luxe_notifications WHERE recipientRole = :role AND recipientEmail = :email ORDER BY timestamp DESC")
    fun getNotificationsForUser(role: String, email: String): Flow<List<LuxeNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: LuxeNotification)

    @Query("UPDATE luxe_notifications SET isRead = :isRead WHERE id = :id")
    suspend fun updateReadStatus(id: String, isRead: Int)

    @Query("UPDATE luxe_notifications SET isRead = 1 WHERE recipientRole = :role AND recipientEmail = :email")
    suspend fun markAllAsRead(role: String, email: String)

    @Query("DELETE FROM luxe_notifications WHERE id = :id")
    suspend fun deleteNotification(id: String)

    @Query("DELETE FROM luxe_notifications WHERE timestamp < :limitTimestamp")
    suspend fun deleteOlderThan(limitTimestamp: Long)
}

@Database(entities = [LuxeNotification::class], version = 1, exportSchema = false)
abstract class LuxeDatabase : RoomDatabase() {
    abstract fun notificationDao(): LuxeNotificationDao

    companion object {
        @Volatile
        private var INSTANCE: LuxeDatabase? = null

        fun getDatabase(context: Context): LuxeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LuxeDatabase::class.java,
                    "luxe_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
