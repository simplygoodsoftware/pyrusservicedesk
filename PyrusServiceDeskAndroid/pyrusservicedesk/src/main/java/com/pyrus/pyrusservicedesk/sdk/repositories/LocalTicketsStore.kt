package com.pyrus.pyrusservicedesk.sdk.repositories

import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.DatabaseMapper
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.dao.SearchDao
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.dao.TicketsDao
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.ApplicationEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.MemberEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.TicketEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.ApplicationWithUsersEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.TicketWithComment
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.TicketWithComments
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine


internal class LocalTicketsStore(
    private val idStore: IdStore,
    private val ticketsDao: TicketsDao,
    private val searchDao: SearchDao,
    private val accountStore: AccountStore,
) {

    fun getApplications(): List<ApplicationEntity> {
        return ticketsDao.getApplications()
    }

    fun getMembersByUserId(userId: String): List<MemberEntity> {
        return ticketsDao.getMembers(userId)
    }

    fun getApplicationsWithTickets(): List<ApplicationWithUsersEntity> {
        return ticketsDao.getApplicationsWithUsers()
    }

    fun getUsersWithData() : List<User> {
        return ticketsDao.getUsers().map { User(it.userId, it.appId, it.userName) }
    }

    fun getApplicationsWithTicketsFlow(): Flow<List<ApplicationWithUsersEntity>> {
        return ticketsDao.getApplicationsFlow()
    }

    fun getTickets(): List<TicketEntity> {
        return ticketsDao.getTickets()
    }

    fun getTicketsFlow(): Flow<List<TicketEntity>> {
        return ticketsDao.getTicketsFlow()
    }

    fun getTicketsWithComments(): List<TicketWithComments> {
        return ticketsDao.getTicketsWithComments()
    }

    fun getTicketWithComments(ticketId: Long): TicketWithComments? {
        val serverTicketId = idStore.getTicketServerId(ticketId) ?: ticketId
        if (serverTicketId <= 0) return null
        return ticketsDao.getTicketWithComments(ticketId)
    }

    fun getTicketWithCommentsFlow(ticketId: Long): Flow<TicketWithComments?> = combine(
        idStore.getTicketServerIdFlow(ticketId),
        ticketsDao.getTicketWithCommentsFlow(ticketId)
    ) { serverId, ticket ->
        if (ticket?.ticket?.ticketId == serverId) {
            ticket
        }
        else {
            ticketsDao.getTicketWithComments(serverId)
        }
    }

    fun storeServerState(users: List<User>, dto: TicketsDto) {

        val applications = dto.applications?.mapNotNull(DatabaseMapper::mapToApplicationEntity)?.distinctBy { it.appId } ?: emptyList()
        val tickets = dto.tickets?.mapNotNull {DatabaseMapper.mapToTicketWithComments(it, accountStore.getAccount())} ?: emptyList()
        val userEntities = users.map(DatabaseMapper::mapToUserEntity)
        val members: MutableList<MemberEntity> = mutableListOf()
        dto.applications?.forEach { application -> application.authorsInfo?.forEach { userMap -> members.addAll(userMap.value.map { DatabaseMapper.mapToMembersEntity(it, userMap.key) }) } }

        ticketsDao.insert(tickets, applications, userEntities, members)
    }

    fun searchTickets(text: String, limit: Int): List<TicketWithComment> {
        return searchDao.searchTicketsWithComment(text, limit)
    }

}