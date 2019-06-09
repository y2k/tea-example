package y2k.teaexample

import y2k.teaexample.TodoList.Model
import y2k.teaexample.TodoList.Msg

class ExampleTests {

    fun test() {
        val originModel = Model()

        val (m, cmd) = TodoList.update(originModel, Msg.AddClicked)

        assert(m == originModel)
    }
}
