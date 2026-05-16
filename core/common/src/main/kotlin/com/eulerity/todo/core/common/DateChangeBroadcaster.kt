package com.eulerity.todo.core.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction over system date/time/timezone change broadcasts.
 * The separation allows unit tests to inject a fake without needing an Android Context.
 */
interface DateChangeBroadcaster {
    /** Emits [Unit] whenever the system date, time, or timezone changes. */
    val changes: Flow<Unit>
}

/**
 * Production implementation backed by a dynamic [BroadcastReceiver].
 * Registered only while the app process is alive — no background wakeups needed.
 */
@Singleton
class SystemDateChangeBroadcaster @Inject constructor(
    @ApplicationContext private val context: Context,
) : DateChangeBroadcaster {
    override val changes: Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        awaitClose { context.unregisterReceiver(receiver) }
    }
}
