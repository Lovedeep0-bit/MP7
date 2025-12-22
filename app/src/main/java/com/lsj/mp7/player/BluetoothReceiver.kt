package com.lsj.mp7.player

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class BluetoothReceiver(
    private val onDisconnected: () -> Unit,
    private val onConnected: () -> Unit,
) : BroadcastReceiver() {
    fun intentFilter(): IntentFilter = IntentFilter().apply {
        addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> onDisconnected()
            BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED, BluetoothDevice.ACTION_ACL_CONNECTED -> onConnected()
        }
    }
}


