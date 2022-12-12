package hu.bme.aut.android.smartfishingalarm.presentation

import android.app.Application
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.bme.aut.android.smartfishingalarm.presentation.MyApplication.Companion.bluetoothGatt
import hu.bme.aut.android.smartfishingalarm.presentation.MyApplication.Companion.isWritable
import hu.bme.aut.android.smartfishingalarm.presentation.MyApplication.Companion.isWritableWithoutResponse
import hu.bme.aut.android.smartfishingalarm.states.DeviceScanViewState
import hu.bme.aut.android.smartfishingalarm.utils.CHARACTERISTIC_UUID_RX
import hu.bme.aut.android.smartfishingalarm.utils.CHARACTERISTIC_UUID_TX
import hu.bme.aut.android.smartfishingalarm.utils.CLIENT_CHARACTERISTIC_CONFIGURATION_UUID
import hu.bme.aut.android.smartfishingalarm.utils.SERVICE_UUID


private const val TAG = "DeviceScanCompose"

object DeviceScanCompose {

    @Composable
    fun ShowDevices(
        scanResults: Map<String, BluetoothDevice>,
        onClick: (BluetoothDevice?) -> Unit
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth()
        ) {
            itemsIndexed(scanResults.keys.toList()) { _, key ->
                Column {
                    Column(
                        modifier = Modifier
                            .clickable {
                                val device: BluetoothDevice? = scanResults.get(key = key)
                                onClick(device)
                            }
                            .background(Color.LightGray, shape = RoundedCornerShape(10.dp))
                            .fillMaxWidth()
                            .border(1.dp, Color.Black, shape = RoundedCornerShape(10.dp))
                            .padding(5.dp)
                    ) {
                        Text(
                            text = scanResults[key]?.name ?: "Unknown Device",
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = scanResults[key]?.address ?: "",
                            fontWeight = FontWeight.Light
                        )
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                }
            }
        }
    }

    @Composable
    fun DeviceScan(deviceScanViewState: DeviceScanViewState, onDeviceSelected: () -> Unit) {
        when (deviceScanViewState) {
            is DeviceScanViewState.ActiveScan -> {
                println("----------scanning------------")
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(15.dp))
                        Text(
                            text = "Scanning for devices",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }

            }
            is DeviceScanViewState.ScanResults -> {
                ShowDevices(scanResults = deviceScanViewState.scanResults, onClick = {
                    Log.i(TAG, "Device Selected ${it!!.name ?: ""}")
                    Log.i("------------------", "Device Selected")
                    val device: BluetoothDevice = it
                    device.connectGatt(MyApplication.appContext, false, bluetoothGattCallback, TRANSPORT_LE)
                    onDeviceSelected()
                })
            }
            is DeviceScanViewState.Error -> {
                println("error-----------------")
                Text(text = deviceScanViewState.message)
            }
            else -> {
                Text(text = "Nothing")
            }
        }
    }
}

val bluetoothGattCallback = object : BluetoothGattCallback() {
    private var notificationCharacteristic: BluetoothGattCharacteristic? = null

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        val isSuccess = status == BluetoothGatt.GATT_SUCCESS
        val isConnected = newState == BluetoothProfile.STATE_CONNECTED
        if (isSuccess && isConnected) {
            bluetoothGatt = gatt
            bluetoothGatt!!.discoverServices()
        }
    }

    override fun onServicesDiscovered(discoveredGatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(discoveredGatt, status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            val service = discoveredGatt.getService(SERVICE_UUID)
            notificationCharacteristic = service.getCharacteristic(CHARACTERISTIC_UUID_TX)
            MyApplication.writeCharacteristic = service.getCharacteristic(CHARACTERISTIC_UUID_RX)

            bluetoothGatt!!.setCharacteristicNotification(notificationCharacteristic, true)
            enableNotifications(notificationCharacteristic!!)
        }
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        super.onDescriptorWrite(gatt, descriptor, status)
        println("descriptooor------------")
        println(status==BluetoothGatt.GATT_SUCCESS)
        println("descriptooor------------")
    }

    fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        val payload = when {
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> {
                Log.e("ConnectionManager", "${characteristic.uuid} doesn't support notifications/indications")
                return
            }
        }

        characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION_UUID
        )?.let { cccDescriptor ->
            if (bluetoothGatt?.setCharacteristicNotification(characteristic, true) == false) {
                Log.e("ConnectionManager", "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, payload)
        } ?: Log.e("ConnectionManager", "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }

    fun writeDescriptor(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        bluetoothGatt?.let { gatt ->
            descriptor.value = payload
            gatt!!.writeDescriptor(descriptor)
        } ?: error("Not connected to a BLE device!")
    }

    fun BluetoothGattCharacteristic.isReadable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

    fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

    fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
        return properties and property != 0
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        super.onCharacteristicChanged(gatt, characteristic)
        println("halooo-------------------------")
        val readBytes: ByteArray = characteristic!!.value
        val ASCIIString = String(readBytes, Charsets.UTF_8)
        MyApplication.receivedValue = ASCIIString
        ConnectedDeviceCompose.readData.value = ASCIIString
        println(ASCIIString)
        println("halooo-------------------------")
    }

    // https://punchthrough.com/android-ble-guide/
    override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        super.onCharacteristicRead(gatt, characteristic, status)
        when (status) {
            BluetoothGatt.GATT_SUCCESS -> {
                Log.i("BluetoothGattCallback", "Read characteristic")
                val readBytes: ByteArray = characteristic.value
                val ASCIIString = String(readBytes, Charsets.UTF_8)
                MyApplication.receivedValue = ASCIIString
                println(MyApplication.receivedValue)
            }
            BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                Log.e("BluetoothGattCallback", "Read not permitted for")
            }
            else -> {
                Log.e("BluetoothGattCallback", "Characteristic read failed for")
            }
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        println("big success--------------------------")
        super.onCharacteristicWrite(gatt, characteristic, status)
    }
}

fun writeCharacteristic(payload: ByteArray) {
    val writeType = when {
        MyApplication.writeCharacteristic!!.isWritable() -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        MyApplication.writeCharacteristic!!.isWritableWithoutResponse() -> {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }
        else -> error("Characteristic ${MyApplication.writeCharacteristic!!.uuid} cannot be written to")
    }

    bluetoothGatt?.let { gatt ->
        MyApplication.writeCharacteristic!!.writeType = writeType
        MyApplication.writeCharacteristic!!.value = payload
        bluetoothGatt!!.writeCharacteristic(MyApplication.writeCharacteristic)
    } ?: error("Not connected to a BLE device!")
}

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }

    companion object {
        private var context: Context? = null
        val appContext: Context?
            get() = context

        var receivedValue = "0"

        var bluetoothGatt: BluetoothGatt? = null
        var writeCharacteristic: BluetoothGattCharacteristic? = null

        fun BluetoothGattCharacteristic.isWritable(): Boolean =
            containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

        fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean =
            containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

        fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
            return properties and property != 0
        }
    }
}
