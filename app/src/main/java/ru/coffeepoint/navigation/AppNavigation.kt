package ru.coffeepoint.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.coffeepoint.feature.camera.ui.CameraScreen
import ru.coffeepoint.feature.dashboard.ui.DashboardScreen
import ru.coffeepoint.feature.service.ui.ServiceStep1Screen
import ru.coffeepoint.feature.service.ui.ServiceStep2Screen
import ru.coffeepoint.feature.station.ui.StationScreen
import ru.coffeepoint.feature.station.ui.StationCreateScreen

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Dashboard.route
    ) {

        composable(Routes.Dashboard.route) {

            DashboardScreen(

                onStationClick = { stationId ->

                    navController.navigate(
                        Routes.Station.createRoute(stationId)
                    )

                },

                onAddStation = {

                    navController.navigate(
                        Routes.StationEditor.createRoute(0)
                    )

                }

            )

        }

        composable(
            route = Routes.StationEditor.route,
            arguments = listOf(
                navArgument("stationId") {
                    type = NavType.IntType
                }
            )
        ) {
            val stationId =
                it.arguments?.getInt("stationId") ?: 0

            StationCreateScreen(

                stationId = stationId,

                onBack = {

                    navController.popBackStack()

                },

                onSaved = {

                    navController.popBackStack()

                }

            )

        }

        composable(
            route = Routes.Station.route,
            arguments = listOf(
                navArgument("stationId") {
                    type = NavType.IntType
                }
            )
        ) {

            val stationId = it.arguments?.getInt("stationId") ?: 0

            StationScreen(
                stationId = stationId,

                onBack = {

                    navController.popBackStack()

                },

                onStartService = {

                    navController.navigate(
                        Routes.ServiceStep1.createRoute(stationId)
                    )

                },

                onEdit = {

                    navController.navigate(
                        Routes.StationEditor.createRoute(stationId)
                    )

                },

                onDeleted = {

                    navController.navigate(Routes.Dashboard.route) {

                        popUpTo(Routes.Dashboard.route) {
                            inclusive = true
                        }

                        launchSingleTop = true

                    }

                }

            )

        }

        composable(
            route = Routes.ServiceStep1.route,
            arguments = listOf(
                navArgument("stationId") {
                    type = NavType.IntType
                }
            )
        ) {

            val stationId = it.arguments?.getInt("stationId") ?: 0

            ServiceStep1Screen(
                stationId = stationId,
                onBack = {
                    navController.popBackStack()
                },
                onNext = {
                    navController.navigate(
                        Routes.ServiceStep2.createRoute(stationId)
                    )
                }
            )

        }

        composable(
            route = Routes.ServiceStep2.route,
            arguments = listOf(
                navArgument("stationId") {
                    type = NavType.IntType
                }
            )
        ) {

            val stationId = it.arguments?.getInt("stationId") ?: 0

            ServiceStep2Screen(
                stationId = stationId,
                onBack = {
                    navController.popBackStack()
                },
                onFinish = {
                    navController.navigate(Routes.Dashboard.route) {
                        popUpTo(Routes.Dashboard.route) {
                            inclusive = true
                        }
                    }
                },
                onOpenCamera = { photoType ->
                    navController.navigate(
                        Routes.Camera.createRoute(photoType.name)
                    )
                },
                photoResult =
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.get<String>("photoPath")
                        ?.let { path ->

                            val type =
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.get<String>("photoType")
                                    ?: return@let null

                            Pair(path, type)

                        }
            )

        }

        composable(
            route = Routes.Camera.route,
            arguments = listOf(
                navArgument("photoType") {
                    type = NavType.StringType
                }
            )
        ) {

            val photoType =
                it.arguments?.getString("photoType") ?: ""

            CameraScreen(

                onPhotoSaved = { path ->

                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("photoPath", path)

                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("photoType", photoType)

                    navController.popBackStack()

                },

                onBack = {

                    navController.popBackStack()

                }

            )

        }

    }

}