package im.casper.locationlist.navigation

object Routes {
    const val HOME = "home"
    const val LOCATION_LIST = "location_list"
    const val CREATE_LOCATION = "create_location"
    const val MAP = "map"
    const val SHARE = "share"
    const val SETTINGS = "settings"
    const val LOCATION_DETAIL = "location_detail"
    fun locationDetail(id: Long) = "$LOCATION_DETAIL/$id"
}