package com.example.andbluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Toast
import com.example.andbluetooth.MainActivity.Companion.dataList
import com.example.andbluetooth.MainActivity.Companion.recyclerView2Adapter
import java.util.*

var UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
var TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
var CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")


private val TAG = "로그"

class DeviceControlActivity(private val context: Context?, private var bluetoothGatt: BluetoothGatt?) {
    private var device: BluetoothDevice? = null
    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            when(newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to GATT server.")
                    Log.i(TAG, "Attempting to start service discovery: " +
                            bluetoothGatt?.discoverServices()
                    )
                }
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Disconnected from GATT server.")
                    disconnectGattServer()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {

                    var tx = gatt?.getService(UART_UUID)!!.getCharacteristic(TX_CHAR_UUID)

                    if (gatt.setCharacteristicNotification(tx, true)) {
                        val descriptor: BluetoothGattDescriptor = tx.getDescriptor(CCCD_UUID)
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                        Log.i(TAG, "Connected to GATT_SUCCESS")
                        broadcastUpdate("Connected " + device?.name)
                    }
                }
                    else -> {
                        Log.w(TAG, "Device service discovery failed, status : $status")
                        broadcastUpdate("Fail Connect " + device?.name)
                    }
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            val data = characteristic!!.value
            val str = String(data)
            if(str != "F" && str != "W") {
                val data1 = str.split('!')[0].split('@')[1]
                val data2 = str.split('/')[0].split('!')[1]
                Log.d("로그", "DeviceControlActivity - onCharacteristicChanged : Save in Database : 심박수 : ${data1}bpm   평균 체온 : ${data2}℃")
                dataList.add(Data(data1, data2))
                recyclerView2Adapter.notifyDataSetChanged()
            }

        }

        

        private  fun broadcastUpdate(str:String) {
            val mHandler : Handler = object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
                    Toast.makeText(context, str, Toast.LENGTH_SHORT).show()
                }
            }
            mHandler.obtainMessage().sendToTarget()
        }
        private fun disconnectGattServer() {
            Log.d(TAG, "Closing Gatt connection")
            //disconnect and close the gatt
            if (bluetoothGatt != null) {
                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()
                bluetoothGatt = null
            }
        }
    }
    fun connectGatt(device: BluetoothDevice):BluetoothGatt?{
        this.device = device

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothGatt = device.connectGatt(context, false, gattCallback,
                BluetoothDevice.TRANSPORT_LE)
        }
        else {
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        }
        return bluetoothGatt
    }
}