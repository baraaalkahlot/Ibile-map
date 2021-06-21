package com.ibile.features.auth

import android.app.Activity
import android.content.Intent
import android.text.Editable
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.fragmentViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.GoogleAuthProvider
import com.ibile.AuthGraphDirections
import com.ibile.BuildConfig
import com.ibile.core.BaseFragment
import com.ibile.core.currentContext
import com.ibile.utils.extensions.navController

abstract class AuthFragment : BaseFragment() {
    val viewModel: AuthViewModel by fragmentViewModel()

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

    protected fun handleAuthSuccess() {

        val direction = AuthGraphDirections.actionGlobalMainGraph()
        navController.navigate(direction)
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
