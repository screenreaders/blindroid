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
    private val calls = CopyOnWriteArraySet<Call>()

    @Volatile
    private var currentCall: Call? = null

    fun getCall(): Call? = currentCall

    fun getCalls(): List<Call> = calls.toList()

    fun getActiveCall(): Call? = firstCall { it.state == Call.STATE_ACTIVE }

    fun getRingingCall(): Call? = firstCall { it.state == Call.STATE_RINGING }

    fun hasActiveCall(exclude: Call? = null): Boolean {
        for (call in calls) {
            if (call == exclude) continue
            if (call.state == Call.STATE_ACTIVE) return true
        }
        return false
    }

    fun addCall(call: Call) {
        calls.add(call)
        updateCurrentCall()
    }

    fun removeCall(call: Call) {
        calls.remove(call)
        updateCurrentCall()
    }

    fun updateCall(call: Call) {
        if (calls.contains(call)) {
            updateCurrentCall()
        }
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
        notifyListener(listener, currentCall)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    private fun updateCurrentCall() {
        currentCall = selectPrimaryCall()
        notifyListeners(currentCall)
    }

    private fun selectPrimaryCall(): Call? {
        return firstCall { it.state == Call.STATE_ACTIVE }
            ?: firstCall { it.state == Call.STATE_DIALING }
            ?: firstCall { it.state == Call.STATE_CONNECTING }
            ?: firstCall { it.state == Call.STATE_RINGING }
            ?: firstCall { it.state == Call.STATE_HOLDING }
            ?: firstCall { call ->
                call.state != Call.STATE_DISCONNECTED && call.state != Call.STATE_DISCONNECTING
            }
    }

    private fun firstCall(predicate: (Call) -> Boolean): Call? {
        for (call in calls) {
            if (predicate(call)) return call
        }
        return null
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
