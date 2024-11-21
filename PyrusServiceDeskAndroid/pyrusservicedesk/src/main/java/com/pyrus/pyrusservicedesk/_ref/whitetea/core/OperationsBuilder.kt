package com.pyrus.pyrusservicedesk._ref.whitetea.core

@DslMarker
internal annotation class OperationsBuilderDsl

@OperationsBuilderDsl
internal class OperationsBuilder<T> {

    private val list = mutableListOf<T>()

    operator fun T?.unaryPlus() {
        this?.let(list::add)
    }

    operator fun List<T>?.unaryPlus() {
        this?.let(list::addAll)
    }

    internal fun build(): List<T> = list
}