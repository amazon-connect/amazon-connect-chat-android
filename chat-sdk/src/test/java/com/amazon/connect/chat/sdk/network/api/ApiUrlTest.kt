package com.amazon.connect.chat.sdk.network.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ApiUrlTest {

    @Test
    fun `ApiUrl annotation stores url value correctly`() {
        @ApiUrl("https://test.example.com")
        class TestClass
        
        val annotation = TestClass::class.java.getAnnotation(ApiUrl::class.java)
        
        assertNotNull("TestClass should have ApiUrl annotation", annotation)
        assertEquals("ApiUrl should store the URL correctly", 
            "https://test.example.com", 
            annotation.url)
    }

    @Test
    fun `ApiUrl annotation can be retrieved at runtime`() {
        @ApiUrl("https://runtime.example.com")
        class RuntimeTestClass
        
        val clazz = RuntimeTestClass::class.java
        val annotation = clazz.getAnnotation(ApiUrl::class.java)
        
        assertNotNull("Should find ApiUrl annotation", annotation)
        assertEquals("ApiUrl annotation should have the correct URL", 
            "https://runtime.example.com", 
            annotation.url)
    }
}