package net.papirus.pyrusservicedesk.sdk.response

internal interface ResponseCallback<Data> {
    fun onSuccess(data: Data)
    fun onFailure(responseError: ResponseError)
}
