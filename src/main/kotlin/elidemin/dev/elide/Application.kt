package elidemin.dev.elide

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.parameters.types.uint
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.SandboxPolicy
import org.graalvm.polyglot.Source
import java.nio.file.Path

sealed class Command(protected val ctx: AppContext, name: String) : CliktCommand(name)

class JavaScript (ctx: AppContext): Command(ctx, "js") {
    private val src: Path by argument(help = "src file to run").path(
        mustExist = true,
        canBeFile = true,
        canBeDir = false,
        mustBeReadable = true,
    )

    override fun run() {
        val url = src.toUri()
        val source = Source.newBuilder("js", url.toURL()).build()
        val result = ctx.context.eval(source)
        echo(result.toString())
    }
}

class Hello (ctx: AppContext) : Command(ctx, "hello") {
	val debug by option(help = "enable debug").flag()
    val count by option(help = "number of times to print").uint().default(3u)

    override fun run() {
        if (debug) {
            echo("debug mode active")
        }
        repeat(count.toInt()) {
            echo("Hello world!")
        }
    }
}

class RootCommand : NoOpCliktCommand("labs")

///

private val _engine by lazy {
    Engine.newBuilder()
        .allowExperimentalOptions(true)
        .sandbox(SandboxPolicy.TRUSTED)
        .build()
}

private val _context by lazy {
    Context.newBuilder()
        .engine(_engine)
        .allowAllAccess(true)
        .allowNativeAccess(true)
        .allowExperimentalOptions(true)
        .build()
}

sealed interface AppContext {
    val engine: Engine
    val context: Context
}

class AppContextImpl (
    override val engine: Engine = _engine,
    override val context: Context = _context,
) : AppContext

val ctx = AppContextImpl()

fun main(args: Array<String>) {
    RootCommand()
        .subcommands(
            Hello(ctx),
            JavaScript(ctx)
        )
        .main(args)
}
