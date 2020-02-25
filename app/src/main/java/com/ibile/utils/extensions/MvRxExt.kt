package com.ibile.utils.extensions

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success

val <T> Async<T>.isLoading: Boolean
    get() = this is Loading

val <T> Async<T>.isError: Boolean
    get() = this is Fail

val <T> Async<T>.isSuccess: Boolean
    get() = this is Success
