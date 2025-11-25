package com.pyrus.pyrusservicedesk._ref.data

/**
 * Represents ticket object.
 * @param name author name.
 * @param authorId author id (phone number hash).
 * @param avatarUrl author avatar url.
 * @param avatarColor author avatar color.
 */

data class Author(
    val isUser: Boolean,
    val name: String?,
    val authorId: String?,
    val avatarUrl: String?,
    val avatarColor: String?,
)