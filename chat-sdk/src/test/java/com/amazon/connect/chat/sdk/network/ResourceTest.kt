package com.amazon.connect.chat.sdk.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ResourceTest {

    @Test
    fun `test Success constructor sets data correctly`() {
        val testData = "test data"
        
        val resource = Resource.Success(testData)
        
        assertEquals(testData, resource.data)
        assertNull(resource.message)
    }

    @Test
    fun `test Error constructor sets message and data correctly`() {
        val testMessage = "error message"
        val testData = "test data"
        
        val resource = Resource.Error(testMessage, testData)
        
        assertEquals(testMessage, resource.message)
        assertEquals(testData, resource.data)
    }

    @Test
    fun `test Error constructor with null data`() {
        val testMessage = "error message"
        
        val resource = Resource.Error<String>(testMessage)
    
        assertEquals(testMessage, resource.message)
        assertNull(resource.data)
    }

    @Test
    fun `test Loading constructor with data`() {
        val testData = "test data"
        
        val resource = Resource.Loading(testData)
        
        assertEquals(testData, resource.data)
        assertNull(resource.message)
    }

    @Test
    fun `test Loading constructor with null data`() {
        val resource = Resource.Loading<String>()
        
        assertNull(resource.data)
        assertNull(resource.message)
    }

    @Test
    fun `test Resource is sealed class with correct subclasses`() {
        val success = Resource.Success("data")
        val error = Resource.Error<String>("error")
        val loading = Resource.Loading<String>()
        
        assert(success is Resource<*>)
        assert(error is Resource<*>)
        assert(loading is Resource<*>)
    }

    @Test
    fun `test type safety with different data types`() {
        // Test with String
        val stringSuccess = Resource.Success("string data")
        assertEquals("string data", stringSuccess.data)
        
        // Test with Int
        val intSuccess = Resource.Success(42)
        assertEquals(42, intSuccess.data)
        
        // Test with custom class
        val customData = TestData("name", 25)
        val customSuccess = Resource.Success(customData)
        assertEquals(customData, customSuccess.data)
    }

    @Test
    fun `test when expression with Resource`() {
        val successResource: Resource<String> = Resource.Success("success")
        val errorResource: Resource<String> = Resource.Error("error")
        val loadingResource: Resource<String> = Resource.Loading()
        
        val successResult = when (successResource) {
            is Resource.Success -> "Success: ${successResource.data}"
            is Resource.Error -> "Error: ${successResource.message}"
            is Resource.Loading -> "Loading"
        }
        
        val errorResult = when (errorResource) {
            is Resource.Success -> "Success: ${errorResource.data}"
            is Resource.Error -> "Error: ${errorResource.message}"
            is Resource.Loading -> "Loading"
        }
        
        val loadingResult = when (loadingResource) {
            is Resource.Success -> "Success: ${loadingResource.data}"
            is Resource.Error -> "Error: ${loadingResource.message}"
            is Resource.Loading -> "Loading"
        }
        
        assertEquals("Success: success", successResult)
        assertEquals("Error: error", errorResult)
        assertEquals("Loading", loadingResult)
    }
    
    // Helper class
    data class TestData(val name: String, val age: Int)
}