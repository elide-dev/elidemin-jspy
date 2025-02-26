@file:Suppress("UnusedReceiverParameter")

package elidemin.dev.elide

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import kotlin.coroutines.CoroutineContext

private val engineCorePoolSize = 1u
private val engineThreadGroup = ThreadGroup("engine")

private object EngineThreadFactory : ThreadFactory {
    override fun newThread(r: Runnable): Thread = Thread(engineThreadGroup, r).apply {
        // nothing at this time
    }
}

private val engineThreadpool by lazy {
    Executors
        .newScheduledThreadPool(engineCorePoolSize.toInt(), EngineThreadFactory)
}

val Dispatchers.Engine: CoroutineDispatcher by lazy {
    engineThreadpool.asCoroutineDispatcher()
}

val Dispatchers.Virtual: CoroutineDispatcher by lazy {
    object : ExecutorCoroutineDispatcher(), Executor {
        override val executor: Executor get() = this

        override fun close() = error("Cannot be invoked on Dispatchers.LOOM")

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            Thread.startVirtualThread(block)
        }

        override fun execute(command: Runnable) {
            Thread.startVirtualThread(command)
        }

        override fun toString() = "Dispatchers.LOOM"
    }
}
