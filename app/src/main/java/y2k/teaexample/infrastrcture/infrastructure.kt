package y2k.teaexample.infrastrcture

import android.app.Application
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import y2k.tea.TeaRuntime
import y2k.tea.TeaView
import y2k.teaexample.TodoList
import y2k.teaexample.TodoList.Model
import y2k.virtual.ui.*
import y2k.virtual.ui.remote.HotReloadServer
import java.io.Closeable

sealed class Either<out L, out R> {
    class Left<T>(val value: T) : Either<T, Nothing>()
    class Right<T>(val value: T) : Either<Nothing, T>()
}

suspend fun <T> catch(f: suspend () -> T): Either<Exception, T> =
    try {
        Either.Right(f())
    } catch (e: Exception) {
        Either.Left(e)
    }

class UiContext(val density: Float)

fun UiContext.pad(
    all: Int? = null,
    start: Int? = null,
    end: Int? = null,
    top: Int? = null,
    bottom: Int? = null,
    horizontal: Int? = null,
    vertical: Int? = null
): Quadruple<Int, Int, Int, Int> = Quadruple(
    (density * (start ?: horizontal ?: all ?: 0)).toInt(),
    (density * (top ?: horizontal ?: all ?: 0)).toInt(),
    (density * (end ?: vertical ?: all ?: 0)).toInt(),
    (density * (start ?: vertical ?: all ?: 0)).toInt()
)

fun <Model> staticListView(items: List<Model>, itemView: (item: Model) -> VirtualNode) {
    scrollView {
        verticalScrollBarEnabled = false
        nodes = {
            linearLayout {
                orientation = LinearLayout.VERTICAL
                nodes = {
                    items.forEach { item ->
                        itemView(item)
                    }
                }
            }
        }
    }
}

inline var ViewGroup_.nodes: () -> Unit
    get() = error("")
    set(value) {
        value()
    }

class MainActivity : AppCompatActivity(), TeaView<Model> {

    private lateinit var virtualHostView: VirtualHostView
    private lateinit var server: Closeable

    private val runtime = TeaRuntime(
        TodoList, this,
        { f -> GlobalScope.launch(Dispatchers.Main) { f() } },
        true
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        virtualHostView = VirtualHostView(this)
        setContentView(virtualHostView)
    }

    override fun view(model: Model) {
        with(TodoList) {
            val uiContext = UiContext(resources.displayMetrics.density)
            virtualHostView.update { uiContext.view(model, runtime::dispatch) }
        }
    }

    override fun onStart() {
        super.onStart()
        runtime.attach()
        server = HotReloadServer.start(virtualHostView)
    }

    override fun onStop() {
        super.onStop()
        runtime.detach()
        server.close()
    }
}

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: Application
    }
}
