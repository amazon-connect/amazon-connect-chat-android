package com.amazon.connect.chat.sdk.network

import com.amazon.connect.chat.sdk.network.api.ApiUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit

@RunWith(RobolectricTestRunner::class)
class RetrofitServiceCreatorTest {

    @Mock
    private lateinit var mockRetrofitBuilder: Retrofit.Builder

    @Mock
    private lateinit var mockRetrofit: Retrofit

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(mockRetrofitBuilder.baseUrl(org.mockito.ArgumentMatchers.anyString())).thenReturn(mockRetrofitBuilder)
        `when`(mockRetrofitBuilder.build()).thenReturn(mockRetrofit)
    }

    @Test
    fun `test createService with explicit URL`() {
        val explicitUrl = "https://explicit-url.com/api/"
        val mockService = TestService()
        `when`(mockRetrofit.create(TestService::class.java)).thenReturn(mockService)

        val service = RetrofitServiceCreator.createService(TestService::class.java, mockRetrofitBuilder, explicitUrl)

        verify(mockRetrofitBuilder).baseUrl(explicitUrl)
        verify(mockRetrofit).create(TestService::class.java)
        assertNotNull(service)
        assertEquals(mockService, service)
    }

    @Test
    fun `test createService with annotated service class`() {
        val mockService = AnnotatedTestService()
        `when`(mockRetrofit.create(AnnotatedTestService::class.java)).thenReturn(mockService)

        val service = RetrofitServiceCreator.createService(AnnotatedTestService::class.java, mockRetrofitBuilder)

        verify(mockRetrofitBuilder).baseUrl("https://annotated-url.com/api/")
        verify(mockRetrofit).create(AnnotatedTestService::class.java)
        assertNotNull(service)
        assertEquals(mockService, service)
    }

    @Test
    fun `test createService with default URL`() {
        val mockService = TestService()
        `when`(mockRetrofit.create(TestService::class.java)).thenReturn(mockService)

        val service = RetrofitServiceCreator.createService(TestService::class.java, mockRetrofitBuilder)

        verify(mockRetrofitBuilder).baseUrl("https://www.example.com/v1/")
        verify(mockRetrofit).create(TestService::class.java)
        assertNotNull(service)
        assertEquals(mockService, service)
    }

    @Test
    fun `test createService prioritizes explicit URL over annotation`() {
        val explicitUrl = "https://explicit-url.com/api/"
        val mockService = AnnotatedTestService()
        `when`(mockRetrofit.create(AnnotatedTestService::class.java)).thenReturn(mockService)

        val service = RetrofitServiceCreator.createService(AnnotatedTestService::class.java, mockRetrofitBuilder, explicitUrl)

        verify(mockRetrofitBuilder).baseUrl(explicitUrl)
        verify(mockRetrofit).create(AnnotatedTestService::class.java)
        assertNotNull(service)
        assertEquals(mockService, service)
    }

    @Test
    fun `test createService with null URL uses annotation or default`() {
        val mockService = AnnotatedTestService()
        `when`(mockRetrofit.create(AnnotatedTestService::class.java)).thenReturn(mockService)

        val service = RetrofitServiceCreator.createService(AnnotatedTestService::class.java, mockRetrofitBuilder, null)

        verify(mockRetrofitBuilder).baseUrl("https://annotated-url.com/api/")
        verify(mockRetrofit).create(AnnotatedTestService::class.java)
        assertNotNull(service)
        assertEquals(mockService, service)
    }

    // Test service classes
    open class TestService
    
    @ApiUrl("https://annotated-url.com/api/")
    open class AnnotatedTestService
}