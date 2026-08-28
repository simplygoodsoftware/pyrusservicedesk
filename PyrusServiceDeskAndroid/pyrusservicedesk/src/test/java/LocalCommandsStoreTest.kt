import com.pyrus.pyrusservicedesk.sdk.repositories.IdStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalCommandsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.dao.CommandsDao
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.dao.SearchDao
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

internal class LocalCommandsStoreTest {

    private val commandsDao: CommandsDao = mockk(relaxed = true)

    private fun createStore() = LocalCommandsStore(
        idStore = mockk<IdStore>(relaxed = true),
        commandsDao = commandsDao,
        searchDao = mockk<SearchDao>(relaxed = true),
    )

    @Test
    fun shouldStartLocalIdsFromMinusOneOnTheEmptyStore() {
        every { commandsDao.getCommandMinLocalId() } returns null

        val store = createStore()

        assertEquals(listOf(-1L, -2L, -3L), List(3) { store.getNextLocalId() })
    }

    /**
     * A negative ticket id means that the ticket is local and is not created on the server yet,
     * see RepositoryMapper.mergeData, where such tickets are collected by `ticketId < 0`. Zero
     * belongs neither to the local ids nor to the server ones, so it must never be issued.
     */
    @Test
    fun shouldIssueOnlyNegativeIds() {
        for (minStoredId in listOf(null, 0L, -1L, -128L)) {
            every { commandsDao.getCommandMinLocalId() } returns minStoredId

            val ids = createStore().let { store -> List(5) { store.getNextLocalId() } }

            assertTrue("issued $ids for the stored min $minStoredId", ids.all { it < 0 })
        }
    }

    /**
     * The counter starts from zero on every launch, so it must be seeded from the store. Otherwise
     * the ids of the commands that are still waiting to be sent are issued again, and the new
     * command replaces the waiting one, because local_id is unique and the inserts replace on
     * conflict.
     */
    @Test
    fun shouldNotIssueLocalIdsThatAreAlreadyStored() {
        val storedIds = listOf(0L, -1L, -2L)
        every { commandsDao.getCommandMinLocalId() } returns storedIds.min()

        val store = createStore()
        val newIds = List(3) { store.getNextLocalId() }

        assertTrue(
            "issued $newIds, but $storedIds are already stored",
            newIds.none { it in storedIds },
        )
        assertEquals(listOf(-3L, -4L, -5L), newIds)
    }
}
