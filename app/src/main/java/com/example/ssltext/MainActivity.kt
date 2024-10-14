package com.example.ssltext

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class MainActivity : AppCompatActivity() {

    private lateinit var textResponse: TextView

    val hostnameVerifier = HostnameVerifier { hostname, session ->
        val expectedHost = "httpbin.org"
        hostname == expectedHost
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textResponse = findViewById(R.id.text_response)

        val buttonRequest: Button = findViewById(R.id.button_request)
        buttonRequest.setOnClickListener {
            makeHttpsRequest()
        }
    }

    private fun makeHttpsRequest() {
        Thread {
            try {
                val client = createClient()
                val request = Request.Builder()
                    .url("https://httpbin.org/get")
                    .build()

                val response: Response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: "No response"

                runOnUiThread {
                    textResponse.text = responseBody
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    textResponse.text = "Error: ${e.message}"
                }
            }
        }.start()
    }

    private fun createClient(): OkHttpClient {
        // Load the SSL certificate
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val inputStream: InputStream = resources.openRawResource(R.raw.httpbin)
        val certificate: X509Certificate = certificateFactory.generateCertificate(inputStream) as X509Certificate

        // Create a KeyStore containing our trusted CAs
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null) // Initialize an empty keystore
            setCertificateEntry("ca", certificate) // Add our certificate
        }

        // Create a TrustManager that trusts the CAs in our KeyStore
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore)
        }

        // Create an SSLContext that uses our TrustManager
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustManagerFactory.trustManagers, null)
        }

        // Create a HostnameVerifier
        val hostnameVerifier = HostnameVerifier { hostname, session ->
            val expectedHost = "httpbin.org"
            hostname.equals(expectedHost, ignoreCase = true)
        }

        // Create an OkHttpClient with the SSL configuration and HostnameVerifier
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManagerFactory.trustManagers[0] as X509TrustManager)
            .hostnameVerifier(hostnameVerifier) 
            .build()
    }
}
