package ru.coffeepoint.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.coffeepoint.core.auth.RolePreferences
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.feature.auth.ui.RoleSelectionScreen
import ru.coffeepoint.feature.camera.ui.CameraScreen
import ru.coffeepoint.feature.dashboard.ui.DashboardScreen
import ru.coffeepoint.feature.earnings.ui.MonthlyEarningsScreen
import ru.coffeepoint.feature.service.ui.ServiceStep1Screen
import ru.coffeepoint.feature.service.ui.ServiceStep2Screen
import ru.coffeepoint.feature.service.ui.SupplyServiceStep2Screen
import ru.coffeepoint.feature.station.ui.StationCreateScreen
import ru.coffeepoint.feature.station.ui.StationScreen
import ru.coffeepoint.feature.station.ui.StationSettingsScreen
import ru.coffeepoint.feature.supplycatalog.ui.SupplyCatalogEditScreen
import ru.coffeepoint.feature.supplycatalog.ui.SupplyCatalogScreen
import ru.coffeepoint.feature.warehouse.ui.WarehouseStockScreen

@Composable
fun AppNavigation() {

    val navController = rememberNavController()
    val context = LocalContext.current
    val rolePreferences = remember {
        RolePreferences(context)
    }

    var selectedRole by remember {
        mutableStateOf(rolePreferences.getSelectedRole())
    }

    val startDestination = remember {
        if (selectedRole != null) {
            Routes.Dashboard.route
        } else {
            Routes.RoleSelection.route
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        composable(Routes.RoleSelection.route) {

            RoleSelectionScreen(
                onRoleSelected = { role ->
                    rolePreferences.saveSelectedRole(role)
                    selectedRole = role

                    navController.navigate(Routes.Dashboard.route) {
                        popUpTo(Routes.RoleSelection.route) {
                            inclusive = true
                        }
                    }
                }
            )

        }

        composable(Routes.Dashboard.route) {

            val role = selectedRole ?: UserRole.OPERATOR

            DashboardScreen(
                role = role,
                onStationClick = { stationId ->

                    navController.navigate(
                        Routes.Station.createRoute(stationId)
                    )

                },
                onAddStation = {

                    if (role == UserRole.MANAGER) {
                        navController.navigate(
                            Routes.StationCreate.createRoute(0)
                        )
                    }

                },
                onChangeRole = {
                    rolePreferences.clearSelectedRole()
                    selectedRole = null

                    navController.navigate(Routes.RoleSelection.route) {
                        popUpTo(Routes.Dashboard.route) {
                            inclusive = true
                        }
                    }

                },
                onCloseApp = {
                    (context as? Activity)?.finish()
                },
                onSupplyCatalog = {
                    if (role == UserRole.MANAGER ||
                        role == UserRole.SUPPLY_OPERATOR
                    ) {
                        navController.navigate(Routes.SupplyCatalog.route)
                    }
                },
                onWarehouseStock = {
                    if (role == UserRole.MANAGER ||
                        role == UserRole.SUPPLY_OPERATOR
                    ) {
                        navController.navigate(Routes.WarehouseStock.route)
                    }
                },
                onMonthlyEarnings = {
                    if (role == UserRole.MANAGER ||
                        role == UserRole.OPERATOR
                    ) {
                        navController.navigate(Routes.MonthlyEarnings.route)
                    }
                }
            )

        }

        composable(Routes.MonthlyEarnings.route) {

            val role = selectedRole ?: UserRole.OPERATOR

            if (role != UserRole.MANAGER &&
                role != UserRole.OPERATOR
            ) {
                navController.popBackStack()
                return@composable
            }

            MonthlyEarningsScreen(
                canCreatePayment = role == UserRole.MANAGER,
                onBack = {
                    navController.popBackStack()
                }
            )

        }

        composable(Routes.WarehouseStock.route) {

            val role = selectedRole ?: UserRole.OPERATOR

            if (role != UserRole.MANAGER &&
                role != UserRole.SUPPLY_OPERATOR
            ) {
                navController.popBackStack()
                return@composable
            }

            WarehouseStockScreen(
                onBack = {
                    navController.popBackStack()
                }
            )

        }

        composable(Routes.SupplyCatalog.route) {

            val role = selectedRole ?: UserRole.OPERATOR

            SupplyCatalogScreen(
                canEdit = role == UserRole.MANAGER,
                onBack = {
                    navController.popBackStack()
                },
                onEditOption = { itemId ->
                    navController.navigate(
                        Routes.SupplyCatalogEdit.createRoute(itemId)
                    )
                },
                onAddOption = { itemType ->
                    navController.navigate(
                        Routes.SupplyCatalogEdit.createRoute(
                            itemId = 0,
                            itemType = itemType
                        )
                    )
                }
            )

        }

        composable(
            route = Routes.SupplyCatalogEdit.route,
            arguments = listOf(
                navArgument("itemId") {
                    type = NavType.IntType
                },
                navArgument("itemType") {
                    type = NavType.StringType
                }
            )
        ) {

            val itemId = it.arguments?.getInt("itemId") ?: 0
            val itemType = it.arguments?.getString("itemType") ?: "cups"
            val role = selectedRole ?: UserRole.OPERATOR

            if (role != UserRole.MANAGER) {
                navController.popBackStack()
                return@composable
            }

            SupplyCatalogEditScreen(
                itemId = itemId,
                initialItemType = itemType,
                onBack = {
                    navController.popBackStack()
                },
                onSaved = {
                    navController.popBackStack()
                }
            )

        }

        composable(
            route = Routes.StationCreate.route,
            arguments = listOf(
                navArgument("stationId") {
                    type = NavType.IntType
                }
            )
        ) {

            val stationId = it.arguments?.getInt("stationId") ?: 0

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
            val role = selectedRole ?: UserRole.OPERATOR

            StationScreen(
                stationId = stationId,
                role = role,
                onBack = {
                    navController.popBackStack()
                },
                onStartService = {
                    navController.navigate(
                        Routes.ServiceStep1.createRoute(stationId)
                    )
                },
                onStartSupplyService = {
                    if (role == UserRole.MANAGER ||
                        role == UserRole.SUPPLY_OPERATOR
                    ) {
                        navController.navigate(
                            Routes.SupplyServiceStep1.createRoute(stationId)
                        )
                    }
                },
                onSettings = {
                    if (role == UserRole.MANAGER) {
                        navController.navigate(
                            Routes.StationSettings.createRoute(stationId)
                        )
                    }
                }
            )

        }

        composable(
            route = Routes.StationSettings.route,
            arguments = listOf(
                navArgument("stationId") {
                    type = NavType.IntType
                }
            )
        ) {

            val stationId = it.arguments?.getInt("stationId") ?: 0

            StationSettingsScreen(
                stationId = stationId,
                onBack = {
                    navController.popBackStack()
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
                role = selectedRole ?: UserRole.OPERATOR,
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
            route = Routes.SupplyServiceStep1.route,
            arguments = listOf(
                navArgument("stationId") {
                    type = NavType.IntType
                }
            )
        ) {

            val stationId = it.arguments?.getInt("stationId") ?: 0
            val role = selectedRole ?: UserRole.OPERATOR
            val isSupplyServiceFlow = role == UserRole.SUPPLY_OPERATOR ||
                role == UserRole.MANAGER

            ServiceStep1Screen(
                stationId = stationId,
                role = role,
                onBack = {
                    navController.popBackStack()
                },
                onNext = {
                    if (isSupplyServiceFlow) {
                        navController.navigate(
                            Routes.SupplyServiceStep2.createRoute(stationId)
                        )
                    } else {
                        navController.navigate(Routes.Dashboard.route) {
                            popUpTo(Routes.Dashboard.route) {
                                inclusive = true
                            }
                        }
                    }
                },
                onFinish = {
                    navController.navigate(Routes.Dashboard.route) {
                        popUpTo(Routes.Dashboard.route) {
                            inclusive = true
                        }
                    }
                },
                stepLabel = if (isSupplyServiceFlow) {
                    "Шаг 1 из 2"
                } else {
                    "Пополнение запасов"
                },
                actionText = if (isSupplyServiceFlow) {
                    "Далее"
                } else {
                    "Завершить пополнение"
                },
                syncImmediatelyOnSave = true
            )

        }

        composable(
            route = Routes.SupplyServiceStep2.route,
            arguments = listOf(
                navArgument("stationId") {
                    type = NavType.IntType
                }
            )
        ) {

            val stationId = it.arguments?.getInt("stationId") ?: 0

            SupplyServiceStep2Screen(
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

            val photoType = it.arguments?.getString("photoType") ?: ""

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
