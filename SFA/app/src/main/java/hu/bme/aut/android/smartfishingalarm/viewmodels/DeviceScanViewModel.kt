package hu.bme.aut.android.smartfishingalarm.viewmodels

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.content.pm.PackageManager
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hu.bme.aut.android.smartfishingalarm.states.DeviceScanViewState
import hu.bme.aut.android.smartfishingalarm.utils.SERVICE_UUID

private const val TAG = "DeviceScanViewModel"

private const val SCAN_PERIOD = 10000L

class DeviceScanViewModel(app: Application) : AndroidViewModel(app) {

    private val _viewState = MutableLiveData<DeviceScanViewState>()
    val viewState = _viewState as LiveData<DeviceScanViewState>

    private val scanResults = mutableMapOf<String, BluetoothDevice>()

    private val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private var scanner: BluetoothLeScanner? = null

    private var scanCallback: DeviceScanCallback? = null
    private lateinit var scanFilters: List<ScanFilter>
    private lateinit var scanSettings: ScanSettings

    override fun onCleared() {
        super.onCleared()
        stopScanning()
    }

    fun startScan() {
        println("111111111111111111")

        scanFilters = buildScanFilters()
        scanSettings = buildScanSettings()

        println("222222222222")

        if (scanCallback == null) {
            println("333333333333333")
            scanner = adapter.bluetoothLeScanner
            _viewState.value = DeviceScanViewState.ActiveScan
            Handler().postDelayed({ stopScanning() }, SCAN_PERIOD)

            scanCallback = DeviceScanCallback()
            if (ActivityCompat.checkSelfPermission(getApplication<Application>().applicationContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                println("--------------------No Bluetooth scan Manifest.permission")
                return
            }
            println("444444444444444444")
            scanner?.startScan(scanFilters, scanSettings, scanCallback)
        }
    }

    private fun stopScanning() {
        println("---------scanning stopped")
        scanner?.stopScan(scanCallback)
        scanCallback = null
        _viewState.value = DeviceScanViewState.ScanResults(scanResults)
    }


    private fun buildScanFilters(): List<ScanFilter> {
//        val builder = ScanFilter.Builder()
//        builder.setServiceUuid(ParcelUuid(SERVICE_UUID))
//        val filter = builder.build()
//        return listOf(filter)

        val filters: MutableList<ScanFilter> = ArrayList()
        val beaconFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        filters.add(beaconFilter)
        return filters
    }


    private fun buildScanSettings(): ScanSettings {
        return ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
    }


    private inner class DeviceScanCallback : ScanCallback() {
        override fun onBatchScanResults(results: List<ScanResult>) {
            Log.d("----------------1", "onBatch")
            super.onBatchScanResults(results)
            for (item in results) {
                item.device?.let { device ->
                    scanResults[device.address] = device
                }
            }
            Log.i(TAG, scanResults.toString())
            _viewState.value = DeviceScanViewState.ScanResults(scanResults)
        }

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.d("-----------------2", "onScanResult")
            super.onScanResult(callbackType, result)
            result.device?.let { device ->
                scanResults[device.address] = device
            }
            Log.i(TAG, scanResults.toString())
            _viewState.value = DeviceScanViewState.ScanResults(scanResults)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d("-----------------2", "onScanFailed")
            super.onScanFailed(errorCode)
            val errorMessage = "Scan failed with error: $errorCode"
            _viewState.value = DeviceScanViewState.Error(errorMessage)
        }
    }
}