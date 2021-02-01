package com.pyrus.pyrusservicedesk

import org.mockito.ArgumentCaptor
import org.mockito.Mockito

object MockitoExtensions {
    fun <T> kEq(obj: T): T = Mockito.eq(obj)
    fun <T> kAny(): T = Mockito.any()
    fun <T> kCapture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()
}