import android.content.Context
import com.amazon.connect.chat.sdk.ChatSession
import com.amazon.connect.chat.sdk.ChatSessionImpl
import com.amazon.connect.chat.sdk.di.ChatModule
import com.amazon.connect.chat.sdk.network.AWSClientImpl
import com.amazon.connect.chat.sdk.network.NetworkConnectionManager
import com.amazon.connect.chat.sdk.network.RetrofitServiceCreator.createService
import com.amazon.connect.chat.sdk.network.WebSocketManagerImpl
import com.amazon.connect.chat.sdk.network.api.APIClient
import com.amazon.connect.chat.sdk.network.api.AttachmentsInterface
import com.amazon.connect.chat.sdk.network.api.MetricsInterface
import com.amazon.connect.chat.sdk.provider.ConnectionDetailsProviderImpl
import com.amazon.connect.chat.sdk.repository.AttachmentsManager
import com.amazon.connect.chat.sdk.repository.ChatServiceImpl
import com.amazon.connect.chat.sdk.repository.MessageReceiptsManagerImpl
import com.amazon.connect.chat.sdk.repository.MetricsManager
import com.amazon.connect.chat.sdk.utils.MetricsUtils.getMetricsEndpoint
import com.amazon.connect.chat.sdk.utils.logger.SDKLogger
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ChatSessionProvider {
    @Volatile
    private var chatSession: ChatSession? = null

    // Public method for customers to get ChatSession
    fun getChatSession(context: Context): ChatSession {
        return chatSession ?: synchronized(this) {
            chatSession ?: initializeChatSession(context).also {
                chatSession = it
            }
        }
    }

    // Private method to initialize ChatSession using Hilt or manual fallback
    private fun initializeChatSession(context: Context): ChatSession {
        return if (isHiltAvailable()) {
            // Use Hilt's EntryPoint mechanism if Hilt is available
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                ChatModule.ChatSessionEntryPoint::class.java
            )
            entryPoint.getChatSession()
        } else {
            // Fallback to manual initialization
            createChatSessionManually(context)
        }
    }

    // Method to check if Hilt is available
    private fun isHiltAvailable(): Boolean {
        return try {
            Class.forName("dagger.hilt.EntryPoints")
            true
        } catch (e: ClassNotFoundException) {
            SDKLogger.logger.logDebug {"Hilt is not available"}
            false
        }
    }

    // Manual initialization of ChatSession for non-Hilt users
    private fun createChatSessionManually(context: Context): ChatSession {
        val appContext = context.applicationContext

        // Step 1: Create AWS Client
        val awsClient = AWSClientImpl.create()

        // Step 2: Create Network Connection Manager
        val networkConnectionManager = NetworkConnectionManager(appContext)

        // Step 3: Create WebSocket Manager
        val webSocketManager = WebSocketManagerImpl(
            dispatcher = Dispatchers.IO,
            networkConnectionManager = networkConnectionManager
        )

        // Step 4: Create Retrofit Builder
        val retrofitBuilder = createRetrofitBuilder()

        // Step 5: Create API Client
        val apiClient = createAPIClient(retrofitBuilder)

        // Step 6: Create Other Dependencies
        val metricsManager = MetricsManager(apiClient)
        val attachmentsManager = AttachmentsManager(appContext, awsClient, apiClient)
        val messageReceiptsManager = MessageReceiptsManagerImpl()
        val connectionDetailsProvider = ConnectionDetailsProviderImpl()

        // Step 7: Create ChatService and return ChatSessionImpl
        val chatService = ChatServiceImpl(
            appContext,
            awsClient,
            connectionDetailsProvider,
            webSocketManager,
            metricsManager,
            attachmentsManager,
            messageReceiptsManager
        )

        return ChatSessionImpl(chatService)
    }

    // Helper method to create Retrofit Builder
    private fun createRetrofitBuilder(): Retrofit.Builder {
        val okHttpClient = OkHttpClient.Builder().build()
        return Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
    }

    // Helper method to create APIClient
    private fun createAPIClient(retrofitBuilder: Retrofit.Builder): APIClient {
        val metricsInterface: MetricsInterface = createService(
            MetricsInterface::class.java,
            retrofitBuilder,
            url = getMetricsEndpoint()
        )
        val attachmentsInterface: AttachmentsInterface = createService(
            AttachmentsInterface::class.java,
            retrofitBuilder
        )
        return APIClient(metricsInterface, attachmentsInterface)
    }
}
