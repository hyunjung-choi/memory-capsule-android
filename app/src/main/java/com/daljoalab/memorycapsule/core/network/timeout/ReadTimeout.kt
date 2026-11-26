package com.daljoalab.memorycapsule.core.network.timeout

import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Invocation
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ReadTimeout(val seconds: Int)

class ReadTimeoutInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val seconds = request.tag(Invocation::class.java)
            ?.method()
            ?.getAnnotation(ReadTimeout::class.java)
            ?.seconds
            ?: return chain.proceed(request)
        return chain.withReadTimeout(seconds, TimeUnit.SECONDS).proceed(request)
    }
}
