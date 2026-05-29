package com.example.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// 1. ENTITIES

@Entity(tableName = "user_sessions")
data class UserSession(
    @PrimaryKey val phone: String,
    val name: String,
    val avatarUrl: String,
    val publicKey: String,
    val privateKey: String,
    val loginTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey val phone: String,
    val name: String,
    val avatarColorHex: String,
    val statusMessage: String,
    val publicKey: String,
    val isBot: Boolean = true,
    val unreadCount: Int = 0
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderPhone: String,
    val receiverPhone: String,
    val ciphertext: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "READ", // SENT, DELIVERED, READ
    val isEncrypted: Boolean = true
)

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactPhone: String,
    val contactName: String,
    val contactAvatarColor: String,
    val callType: String, // "AUDIO" or "VIDEO"
    val isIncoming: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0, // 0 for missed
    val status: String = "CONNECTED" // MISSED, REJECTED, CONNECTED
)

@Entity(tableName = "statuses")
data class StatusUpdate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactPhone: String,
    val contactName: String,
    val contactAvatarColor: String,
    val text: String,
    val backgroundColorHex: String,
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val viewCount: Int = 0
)

// 2. DAOS

@Dao
interface UserSessionDao {
    @Query("SELECT * FROM user_sessions LIMIT 1")
    fun getActiveSessionFlow(): Flow<UserSession?>

    @Query("SELECT * FROM user_sessions LIMIT 1")
    suspend fun getActiveSession(): UserSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: UserSession)

    @Query("DELETE FROM user_sessions")
    suspend fun clearSession()
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts")
    fun getAllContactsFlow(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts")
    suspend fun getAllContacts(): List<Contact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Contact>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Query("SELECT * FROM contacts WHERE phone = :phone LIMIT 1")
    suspend fun getContactByPhone(phone: String): Contact?
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE (senderPhone = :p1 AND receiverPhone = :p2) OR (senderPhone = :p2 AND receiverPhone = :p1) ORDER BY timestamp ASC")
    fun getMessagesForChatFlow(p1: String, p2: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogsFlow(): Flow<List<CallLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(log: CallLog)

    @Query("DELETE FROM call_logs")
    suspend fun clearCallLogs()
}

@Dao
interface StatusDao {
    @Query("SELECT * FROM statuses ORDER BY timestamp DESC")
    fun getAllStatusesFlow(): Flow<List<StatusUpdate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: StatusUpdate)

    @Query("DELETE FROM statuses WHERE timestamp < :expirationTime")
    suspend fun deleteExpiredStatuses(expirationTime: Long)
}

// 3. DATABASE

@Database(
    entities = [UserSession::class, Contact::class, Message::class, CallLog::class, StatusUpdate::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userSessionDao(): UserSessionDao
    abstract fun contactDao(): ContactDao
    abstract fun messageDao(): MessageDao
    abstract fun callLogDao(): CallLogDao
    abstract fun statusDao(): StatusDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cipherwire_db"
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Populate default interactive contacts and simulation data using Coroutines
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialContacts(database.contactDao())
                        populateInitialStatuses(database.statusDao())
                        populateInitialCallLogs(database.callLogDao())
                    }
                }
            }

            private suspend fun populateInitialContacts(contactDao: ContactDao) {
                val initialContacts = listOf(
                    Contact(
                        phone = "+12025550143",
                        name = "Alice [Aegis Security]",
                        avatarColorHex = "#0D9488", // Teal
                        statusMessage = "End-to-End Encrypted channels certified.",
                        publicKey = "CIPHER_WIRE_ALICE_RSA2048_PUB_KEY"
                    ),
                    Contact(
                        phone = "+14155552671",
                        name = "Bob [Morpheus]",
                        avatarColorHex = "#6366F1", // Indigo
                        statusMessage = "Free your mind, ciphertext is reality.",
                        publicKey = "CIPHER_WIRE_BOB_RSA2048_PUB_KEY"
                    ),
                    Contact(
                        phone = "+13125559021",
                        name = "Clara (E2EE Explorer)",
                        avatarColorHex = "#EC4899", // Pink
                        statusMessage = "I love exploring privacy applications! ✨",
                        publicKey = "CIPHER_WIRE_CLARA_RSA2048_PUB_KEY"
                    ),
                    Contact(
                        phone = "+18005553224",
                        name = "CipherWire DevBot",
                        avatarColorHex = "#10B981", // Emerald
                        statusMessage = "Tap to verify secure keys.",
                        publicKey = "CIPHER_WIRE_DEVBOT_RSA2048_PUB_KEY"
                    )
                )
                contactDao.insertContacts(initialContacts)
            }

            private suspend fun populateInitialStatuses(statusDao: StatusDao) {
                val now = System.currentTimeMillis()
                val statuses = listOf(
                    StatusUpdate(
                        contactPhone = "+12025550143",
                        contactName = "Alice [Aegis Security]",
                        contactAvatarColor = "#0D9488",
                        text = "🔐 All CipherWire communication is end-to-end encrypted. Tap to verify cryptographic handshake in chat settings.",
                        backgroundColorHex = "#1E293B", // Slate Dark
                        timestamp = now - 3600000,
                        viewCount = 14
                    ),
                    StatusUpdate(
                        contactPhone = "+14155552671",
                        contactName = "Bob [Morpheus]",
                        contactAvatarColor = "#6366F1",
                        text = "Do you want the blue pill, or the red pill? The ciphertext is beautiful.",
                        backgroundColorHex = "#0F172A", // Slate Darker
                        timestamp = now - 7200000,
                        viewCount = 28
                    )
                )
                statuses.forEach { statusDao.insertStatus(it) }
            }

            private suspend fun populateInitialCallLogs(callLogDao: CallLogDao) {
                val now = System.currentTimeMillis()
                val logs = listOf(
                    CallLog(
                        contactPhone = "+12025550143",
                        contactName = "Alice [Aegis Security]",
                        contactAvatarColor = "#0D9488",
                        callType = "VIDEO",
                        isIncoming = true,
                        timestamp = now - 72000000,
                        durationSeconds = 215,
                        status = "CONNECTED"
                    ),
                    CallLog(
                        contactPhone = "+14155552671",
                        contactName = "Bob [Morpheus]",
                        contactAvatarColor = "#6366F1",
                        callType = "AUDIO",
                        isIncoming = false,
                        timestamp = now - 144000000,
                        durationSeconds = 87,
                        status = "CONNECTED"
                    ),
                    CallLog(
                        contactPhone = "+13125559021",
                        contactName = "Clara (E2EE Explorer)",
                        contactAvatarColor = "#EC4899",
                        callType = "AUDIO",
                        isIncoming = true,
                        timestamp = now - 216000000,
                        durationSeconds = 0,
                        status = "MISSED"
                    )
                )
                logs.forEach { callLogDao.insertCallLog(it) }
            }
        }
    }
}
