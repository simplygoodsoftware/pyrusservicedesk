package net.papirus.pyrusservicedesk.sdk

internal class RepositoryFactory {
    companion object {
        fun create(webRepository: Repository): Repository = CentralRepository(webRepository)
    }
}