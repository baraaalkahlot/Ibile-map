package com.ibile.data.repositiories

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.ibile.core.toObservable
import io.reactivex.Observable

class AuthRepository(private val firebaseAuth: FirebaseAuth) {
    fun createAccountWithEmailAndPassword(email: String, password: String): Observable<AuthResult> =
        firebaseAuth.createUserWithEmailAndPassword(email, password).toObservable()

    fun signInUserEmailAndPassword(email: String, password: String): Observable<AuthResult> =
        firebaseAuth.signInWithEmailAndPassword(email, password).toObservable()

    fun authenticateWithGoogle(credential: AuthCredential): Observable<AuthResult> {
        return firebaseAuth.signInWithCredential(credential).toObservable()
    }

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser
}
