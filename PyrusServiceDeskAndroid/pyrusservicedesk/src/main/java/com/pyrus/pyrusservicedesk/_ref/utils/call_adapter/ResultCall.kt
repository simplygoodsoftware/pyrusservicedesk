package com.pyrus.pyrusservicedesk._ref.utils.call_adapter

import com.pyrus.pyrusservicedesk._ref.utils.Try
import okio.Timeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

internal class ResultCall<T>(proxy: Call<T>) : CallDelegate<T, Try<T>>(proxy) {

    override fun enqueueImpl(callback: Callback<Try<T>>) {
        proxy.enqueue(ResultCallback(this, callback))
    }

    override fun cloneImpl(): ResultCall<T> {
        return ResultCall(proxy.clone())
    }

    private class ResultCallback<T>(
        private val proxy: ResultCall<T>,
        private val callback: Callback<Try<T>>
    ) : Callback<T> {

        override fun onResponse(call: Call<T>, response: Response<T>) {
            val result: Try<T> = if (response.isSuccessful) {
                Try.Success(response.body() as T)
            }
            else {
                Try.Failure(
                    HttpException(
                        statusCode = response.code(),
                        statusMessage = response.message(),
                        url = call.request().url().toString(),
                    )
                )
            }
            callback.onResponse(proxy, Response.success(result))
        }

        override fun onFailure(call: Call<T>, error: Throwable) {
            val result = Try.Failure(error)
            callback.onResponse(proxy, Response.success(result))
        }
    }

    override fun timeout(): Timeout {
        return proxy.timeout()
    }
}