package y2k.teaexample

import android.view.View
import android.widget.LinearLayout
import y2k.tea.Cmd
import y2k.tea.TeaComponent
import y2k.tea.map
import y2k.teaexample.TodoList.Model
import y2k.teaexample.TodoList.Msg
import y2k.teaexample.infrastrcture.*
import y2k.virtual.ui.appCompatButton
import y2k.virtual.ui.appCompatTextView
import y2k.virtual.ui.linearLayout

object TodoList : TeaComponent<Model, Msg> {

    data class Model(val items: List<String> = emptyList(), val newItem: String = "")
    sealed class Msg {
        class NewItemTextChanged(val text: String) : Msg()
        class TodoChanged(val items: List<String>) : Msg()
        class RemoveClicked(val text: String) : Msg()
        object AddClicked : Msg()
        class TodoAddResult(val result: Either<Exception, Unit>) : Msg() }

    override fun initialize() = Model() to Cmd.none<Msg>()

    override fun update(model: Model, msg: Msg) = when (msg) {
        Msg.AddClicked -> model.copy(newItem = "") to
                Effects.add("todo-list", mapOf("text" to model.newItem)).map { Msg.TodoAddResult(it) }
        is Msg.RemoveClicked -> model to Effects.remove("todo-list") { it.whereEqualTo("text", msg.text) }
        is Msg.TodoChanged -> model.copy(items = msg.items) to Cmd.none()
        is Msg.NewItemTextChanged -> model.copy(newItem = msg.text) to Cmd.none()
        is Msg.TodoAddResult -> when (msg.result) {
            is Either.Left -> model to Effects.showToast("Не удалось добавить элемент")
            is Either.Right -> model to Cmd.none() } }

    override fun sub() =
        Effects.subscribeCollections("todo-list")
            .map { list -> Msg.TodoChanged(list.map { it.getString("text")!! }) }

    fun UiContext.view(model: Model, dispatch: (Msg) -> Unit) =
        linearLayout {
            padding = pad(all = 8)
            orientation = LinearLayout.VERTICAL
            nodes = {
                textField(
                    text = model.newItem,
                    onTextChanged = { dispatch(Msg.NewItemTextChanged(it)) }) {
                    singleLine = true
                    hintCharSequence = "Enter text..." }
                appCompatButton {
                    onClickListener = View.OnClickListener { dispatch(Msg.AddClicked) }
                    textCharSequence = "Add" }
                staticListView(model.items) {
                    itemView(it, dispatch) } } }

    private fun UiContext.itemView(item: String, dispatch: (Msg) -> Unit) =
        linearLayout {
            nodes = {
                appCompatTextView {
                    layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                    padding = pad(vertical = 20)
                    textSizeFloat = 16f
                    textCharSequence = item }
                appCompatButton {
                    onClickListener = View.OnClickListener { dispatch(Msg.RemoveClicked(item)) }
                    textCharSequence = "Delete" } } } }
