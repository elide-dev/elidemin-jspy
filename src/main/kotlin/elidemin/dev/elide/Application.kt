package elidemin.dev.elide

import io.micronaut.runtime.Micronaut.run
import io.micronaut.context.ApplicationContext
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main

class Hello (private val ctx: ApplicationContext) : CliktCommand() {
	val count = 3
    override fun run() {
        repeat(count) {
            echo("Hello world!")
        }
    }
}

fun main(args: Array<String>) {
	val context = ApplicationContext.run()
	Hello(context).main(args)
}
