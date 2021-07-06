package com.ibile.features.auth

import android.os.Bundle
import android.util.Log
import android.view.View
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.ibile.R
import com.ibile.core.MvRxEpoxyController
import com.ibile.core.simpleController
import com.ibile.signIn
import com.ibile.utils.extensions.navController

class SignInFragment : AuthFragment() {

    private var email: String? = null

    override fun epoxyController(): MvRxEpoxyController =
        simpleController(viewModel) { (_, signInAsyncResult) ->
            if (signInAsyncResult is Success) //return@simpleController
            handleAuthSuccess(
                true,
                email ?: ""
            )

            signIn {
                id("FragmentSignIn")
                errorMessage((signInAsyncResult as? Fail)?.error?.message)
                inProgress(signInAsyncResult is Loading)
                onClick { _, _, view, _ -> handleViewClick(view) }
                onEmailInputChange(emailInputChangeHandler)
                onPasswordInputChange(passwordInputChangeHandler)
            }
        }

    private fun handleViewClick(clickedView: View) {
        when (clickedView.id) {
            R.id.btn_sign_in -> handleSignInBtnClick()
            R.id.btn_sign_in_google -> authWithGoogle()
            R.id.btn_navigate_create_account -> handleCreateAccountNavigateBtn()
        }
    }

    private fun handleSignInBtnClick() {
        val (email, password) = viewModel.state.formData
        if (email.isBlank() || password.isBlank()) return
        this.email = email
        viewModel.signInAccount(email, password)
    }

    private fun handleCreateAccountNavigateBtn() {
        val direction = SignInFragmentDirections.actionSignInFragmentToSignUpFragment()
        navController.navigate(direction)
    }
}
