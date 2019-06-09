package y2k.teaexample

import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import y2k.tea.Cmd
import y2k.tea.Sub
import y2k.teaexample.infrastrcture.App
import y2k.teaexample.infrastrcture.Either
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object Effects {

    init {
        FirebaseApp.initializeApp(App.instance)
    }

    fun showToast(message: String): Cmd<Nothing> = Cmd.ofAction {
        Toast.makeText(App.instance, message, Toast.LENGTH_LONG).show()
    }

    fun add(collection: String, item: Map<String, Any>): Cmd<Either<Exception, Unit>> =
        Cmd.ofFunc {
            suspendCoroutine<Either<Exception, Unit>> { continuation ->
                getCollection(collection)
                    .add(item)
                    .addOnSuccessListener { continuation.resume(Either.Right(Unit)) }
                    .addOnFailureListener { continuation.resume(Either.Left(it)) }
            }
        }

    fun remove(collection: String, filter: (Query) -> Query): Cmd<Nothing> = Cmd.ofAction {
        getCollection(collection)
            .let(filter)
            .get()
            .addOnSuccessListener { it.forEach { it.reference.delete() } }
    }

    fun subscribeCollections(collection: String): Sub<List<DocumentSnapshot>> =
        Sub.ofFunc { dispatch ->
            val listenerRegistration = getCollection(collection)
                .addSnapshotListener { s, e ->
                    require(e == null) { e!! }
                    dispatch(s!!.documents)
                }
            Closeable { listenerRegistration.remove() }
        }

    private fun getCollection(collection: String): CollectionReference =
        FirebaseApp.getInstance().let(FirebaseFirestore::getInstance).collection(collection)
}
