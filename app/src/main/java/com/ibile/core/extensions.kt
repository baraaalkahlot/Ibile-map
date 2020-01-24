package com.ibile.core

import android.animation.ObjectAnimator
import android.view.View
import com.google.android.gms.tasks.Task
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.util.*

fun View.animateSlideVertical(distance: Float, duration: Long) {
    ObjectAnimator.ofFloat(this, "translationY", distance).apply {
        this.duration = duration
        start()
    }
}

fun getCurrentDateTime(): Date {
    return Calendar.getInstance().time
}

fun <T> Task<T>.toObservable(): Observable<T> {
    return Observable.create<T> { emitter: ObservableEmitter<T> ->
        this.addOnSuccessListener {
            emitter.onNext(it)
            emitter.onComplete()
        }.addOnFailureListener {
            emitter.onError(it)
        }
    }
}

fun Disposable.addTo(compositeDisposable: CompositeDisposable): Disposable =
    apply { compositeDisposable.add(this) }
