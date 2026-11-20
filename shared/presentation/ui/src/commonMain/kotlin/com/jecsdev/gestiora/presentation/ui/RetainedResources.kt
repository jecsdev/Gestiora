package com.jecsdev.gestiora.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.retain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication

/** These resources stay in RAM across configuration changes, never in saved instance state. */
internal class RetainedOperationScope(dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : RetainObserver {
    val scope = CoroutineScope(SupervisorJob() + dispatcher)
    override fun onRetained() = Unit
    override fun onEnteredComposition() = Unit
    override fun onExitedComposition() = Unit
    override fun onRetired() { scope.cancel() }
    override fun onUnused() { scope.cancel() }
}

@Composable
internal fun retainedOperationScope(): CoroutineScope = retain { RetainedOperationScope() }.scope

/** Keeps one repository/mutex graph while an operation continues through Activity recreation. */
internal class RetainedAppGraph(modules: List<Module>) : RetainObserver {
    val application: KoinApplication = koinApplication { modules(modules) }
    private var closed = false
    override fun onRetained() = Unit
    override fun onEnteredComposition() = Unit
    override fun onExitedComposition() = Unit
    override fun onRetired() = close()
    override fun onUnused() = close()
    private fun close() {
        if (!closed) {
            closed = true
            application.close()
        }
    }
}
