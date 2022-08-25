package com.example.andbluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val REQUEST_ENABLE_BT = 1
    private var bluetoothAdapter: BluetoothAdapter? = null

    private val REQUEST_ALL_PERMISSION = 2
    private val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private var scanning: Boolean = false
    private var devicesArr = ArrayList<BluetoothDevice>()
    private val SCAN_PERIOD = 1000
    private val handler = Handler()

    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var recyclerViewAdapter: RecyclerViewAdapter

    //BLE Gatt
    private var bleGatt: BluetoothGatt? = null
    private var mContext:Context? = null

    private val mLeScanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d("scanCallback", "BLE Scan Failed : " + errorCode)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.let {
                //results is not null
                for (result in it) {
                    if (!devicesArr.contains(result.device) && result.device.name != null) devicesArr.add(
                        result.device
                    )
                }
            }
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                //result is not null
                if (!devicesArr.contains(it.device) && it.device.name != null) devicesArr.add(it.device)
                recyclerViewAdapter.notifyDataSetChanged()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun scanDevice(state: Boolean) = if (state) {
        handler.postDelayed({
            scanning = false
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(mLeScanCallback)
        }, SCAN_PERIOD)
        scanning = true
        devicesArr.clear()
        bluetoothAdapter?.bluetoothLeScanner?.startScan(mLeScanCallback)
    } else {
        scanning = false
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(mLeScanCallback)
    }


    private fun hasPermissions(context: Context?, permissions: Array<String>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
        return true
    }

    //Permission 확인

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_ALL_PERMISSION -> {
                //if request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission must be granted", Toast.LENGTH_SHORT).show()
                } else {
                    requestPermissions(permissions, REQUEST_ALL_PERMISSION)
                    Toast.makeText(this, "Permissions must be granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mContext = this

        val bleOnOffBtn:ToggleButton = findViewById(R.id.ble_on_off_btn)
        val scanBtn: Button = findViewById(R.id.scanBtn)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        viewManager = LinearLayoutManager(this)
        recyclerViewAdapter = RecyclerViewAdapter(devicesArr)


        recyclerViewCreate()


        recyclerViewAdapter.mListener = object : RecyclerViewAdapter.OnItemClickListener{
            override fun onClick(view: View, position: Int) {
                scanDevice(false) //scan 중지
                val device = devicesArr.get(position)
                bleGatt = DeviceControlActivity(mContext, bleGatt).connectGatt(device)
            }
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = viewManager
            adapter = recyclerViewAdapter
        }

        if(bluetoothAdapter!=null){
            // Device doesn't support Bluetooth
            if(bluetoothAdapter?.isEnabled==false){
                bleOnOffBtn.isChecked = true
                scanBtn.isVisible = false
            } else{
                bleOnOffBtn.isChecked = false
                scanBtn.isVisible =true
            }
        }

        bleOnOffBtn.setOnCheckedChangeListener { _, isChecked ->
            bluetoothOnOff()
            scanBtn.visibility =
                if (scanBtn.visibility == View.VISIBLE){
                    View.INVISIBLE
                } else {
                    View.VISIBLE
                }
            if (scanBtn.visibility == View.INVISIBLE){
                scanDevice(false)
                devicesArr.clear()
                recyclerViewAdapter.notifyDataSetChanged()
            }
        }

        scanBtn.setOnClickListener{
            v:View? -> // Scan Button Oncliick
            if(!hasPermissions(this, PERMISSIONS)) {
                requestPermissions(PERMISSIONS, REQUEST_ALL_PERMISSION)
            }
            scanDevice(true)
        }
    }

    companion object {
        var dataList = ArrayList<Data>()
        @SuppressLint("StaticFieldLeak")
        lateinit var recyclerView2Adapter: RecyclerView2Adapter
    }

    private fun recyclerViewCreate() {
        val recyclerView2 = findViewById<RecyclerView>(R.id.recyclerView2)
        recyclerView2Adapter = RecyclerView2Adapter(this, dataList)
        val layoutManager = LinearLayoutManager(this)
        recyclerView2.layoutManager = layoutManager
        recyclerView2.adapter = recyclerView2Adapter
    }

    fun bluetoothOnOff(){
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Log.d("bluetoothAdapter","Device doesn't support Bluetooth")
        }else{
            if (bluetoothAdapter?.isEnabled == false) { // 블루투스 꺼져 있으면 블루투스 활성화
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else{ // 블루투스 켜져있으면 블루투스 비활성화
                bluetoothAdapter?.disable()
            }
        }
    }
class RecyclerViewAdapter(
    private val myDataset: ArrayList<BluetoothDevice>):
        RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> () {

        var mListener : OnItemClickListener? = null

        interface OnItemClickListener {
            fun onClick(view: View, position: Int)
        }

            class MyViewHolder(
                val linearView: LinearLayout):RecyclerView.ViewHolder(linearView)
            override fun onCreateViewHolder(parent: ViewGroup,
                                            viewType: Int):RecyclerViewAdapter.MyViewHolder {
                //create a new view
                val linearView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.recyclerview_item, parent, false) as LinearLayout
                return MyViewHolder(linearView)
            }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val itemName: TextView = holder.linearView.findViewById(R.id.item_name)
        val itemAddress: TextView = holder.linearView.findViewById(R.id.item_address)
        itemName.text = myDataset[position].name
        itemAddress.text = myDataset[position].address
        if(mListener!=null) {
            holder?.itemView?.setOnClickListener{v->
                mListener?.onClick(v, position)
            }
        }
    }

    override fun getItemCount() = myDataset.size
    }
}
private fun Handler.postDelayed(function: () -> Unit?, scanPeriod: Int) {

}

