package elidemin.dev.elide

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main

class Hello : CliktCommand("hello") {
	val count = 3
    override fun run() {
        repeat(count) {
            echo("Hello world!")
        }
    }
}

fun main(args: Array<String>) {
	Hello().main(args)
}
