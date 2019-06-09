package y2k.teaexample

import org.junit.Test
import y2k.teaexample.infrastrcture.UiContext
import y2k.virtual.ui.remote.HotReloadClient

class HotReloadRunners {

    @Test
    fun run() = ui {
        TodoList.Model(
            newItem = "Hello World",
            items = List(20) { "Item #$it" }
        )
    }

    private fun ui(model: () -> TodoList.Model) {
        HotReloadClient.send {
            with(TodoList) {
                UiContext(2f).view(
                    model()
                ) {}
            }
        }
    }
}
