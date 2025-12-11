package com.pyrus.pyrusservicedesk.sdk.repositories.data_base

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pyrus.pyrusservicedesk.sdk.Converter
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.APPLICATIONS_TABLE
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.COMMANDS_TABLE
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.COMMENTS_TABLE
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.MEMBERS_TABLE
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.TICKETS_TABLE
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.dao.CommandsDao
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.dao.SearchDao
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.dao.TicketsDao
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.ApplicationEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.AttachmentEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.CommandEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.CommentEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.LocalAttachmentEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.MemberEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.TicketEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.UserEntity

@Database(
    entities = [
        ApplicationEntity::class,
        UserEntity::class,
        TicketEntity::class,
        CommentEntity::class,
        AttachmentEntity::class,
        CommandEntity::class,
        LocalAttachmentEntity::class,
        MemberEntity::class,
    ],
    exportSchema = false,
    version = 6
)
@TypeConverters(Converter::class)
internal abstract class SdDatabase : RoomDatabase() {

    abstract fun ticketsDao(): TicketsDao

    abstract fun commandsDao(): CommandsDao

    abstract fun searchDao(): SearchDao

    internal companion object {

        const val APPLICATIONS_TABLE = "applications_table"
        const val USERS_TABLE = "users_table"
        const val TICKETS_TABLE = "tickets_table"
        const val COMMENTS_TABLE = "comments_table"
        const val ATTACHMENTS_TABLE = "attachment_table"
        const val COMMANDS_TABLE = "commands_table"
        const val LOCAL_ATTACHMENTS_TABLE = "local_attachment_table"
        const val MEMBERS_TABLE = "members_table"

        fun create(appContext: Context): SdDatabase {
            return Room.databaseBuilder(
                appContext,
                SdDatabase::class.java,
                "sd_database"
            ).addMigrations(
                Migration1to2(),
                Migration2to3(),
                Migration3to4(),
                Migration4to5(),
                Migration5to6(),
            ).build()
        }

    }

}

private class Migration1to2: Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE $COMMENTS_TABLE ADD COLUMN unescaped_body TEXT")
        db.execSQL("ALTER TABLE $TICKETS_TABLE ADD COLUMN unescaped_subject TEXT DEFAULT '' NOT NULL")
    }
}

private class Migration2to3: Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE $APPLICATIONS_TABLE ADD COLUMN org_description TEXT")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $MEMBERS_TABLE (
                `author_id` TEXT NOT NULL,
                `has_access` INTEGER NOT NULL,
                `id` TEXT NOT NULL,
                `name` TEXT,
                `phone` TEXT,
                `user_id` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """
        )

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_members_table_user_id` 
            ON $MEMBERS_TABLE (`user_id`)
        """)

    }
}

internal class Migration3to4: Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL("ALTER TABLE $COMMANDS_TABLE ADD COLUMN user_id_new TEXT")
        db.execSQL("UPDATE $COMMANDS_TABLE SET user_id_new = user_id")
        db.execSQL("ALTER TABLE $COMMANDS_TABLE DROP COLUMN user_id")
        db.execSQL("ALTER TABLE $COMMANDS_TABLE RENAME COLUMN user_id_new TO user_id")

        db.execSQL("ALTER TABLE $APPLICATIONS_TABLE ADD COLUMN rating_settings_size INTEGER")
        db.execSQL("ALTER TABLE $APPLICATIONS_TABLE ADD COLUMN rating_settings_type INTEGER")
        db.execSQL("ALTER TABLE $APPLICATIONS_TABLE ADD COLUMN rating_settings_rating_text_values TEXT")

        db.execSQL("ALTER TABLE $COMMANDS_TABLE ADD COLUMN rating_comment TEXT")
        db.execSQL("ALTER TABLE $APPLICATIONS_TABLE ADD COLUMN welcome_message TEXT")
    }
}

internal class Migration4to5: Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE $COMMANDS_TABLE ADD COLUMN extra_fields TEXT")
    }
}

internal class Migration5to6: Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE $COMMENTS_TABLE ADD COLUMN is_system INTEGER NOT NULL DEFAULT 0")
    }
}