package net.papirus.pyrusservicedesk.ui.view.recyclerview

import android.support.v7.util.DiffUtil

internal class DiffResultWithNewItems<Item>(
    val diffResult: DiffUtil.DiffResult,
    val newItems: List<Item>)