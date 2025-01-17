package com.pyrus.pyrusservicedesk._ref.utils

internal sealed interface GetTicketsError {
    class ServiceError(
        val title: String?,
        val description: String,
    ) : GetTicketsError

    data object ConnectionError: GetTicketsError

    data object NoDataFound: GetTicketsError

    data object AuthorAccessDenied: GetTicketsError

}