package com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview

import androidx.recyclerview.widget.DiffUtil

/**
 * Object that incapsulates data to be applied to [AdapterBase]
 * when gentle applying of the difference between current data and the new one is required.
 *
 * @param diffResult [DiffUtil.DiffResult] that should be applied to [AdapterBase]
 * @param newItems list of new items that should be applied to [AdapterBase]. This list should be
 * applied before [diffResult] applied its data to an adapter
 */
internal class DiffResultWithNewItems<Item>(
    val diffResult: DiffUtil.DiffResult,
    val newItems: List<Item>)