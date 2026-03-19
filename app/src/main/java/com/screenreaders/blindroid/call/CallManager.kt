package com.screenreaders.blindroid.call

import android.os.Handler
import android.os.Looper
import android.telecom.Call
import java.util.concurrent.CopyOnWriteArraySet

object CallManager {
    interface Listener {
        fun onCallChanged(call: Call?)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArraySet<Listener>()

    @Volatile
    private var currentCall: Call? = null

    fun getCall(): Call? = currentCall

    fun setCall(call: Call?) {
        currentCall = call
        notifyListeners(call)
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
        notifyListener(listener, currentCall)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    private fun notifyListeners(call: Call?) {
        mainHandler.post {
            for (listener in listeners) {
                listener.onCallChanged(call)
            }
        }
    }

    private fun notifyListener(listener: Listener, call: Call?) {
        mainHandler.post { listener.onCallChanged(call) }
    }
}
