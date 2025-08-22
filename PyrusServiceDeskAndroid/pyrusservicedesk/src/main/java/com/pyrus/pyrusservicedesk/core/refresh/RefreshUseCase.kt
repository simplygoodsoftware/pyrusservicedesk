package com.pyrus.pyrusservicedesk.core.refresh

import com.pyrus.pyrusservicedesk.sdk.repositories.SdRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class RefreshUseCase(
    private val repository: SdRepository,
    private val coreScope: CoroutineScope,
) {

    fun refresh() {
        coreScope.launch(Dispatchers.IO) {
            repository.getTicketsInfo(true)
        }
    }

}