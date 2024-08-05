package com.amazon.connect.chat.sdk.network


// https://thinkupsoft.com/blog/retrofit-multiples-base-urls-with-annotations/

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiUrl(val url: String)
