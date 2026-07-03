package ru.coffeepoint.navigation

sealed class Routes(val route: String) {

    data object Dashboard : Routes("dashboard")

    data object StationEditor : Routes("station_editor/{stationId}") {

        fun createRoute(stationId: Int): String {
            return "station_editor/$stationId"
        }

    }

    data object Station : Routes("station/{stationId}") {

        fun createRoute(stationId: Int): String {
            return "station/$stationId"
        }
    }

    data object ServiceStep1 : Routes("service_step1/{stationId}") {

        fun createRoute(stationId: Int): String {
            return "service_step1/$stationId"
        }
    }

    data object ServiceStep2 : Routes("service_step2/{stationId}") {

        fun createRoute(stationId: Int): String {
            return "service_step2/$stationId"
        }
    }

    data object Camera : Routes("camera/{photoType}") {

        fun createRoute(photoType: String): String {
            return "camera/$photoType"
        }

    }
}