package com.pyrus.pyrusservicedesk._ref.data

/**
 * Represents ticket object.
 * @param name author name.
 * @param authorId author id (phone number hash).
 * @param avatarUrl author avatar url.
 * @param avatarColor author avatar color.
 */

internal data class Author(
    val name: String?,
    val authorId: String?,
    val avatarUrl: String?,
    val avatarColor: String?,
)