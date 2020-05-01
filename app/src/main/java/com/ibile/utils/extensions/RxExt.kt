package com.ibile.utils.extensions

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

fun Completable.withDefaultScheduler(): Completable =
    subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

fun <T> Observable<T>.withDefaultScheduler(): Observable<T> =
    subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
