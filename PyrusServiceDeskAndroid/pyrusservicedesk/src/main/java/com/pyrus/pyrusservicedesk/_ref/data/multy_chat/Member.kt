package com.pyrus.pyrusservicedesk._ref.data.multy_chat

data class Member(
    val authorId: String,
    val name: String?,
    val hasAccess: Boolean,
    val phone: String?,
)