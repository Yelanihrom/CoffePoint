package ru.coffeepoint.feature.service.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.coffeepoint.feature.service.component.ChecklistRow
import ru.coffeepoint.feature.service.component.FinishServiceButton
import ru.coffeepoint.feature.service.component.PhotoCard
import ru.coffeepoint.feature.service.presentation.ServiceStep2ViewModel
import ru.coffeepoint.feature.camera.model.PhotoType
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import ru.coffeepoint.feature.service.presentation.ServiceStep1ViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceStep2Screen(
    stationId: Int,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onOpenCamera: (PhotoType) -> Unit,
    photoResult: Pair<String, String>?
) {

    val stationViewModel: ServiceStep1ViewModel = viewModel()

    stationViewModel.loadStation(stationId)

    val station = stationViewModel.station ?: return

    val viewModel: ServiceStep2ViewModel = viewModel()

    val checklist by viewModel::checklist
    val machinePhoto by viewModel::machinePhoto
    val overviewPhoto by viewModel::standPhoto
    LaunchedEffect(photoResult) {

        photoResult ?: return@LaunchedEffect

        val type = when (photoResult.second) {

            "MACHINE" -> PhotoType.MACHINE

            else -> PhotoType.OVERVIEW

        }

        viewModel.setPhoto(
            stationId = stationId,
            photoType = type,
            path = photoResult.first
        )

    }
    val context = LocalContext.current

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text("Шаг 2 из 2")

                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )

                    }

                }

            )

        }

    ) { padding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),

            verticalArrangement = Arrangement.spacedBy(16.dp)

        ) {

            Text(
                text = station.station.name
            )

            Card {

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        "Чек-лист",
                        fontWeight = FontWeight.Bold
                    )

                    checklist.forEachIndexed { index, item ->

                        ChecklistRow(

                            checked = item.checked,

                            title = item.title,

                            hint = item.description,

                            onCheckedChange = {

                                viewModel.toggleChecklist(
                                    index,
                                    it
                                )

                            }

                        )

                    }

                }

            }

            PhotoCard(

                machinePhoto = machinePhoto,

                overviewPhoto = overviewPhoto,

                onMachineClick = {
                    onOpenCamera(PhotoType.MACHINE)
                },

                onOverviewClick = {
                    onOpenCamera(PhotoType.OVERVIEW)
                }

            )

            FinishServiceButton(

                enabled = viewModel.canFinish(),

                onClick = {

                    viewModel.finishService(stationId) {

                        onFinish()

                    }
                }
            )

        }

    }

}