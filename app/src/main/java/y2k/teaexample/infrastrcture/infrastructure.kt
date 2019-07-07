package y2k.teaexample.infrastrcture

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import y2k.tea.TeaRuntime
import y2k.tea.TeaView
import y2k.teaexample.TodoList
import y2k.teaexample.TodoList.Model
import y2k.virtual.ui.*
import y2k.virtual.ui.common.editableView
import y2k.virtual.ui.remote.HotReloadServer
import java.io.Closeable
import kotlin.properties.Delegates

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

fun textField(
    text: String,
    onTextChanged: (String) -> Unit,
    config: AppCompatEditText_.() -> Unit
) {
    editableView {
        this.onTextChanged = { onTextChanged(it.toString()) }
        this.text = text
        nodes = {
            appCompatEditText(config)
        }
    }
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

object RecycleViewModule {

    fun <T> flatRecyclerView(f: MyRecycleView_<T>.() -> Unit): MyRecycleView_<T> {
        val x = MyRecycleView_<T>()
        globalViewStack.push(x)
        x.f()
        globalViewStack.pop()
        globalViewStack.lastOrNull()?.children?.add(x)
        return x
    }

    @Suppress("ClassName")
    class MyRecycleView_<T> : RecyclerView_() {

        var items: List<T>
            @Deprecated("", level = DeprecationLevel.HIDDEN)
            get() = throw IllegalStateException()
            set(value) = updateProp(false, 3000, value)

        var idMapping: (T, Int) -> Long
            @Deprecated("", level = DeprecationLevel.HIDDEN)
            get() = throw IllegalStateException()
            set(value) = updateProp(false, 3001, value)

        var mapping: (T) -> VirtualNode
            @Deprecated("", level = DeprecationLevel.HIDDEN)
            get() = throw IllegalStateException()
            set(value) = updateProp(false, 3002, value)

        override fun createEmpty(context: Context) = MyRecycleView(context)

        @Suppress("UNCHECKED_CAST")
        override fun update(p: Property, a: View) {
            val view = a as MyRecycleView
            when (p.propId) {
                3000 -> view.items = p.value as List<Any>
                3001 -> view.idMapping = p.value as (Any, Int) -> Long
                3002 -> view.mapping = p.value as (Any) -> VirtualNode
            }
        }
    }

    class MyRecycleView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
    ) : RecyclerView(context, attrs, defStyleAttr) {

        private val adapter = object : RecyclerView.Adapter<ViewHolder>() {

            init {
                setHasStableIds(true)
            }

            override fun getItemCount(): Int = items.size

            override fun getItemId(position: Int): Long = idMapping(items[position], position)

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
                object : ViewHolder(VirtualHostView(parent.context)) {}

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                val vh = holder.itemView as VirtualHostView
                vh.update(mkNode {
                    frameLayout {
                        layoutParams = ViewGroup.LayoutParams(-1, -2)
                        mapping(items[position])
                    }
                })
            }
        }

        var items: List<Any> by Delegates.observable(emptyList()) { _, _, _ ->
            adapter.notifyDataSetChanged()
        }

        var idMapping: (Any, Int) -> Long by Delegates.observable({ _, p -> p.toLong() }) { _, _, _ ->
            adapter.notifyDataSetChanged()
        }

        var mapping: (Any) -> VirtualNode by Delegates.observable({ _ -> TODO() }) { _, _, _ ->
            adapter.notifyDataSetChanged()
        }

        init {
            layoutManager = LinearLayoutManager(null)
            setAdapter(adapter)
        }
    }
}
