package com.ibile.features.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.text.Editable
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.fragmentViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.maps.model.LatLng
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ibile.AuthGraphDirections
import com.ibile.BuildConfig
import com.ibile.USERS_COLLECTION
import com.ibile.USERS_MARKERS
import com.ibile.core.BaseFragment
import com.ibile.core.currentContext
import com.ibile.data.database.entities.ConvertedFirebaseMarker
import com.ibile.data.database.entities.Folder
import com.ibile.data.database.entities.Marker
import com.ibile.features.main.addfolder.AddFolderViewModel
import com.ibile.features.main.addmarkerpoi.AddMarkerPoiViewModel
import com.ibile.utils.extensions.navController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

abstract class AuthFragment : BaseFragment() {

    val viewModel: AuthViewModel by fragmentViewModel()
    private val folderViewModel: AddFolderViewModel by fragmentViewModel()
    private val addMarkerPoiViewModel: AddMarkerPoiViewModel by fragmentViewModel()
    val db = Firebase.firestore.collection("users")
    private var index = 0


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_GOOGLE_SIGN_IN -> {
                if (resultCode != Activity.RESULT_OK) return
                handleGoogleSignInResult(GoogleSignIn.getSignedInAccountFromIntent(data))
            }
        }
    }

    private fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account!!.idToken, null)
            viewModel.authenticateWithGoogle(credential)
        } catch (exception: ApiException) {
            viewModel.updateState { copy(authAsyncResult = Fail(exception)) }
            exception.printStackTrace()
        }
    }

    protected fun authWithGoogle() {
        val googleSignInOptions = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_SIGN_IN_CLIENT_ID)
            .build()
        val signInIntent = GoogleSignIn.getClient(currentContext, googleSignInOptions).signInIntent
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
    }

    protected fun handleAuthSuccess(isLogin: Boolean, email: String) {

        val sharedPreferences: SharedPreferences =
            requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("user_email", email).apply()


        if (email.isBlank())
            return


        if (isLogin) {
            db.document(email).get()
                .addOnSuccessListener { result ->
                    // Log.d(TAG, "${document.id} => ${document.data}")
                    if (result.get("isActive") == true) {
                        CoroutineScope(IO).launch {
                            viewModel.deleteTables()
                        }
                        cloneDataFromFirebase()
                    } else {
                        showAlertDialog(" Sign in is pending manual approval.\n Pls contact the App admin to approve Sign in faster. \nAdmin contact details\n PHONE: +2348059612889 (WhatsApp Messages or Calls)\n EMAIL: olayenieo@gmail.com")
                    }
                }
                .addOnFailureListener { exception ->
                    //   Log.w(TAG, "Error getting documents.", exception)

                }

        } else {

            db.document(email)
                .set(hashMapOf("isActive" to false)).addOnSuccessListener {
                    showAlertDialog("the account registration is pending authorization from developer. \n contact developer to approve registration faster (contact details:- olayenieo@gmail.com or +2348059612889)")

                }.addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Error while making auth,please try again",
                        Toast.LENGTH_LONG
                    ).show()
                }

        }


    }


    // Clone data from Firebase
    private fun cloneDataFromFirebase() {

        val userEmail = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE)
            .getString("user_email", "empty")

        val db = FirebaseFirestore.getInstance()
        val mainCollection: CollectionReference = db.collection(USERS_COLLECTION)
            .document(userEmail!!)
            .collection("file")

        val folders = ArrayList<Folder>()
        mainCollection.get()
            .addOnCompleteListener { values ->
                for (item: QueryDocumentSnapshot in values.result!!) {
                    val folder = item.toObject(Folder::class.java)
                    folders.add(folder)
                    folderViewModel.addFolderToRoomOnly(folder)
                }

                loopThrowFolders(folders, mainCollection)


                val direction = AuthGraphDirections.actionGlobalMainGraph()
                navController.navigate(direction)
            }
    }


    private fun loopThrowFolders(folders: ArrayList<Folder>, mainCollection: CollectionReference) {

        if (index > folders.size - 1) {
            return

        }

        Log.d(
            "wasd",
            "loopThrowFolders: start index = $index  folder id = ${folders[index].id}"
        )

        mainCollection
            .document(folders[index].id.toString())
            .collection(USERS_MARKERS)
            .get()
            .addOnCompleteListener { values ->
                if (values.isSuccessful) {

                    for (item: QueryDocumentSnapshot in values.result!!) {

                        val marker = item.toObject(ConvertedFirebaseMarker::class.java)


                        Log.d(
                            "wasd",
                            "loopThrowFolders: process index = $index  folder id = ${folders[index].id} marker id = ${marker.id}"
                        )

                        val arrayOfGeoPoint: ArrayList<LatLng> = arrayListOf()
                        for (p in marker.points) {
                            arrayOfGeoPoint.add(LatLng(p.latitude, p.longitude))
                        }

                        val arrayOfImagePath: ArrayList<Uri> = arrayListOf()
                        for (i in marker.imageUris) {
                            arrayOfImagePath.add(Uri.parse(i))
                        }

                        val roomMarker =
                            Marker(
                                id = marker.id!!,
                                points = arrayOfGeoPoint,
                                type = marker.type,
                                createdAt = marker.createdAt,
                                updatedAt = marker.updatedAt,
                                description = marker.description,
                                color = marker.color,
                                icon = marker.icon?.let { it1 -> Marker.Icon(it1) },
                                phoneNumber = marker.phoneNumber,
                                imageUris = arrayOfImagePath,
                                folderId = marker.folderId
                            )

                        Log.d(
                            "wasd",
                            "loopThrowFolders: point = ${marker.points[0]} and the icon  = ${marker.icon}"
                        )
                        addMarkerPoiViewModel.addMarkerToRoomOnly(roomMarker)
                    }

                    Log.d(
                        "wasd",
                        "loopThrowFolders: end index = $index  folder id = ${folders[index].id}"
                    )

                    index++
                    loopThrowFolders(
                        folders, mainCollection
                    )

                }

            }
    }


    private fun showAlertDialog(msg: String) {

        AlertDialog.Builder(requireContext())
            .setTitle("Alert!!")
            .setMessage(msg)
            .setPositiveButton(android.R.string.ok, null)
            .create().show()

    }


    protected val emailInputChangeHandler = { editable: Editable ->
        val email = editable.toString().trim()
        viewModel.updateState {
            copy(formData = formData.copy(email = email), authAsyncResult = Uninitialized)
        }
    }

    protected val passwordInputChangeHandler = { editable: Editable ->
        val password = editable.toString().trim()
        viewModel.updateState {
            copy(formData = formData.copy(password = password), authAsyncResult = Uninitialized)
        }
    }

    companion object {
        private const val RC_GOOGLE_SIGN_IN = 1001
    }
}
