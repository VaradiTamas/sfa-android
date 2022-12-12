package hu.bme.aut.android.smartfishingalarm

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.webkit.PermissionRequest
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.bme.aut.android.smartfishingalarm.presentation.ConnectedDeviceCompose
import hu.bme.aut.android.smartfishingalarm.presentation.DeviceScanCompose
import hu.bme.aut.android.smartfishingalarm.presentation.MyApplication.Companion.bluetoothGatt
import hu.bme.aut.android.smartfishingalarm.ui.theme.SmartFishingAlarmTheme
import hu.bme.aut.android.smartfishingalarm.viewmodels.DeviceScanViewModel

private const val TAG = "MainActivityTAG"

class MainActivity : ComponentActivity() {

    private val viewModel: DeviceScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SmartFishingAlarmTheme {
                val result = remember { mutableStateOf<Int?>(100) }
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    result.value = it.resultCode
                }

                LaunchedEffect(key1 = true){

                    Dexter.withContext(this@MainActivity)
                        .withPermissions(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                        )
                        .withListener(object : MultiplePermissionsListener {
                            override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                launcher.launch(intent)
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permissions: List<PermissionRequest?>?,
                                token: PermissionToken?
                            ) {

                            }
                        })
                        .check()

                }

                LaunchedEffect(key1 = result.value){
                    if(result.value == RESULT_OK){
                        viewModel.startScan()
                    }
                }

                Scaffold(topBar = {
                    TopAppBar(
                        title = {
                            Text(text = "Smart Fishing Alarm")
                        },
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = Color.White,
                        elevation = 10.dp
                    )
                }) {


                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        color = MaterialTheme.colors.background
                    ) {
                        val deviceScanningState by viewModel.viewState.observeAsState()

                        var isChatOpen by remember {
                            mutableStateOf(false)
                        }

                        Box(
                            contentAlignment = Alignment.TopCenter,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (deviceScanningState != null && !isChatOpen) {
                                Column {
                                    Text(
                                        text = "Choose a fishing alarm:",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    DeviceScanCompose.DeviceScan(deviceScanViewState = deviceScanningState!!) {
                                        isChatOpen = true
                                    }
                                }
                            } else {
                                ConnectedDeviceCompose.InteractionPresentation {

                                }

                                Button(
                                    onClick = {
                                        bluetoothGatt!!.disconnect()
                                        isChatOpen = false
                                    },
                                    modifier = Modifier.padding(vertical = 400.dp)
                                ) {
                                    Text(text = "Disconnect")
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}