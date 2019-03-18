package net.papirus.pyrusservicedesk.ui.view.swiperefresh

/**
 * Class taken from https://github.com/omadahealth/SwipyRefreshLayout. v.1.2.3.
 */
internal enum class RefreshLayoutDirection(private val mValue: Int) {

    TOP(0),
    BOTTOM(1),
    BOTH(2);


    companion object {

        fun getFromInt(value: Int): RefreshLayoutDirection {
            for (direction in RefreshLayoutDirection.values()) {
                if (direction.mValue == value) {
                    return direction
                }
            }
            return BOTH
        }
    }

}
