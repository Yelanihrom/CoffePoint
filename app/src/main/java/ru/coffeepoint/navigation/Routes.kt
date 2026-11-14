package ru.coffeepoint.navigation

sealed class Routes(val route: String) {

    data object RoleSelection : Routes("role_selection")

    data object Dashboard : Routes("dashboard")

    data object ManagerHome : Routes("manager_home")

    data object NotificationSettings : Routes("notification_settings")

    data object Settings : Routes("settings")

    data object SalesAnalytics : Routes("sales_analytics")
    data object AnalyticsMenu : Routes("analytics_menu")
    data object ServiceVisitHistory : Routes("service_visit_history")

    data object SupplyCatalog : Routes("supply_catalog")

    data object WarehouseStock : Routes("warehouse_stock")

    data object WarehouseStockSettings : Routes("warehouse_stock_settings")

    data object MonthlyEarnings : Routes("monthly_earnings?employeeRole={employeeRole}") {
        fun createRoute(employeeRole: String = "OPERATOR"): String =
            "monthly_earnings?employeeRole=$employeeRole"
    }

    data object SupplyCatalogEdit : Routes("supply_catalog_edit/{itemId}/{itemType}") {

        fun createRoute(
            itemId: Int,
            itemType: String = "cups"
        ): String {
            return "supply_catalog_edit/$itemId/$itemType"
        }

    }

    data object StationCreate : Routes("station_create/{stationId}") {

        fun createRoute(stationId: Int): String {
            return "station_create/$stationId"
        }

    }

    data object Station : Routes("station/{stationId}") {

        fun createRoute(stationId: Int): String {
            return "station/$stationId"
        }
    }

    data object StationSettings : Routes("station_settings/{stationId}") {

        fun createRoute(stationId: Int): String {
            return "station_settings/$stationId"
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

    data object SupplyServiceStep1 : Routes("supply_service_step1/{stationId}") {

        fun createRoute(stationId: Int): String {
            return "supply_service_step1/$stationId"
        }
    }

    data object GrindControl : Routes("grind_control/{stationId}") {

        fun createRoute(stationId: Int): String {
            return "grind_control/$stationId"
        }
    }

    data object SupplyServiceStep2 : Routes("supply_service_step2/{stationId}") {

        fun createRoute(stationId: Int): String {
            return "supply_service_step2/$stationId"
        }
    }

    data object Camera : Routes("camera/{photoType}") {

        fun createRoute(photoType: String): String {
            return "camera/$photoType"
        }

    }
}
