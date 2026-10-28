// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.di

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Regression test for https://github.com/amazon-connect/amazon-connect-chat-android/issues/88.
 *
 * Hilt aggregates the SDK's modules into the application's SingletonComponent, so an unqualified
 * binding for a type the SDK does not own (OkHttpClient, Retrofit.Builder, Context, ...) collides
 * with the application's own binding for that type.
 */
class HiltModuleBindingsTest {

    private val sdkPackage = "com.amazon.connect.chat.sdk."
    private val modules = listOf(ChatModule::class.java, NetworkModule::class.java)

    // The modules contain only @Provides methods, all named provide*. Hilt is compileOnly, so its
    // annotations are not on the unit test classpath and cannot be read reflectively.
    private fun providerMethods(): List<Method> =
        modules.flatMap { it.declaredMethods.toList() }
            .filter { Modifier.isPublic(it.modifiers) && it.name.startsWith("provide") }

    private fun isSdkOwned(type: Class<*>) = type.name.startsWith(sdkPackage)

    @Test
    fun providedTypesNotOwnedBySdkAreQualified() {
        val unqualified = providerMethods()
            .filter { !isSdkOwned(it.returnType) && !it.isAnnotationPresent(ChatSdkInternal::class.java) }
            .map { "${it.declaringClass.simpleName}.${it.name}(): ${it.returnType.name}" }

        assertEquals(emptyList<String>(), unqualified)
    }

    @Test
    fun injectedTypesNotOwnedBySdkAreQualified() {
        // provideContext receives Hilt's @ApplicationContext binding, whose qualifier has CLASS retention.
        val unqualified = providerMethods().filter { it.name != "provideContext" }.flatMap { method ->
            method.parameterTypes.indices
                .filter { index -> !isSdkOwned(method.parameterTypes[index]) }
                .filter { index -> method.parameterAnnotations[index].none { it is ChatSdkInternal } }
                .map { index -> "${method.declaringClass.simpleName}.${method.name} parameter $index: ${method.parameterTypes[index].name}" }
        }

        assertEquals(emptyList<String>(), unqualified)
    }

    @Test
    fun providerMethodsAreFound() {
        assertTrue(providerMethods().size >= 10)
    }

    @Test
    fun chatSessionRemainsInjectableWithoutQualifier() {
        val chatSessionProvider = providerMethods().single { it.name == "provideChatSession" }

        assertTrue(chatSessionProvider.annotations.none { it is ChatSdkInternal })
    }
}
