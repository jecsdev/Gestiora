package com.jecsdev.gestiora.presentation.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.ManagedRetainedValuesStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import kotlin.test.*

/** Exercises the real retention store and resource lifecycle without Android/Compose UI mocks. */
class RetainedResourcesTest {
    @Test fun retainedStoreKeepsDraftGraphAndInFlightWriteUntilConsumed() = runBlocking<Unit> {
        val store = ManagedRetainedValuesStore()
        val releaseWrite = CompletableDeferred<Unit>()
        var writes = 0
        val draft = mutableStateOf("Borrador médico\ncon Unicode 😊")
        val owner = RetainedOperationScope(Dispatchers.Unconfined)
        val graph = RetainedAppGraph(listOf(module { single { Any() } }))
        val singleton = graph.application.koin.get<Any>()
        val write = owner.scope.launch { releaseWrite.await(); writes++ }
        store.onContentEnteredComposition()
        assertFalse(store.isRetainingExitedValues)
        store.onContentExitComposition()
        owner.onExitedComposition()
        graph.onExitedComposition()
        store.saveExitingValue("draft", draft)
        store.saveExitingValue("operations", owner)
        store.saveExitingValue("graph", graph)
        assertTrue(write.isActive)
        assertSame(draft, store.consumeExitedValueOrDefault("draft", null))
        assertSame(owner, store.consumeExitedValueOrDefault("operations", null))
        assertSame(graph, store.consumeExitedValueOrDefault("graph", null))
        store.onContentEnteredComposition()
        owner.onEnteredComposition()
        graph.onEnteredComposition()
        assertSame(singleton, graph.application.koin.get<Any>())
        assertEquals("Borrador médico\ncon Unicode 😊", draft.value)
        releaseWrite.complete(Unit)
        write.join()
        assertEquals(1, writes)
        // A screen leaving permanently releases its resources immediately.
        store.saveExitingValue("operations", owner)
        store.saveExitingValue("graph", graph)
        assertFalse(owner.scope.isActive)
        assertFails { graph.application.koin.get<Any>() }
        store.dispose()
    }

    @Test fun returningWithoutThePreviousScreenRetiresItsResources() {
        val store = ManagedRetainedValuesStore()
        val owner = RetainedOperationScope(Dispatchers.Unconfined)
        val graph = RetainedAppGraph(listOf(module { single { Any() } }))
        store.onContentEnteredComposition()
        store.onContentExitComposition()
        store.saveExitingValue("operations", owner)
        store.saveExitingValue("graph", graph)
        assertTrue(owner.scope.isActive)
        store.onContentEnteredComposition()
        assertFalse(owner.scope.isActive)
        assertFails { graph.application.koin.get<Any>() }
        store.dispose()
    }

    @Test fun abandonedResourcesAreReleasedWithoutWaitingForRecreation() {
        val scope = RetainedOperationScope(Dispatchers.Unconfined)
        scope.onUnused()
        assertFalse(scope.scope.isActive)
        val graph = RetainedAppGraph(listOf(module { single { Any() } }))
        graph.onUnused()
        graph.onRetired()
        assertFails { graph.application.koin.get<Any>() }
    }
}
