package com.ibile.features.auth

import android.view.View
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.ibile.R
import com.ibile.core.MvRxEpoxyController
import com.ibile.core.simpleController
import com.ibile.createAccount
import com.ibile.utils.extensions.navController

class SignUpFragment : AuthFragment() {

    override fun epoxyController(): MvRxEpoxyController =
        simpleController(viewModel) { (_, createAccountAsyncResult) ->
            if (createAccountAsyncResult is Success) return@simpleController handleAuthSuccess()

            createAccount {
                id("CreateAccount")
                errorMessage((createAccountAsyncResult as? Fail)?.error?.message)
                inProgress(createAccountAsyncResult is Loading)
                onClick { _, _, view, _ -> handleViewClick(view) }
                onEmailInputChange(emailInputChangeHandler)
                onPasswordInputChange(passwordInputChangeHandler)
            }
        }

    private fun handleViewClick(clickedView: View) {
        when (clickedView.id) {
            R.id.btn_navigate_sign_in -> handleSignInNavigateBtnClick()
            R.id.btn_create_account -> handleCreateAccountBtnClick()
            R.id.btn_create_account_google -> authWithGoogle()
        }
    }

    private fun handleCreateAccountBtnClick() {
        val (email, password) = viewModel.state.formData
        if (email.isBlank() || password.isBlank()) return
        viewModel.createAccount(email, password)
    }

    private fun handleSignInNavigateBtnClick() {
        val direction = SignUpFragmentDirections.actionSignUpFragmentToSignInFragment()
        navController.navigate(direction)
    }
}
