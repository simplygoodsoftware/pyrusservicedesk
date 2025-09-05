import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.Migration3to4
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.COMMANDS_TABLE
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.COMMENTS_TABLE
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.TICKETS_TABLE
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SdDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate3To4() {
        var db = helper.createDatabase(TEST_DB, 3)

        db.insertValue(COMMENTS_TABLE, ::putCommandsV3Values)
        db.insertValue(TICKETS_TABLE, ::putTicketsV3Values)

        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, Migration3to4())

        val ticketsCursor = db.query("SELECT * FROM $TICKETS_TABLE")
        val commandsCursor = db.query("SELECT * FROM $COMMANDS_TABLE")

        ticketsCursor.moveToNext()
        commandsCursor.moveToNext()

        ticketsCursor.getColumnIndexOrThrow("rating_settings_size")
        ticketsCursor.getColumnIndexOrThrow("rating_settings_type")
        ticketsCursor.getColumnIndexOrThrow("rating_settings_rating_text_values")

        ticketsCursor.getColumnIndexOrThrow("welcome_message")

        commandsCursor.getColumnIndexOrThrow("user_id")
        commandsCursor.getColumnIndexOrThrow("rating_comment")

        val ratingSettingsSizeIndex = ticketsCursor.getColumnIndexOrThrow("rating_settings_size")
        val ratingSettingsSizeValue = ticketsCursor.getInt(ratingSettingsSizeIndex)
        val ratingSettingsTypeIndex = ticketsCursor.getColumnIndexOrThrow("rating_settings_type")
        val ratingSettingsTypeValue = ticketsCursor.getInt(ratingSettingsTypeIndex)
        val ratingSettingsRatingTextValuesIndex = ticketsCursor.getColumnIndexOrThrow("rating_settings_rating_text_values")
        val ratingSettingsRatingTextValuesValue = ticketsCursor.getInt(ratingSettingsRatingTextValuesIndex)
        val welcomeMessageIndex = ticketsCursor.getColumnIndexOrThrow("welcome_message")
        val welcomeMessageValue = ticketsCursor.getInt(welcomeMessageIndex)
        val ratingCommentIndex = commandsCursor.getColumnIndexOrThrow("rating_comment")
        val ratingCommentValue = commandsCursor.getInt(ratingCommentIndex)
        val userIdIndex = commandsCursor.getColumnIndexOrThrow("user_id")
        val userIdValue = commandsCursor.getInt(userIdIndex)
        assertEquals(null, ratingSettingsSizeValue)
        assertEquals(null, ratingSettingsTypeValue)
        assertEquals(null, ratingSettingsRatingTextValuesValue)
        assertEquals(null, welcomeMessageValue)
        assertEquals(null, ratingCommentValue)
        assertEquals("543454", userIdValue)

    }
    private fun SupportSQLiteDatabase.insertValue(table: String, vararg stackers: (values: ContentValues) -> Unit) {
        val personContentValues = ContentValues()
        for (stacker in stackers) {
            stacker(personContentValues)
        }
        insert(table, SQLiteDatabase.CONFLICT_REPLACE, personContentValues)
    }

    private fun putCommentsV3Values(contentValues: ContentValues) {
        contentValues.put("comment_id", 1L)
        contentValues.put("ticket_id", 11111111L)
        contentValues.put("body", "Pop")
        contentValues.put("unescaped_body", "test")
        contentValues.put("is_inbound", true)
        contentValues.put("created_at", 1757052450L)
        contentValues.put("rating", 5)
        contentValues.put("author_name", "Kate")
        contentValues.put("author_id", "hngvgf152")
        contentValues.put("author_avatar_id", 13)
        contentValues.put("author_avatar_color", "color")
    }

    private fun putCommandsV3Values(contentValues: ContentValues) {
        contentValues.put("is_error", false)
        contentValues.put("local_id", 5L)
        contentValues.put("command_id", "jhgkjgkjhg")
        contentValues.put("command_type", 1)
        contentValues.put("user_id", "543454")
        contentValues.put("app_id", "app_idapp_idapp_idapp_idapp_idapp_idapp_id")
        contentValues.put("creation_time", 1757052450L)
        contentValues.put("request_new_ticket", false)
        contentValues.put("comment", "hngvgf152")
        contentValues.put("ticket_id", 123456789L)
        contentValues.put("rating", 5)
        contentValues.put("comment_id", 44565457L)
        contentValues.put("token", "token7676")
        contentValues.put("token_type", "token_type")
    }

    private fun putTicketsV3Values(values: ContentValues) {

        values.put("ticket_id", 144544444L)
        values.put("user_id", "15436532")
        values.put("subject", "subject")
        values.put("unescaped_subject", "unescaped_subject")
        values.put("author", "Kate")
        values.put("is_read", true)
        values.put("last_comment_comment_id", 1345223L)
        values.put("last_comment_created_at", 1757052450L)
        values.put("last_comment_author_name", "Kate")
        values.put("last_comment_author_id", "hngvgf152")
        values.put("last_comment_author_avatar_id", 13)
        values.put("last_comment_author_avatar_color", "color")
        values.put("last_comment_body", "body")
        values.put("last_comment_last_attachment_name", "1.png")
        values.put("is_active", true)
        values.put("created_at", 1757052450L)
        values.put("show_rating", true)
    }

    companion object {
        private const val TEST_DB = "test-db"
    }


}