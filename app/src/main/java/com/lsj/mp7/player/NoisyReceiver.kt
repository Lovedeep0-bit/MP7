package com.lsj.mp7.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NoisyReceiver(private val onNoisy: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        onNoisy()
    }
}


