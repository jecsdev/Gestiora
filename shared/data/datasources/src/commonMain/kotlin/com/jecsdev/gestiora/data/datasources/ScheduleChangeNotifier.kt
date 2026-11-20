package com.jecsdev.gestiora.data.datasources

/** Wake the platform scheduler AFTER a successful durable agenda mutation. No clinical payload. */
fun interface ScheduleChangeNotifier {
    fun requestReconcile()
}
