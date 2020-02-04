package com.ibile.core

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import androidx.core.content.ContextCompat
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

fun Drawable.setColorFilter(color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        colorFilter = BlendModeColorFilter(color, BlendMode.SRC_IN)
    } else {
        @Suppress("DEPRECATION")
        setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
}

fun Drawable.toGrayScale(): Drawable? {
    val drawable = constantState?.newDrawable()?.mutate()
    return drawable?.apply { setColorFilter(Color.GRAY) }
}

fun Context.bitmapFromVectorDrawable(vectorResId: Int): Bitmap? {
    return ContextCompat.getDrawable(this, vectorResId)?.run {
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap))
        return bitmap
    }
}

fun Context.bitmapFromVectorDrawable(vectorResId: Int, color: Int): Bitmap? {
    return ContextCompat.getDrawable(this, vectorResId)?.run {
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        setColorFilter(color)
        val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap))
        return bitmap
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
