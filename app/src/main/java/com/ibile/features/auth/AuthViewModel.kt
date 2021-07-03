package com.ibile.features.auth

import com.airbnb.mvrx.*
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.ibile.core.BaseViewModel
import com.ibile.data.repositiories.AuthRepository
import com.ibile.data.repositiories.FoldersRepository
import com.ibile.data.repositiories.MarkersRepository
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get

class AuthViewModel(
    initialState: State,
    private val markersRepository: MarkersRepository,
    private val authRepository: AuthRepository,
    private val foldersRepository: FoldersRepository
) :
    BaseViewModel<AuthViewModel.State>(initialState) {

    fun createAccount(email: String, password: String) {
        authRepository
            .createAccountWithEmailAndPassword(email, password)
            .map { Unit }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .execute { copy(authAsyncResult = it) }
    }

    fun authenticateWithGoogle(credential: AuthCredential) {
        authRepository
            .authenticateWithGoogle(credential)
            .map { Unit }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .execute { copy(authAsyncResult = it) }
    }

    fun signInAccount(email: String, password: String) {
        authRepository
            .signInUserEmailAndPassword(email, password)
            .map { Unit }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .execute { copy(authAsyncResult = it) }
    }


    fun deleteTables() {
        foldersRepository.dropFolderTable()
        markersRepository.dropMarkersTable()
    }

    val currentUser: FirebaseUser?
        get() = authRepository.currentUser

    data class State(
        val formData: FormData = FormData("", ""),
        val authAsyncResult: Async<Unit> = Uninitialized
    ) : MvRxState

    data class FormData(val email: String, val password: String)

    companion object : MvRxViewModelFactory<AuthViewModel, State> {
        override fun create(viewModelContext: ViewModelContext, state: State): AuthViewModel? {
            val fragment = (viewModelContext as FragmentViewModelContext).fragment
            return AuthViewModel(
                state,
                fragment.get(),
                fragment.get(),
                fragment.get()
            )
        }
    }

}
