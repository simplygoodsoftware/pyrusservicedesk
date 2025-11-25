import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.Migration3to4
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.Migration4to5
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase
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

        db.insertValue(SdDatabase.COMMANDS_TABLE, ::putCommandsV3Values)
        db.insertValue(SdDatabase.APPLICATIONS_TABLE, ::putApplicationsV3Values)

        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, Migration3to4())

        val applicationCursor = db.query("SELECT * FROM ${SdDatabase.APPLICATIONS_TABLE}")
        val commandsCursor = db.query("SELECT * FROM ${SdDatabase.COMMANDS_TABLE}")

        applicationCursor.moveToNext()
        commandsCursor.moveToNext()

        applicationCursor.getColumnIndexOrThrow("rating_settings_size")
        applicationCursor.getColumnIndexOrThrow("rating_settings_type")
        applicationCursor.getColumnIndexOrThrow("rating_settings_rating_text_values")

        applicationCursor.getColumnIndexOrThrow("welcome_message")

        commandsCursor.getColumnIndexOrThrow("user_id")
        commandsCursor.getColumnIndexOrThrow("rating_comment")

        val ratingSettingsSizeIndex = applicationCursor.getColumnIndexOrThrow("rating_settings_size")
        val ratingSettingsSizeValue = applicationCursor.getIntOrNull(ratingSettingsSizeIndex)
        val ratingSettingsTypeIndex = applicationCursor.getColumnIndexOrThrow("rating_settings_type")
        val ratingSettingsTypeValue = applicationCursor.getIntOrNull(ratingSettingsTypeIndex)
        val ratingSettingsRatingTextValuesIndex = applicationCursor.getColumnIndexOrThrow("rating_settings_rating_text_values")
        val ratingSettingsRatingTextValuesValue = applicationCursor.getIntOrNull(ratingSettingsRatingTextValuesIndex)
        val welcomeMessageIndex = applicationCursor.getColumnIndexOrThrow("welcome_message")
        val welcomeMessageValue = applicationCursor.getStringOrNull(welcomeMessageIndex)
        val ratingCommentIndex = commandsCursor.getColumnIndexOrThrow("rating_comment")
        val ratingCommentValue = commandsCursor.getStringOrNull(ratingCommentIndex)
        val userIdIndex = commandsCursor.getColumnIndexOrThrow("user_id")
        val userIdValue = commandsCursor.getStringOrNull(userIdIndex)
        assertEquals(null, ratingSettingsSizeValue)
        assertEquals(null, ratingSettingsTypeValue)
        assertEquals(null, ratingSettingsRatingTextValuesValue)
        assertEquals(null, welcomeMessageValue)
        assertEquals(null, ratingCommentValue)
        assertEquals("543454", userIdValue)

    }

    @Test
    @Throws(IOException::class)
    fun migrate4To5() {
        var db = helper.createDatabase(TEST_DB, 4)

        db.insertValue(SdDatabase.COMMANDS_TABLE, ::putCommandsV3Values, ::putCommandsV4Values)

        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 5, true, Migration4to5())

        val commandsCursor = db.query("SELECT * FROM ${SdDatabase.COMMANDS_TABLE}")

        commandsCursor.moveToNext()

        commandsCursor.getColumnIndexOrThrow("extra_fields")

        val extraFieldsIndex = commandsCursor.getColumnIndexOrThrow("extra_fields")
        val extraFieldsValue = commandsCursor.getStringOrNull(extraFieldsIndex)

        assertEquals(null, extraFieldsValue)

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

    private fun putCommandsV4Values(contentValues: ContentValues) {
        contentValues.put("rating_comment", "comment")
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

    private fun putApplicationsV3Values(values: ContentValues) {
        values.put("app_id", "hgc87vjhbx98jhbv")
        values.put("org_name", "organisation")
        values.put("org_logo_url", "org_logo_url")
        values.put("org_description", "org_description/information")
    }

    companion object {
        private const val TEST_DB = "test-db"
    }


}