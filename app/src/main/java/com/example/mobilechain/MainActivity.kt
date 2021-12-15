package com.example.mobilechain

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.mobilechain.databinding.ActivityMainBinding
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        CoroutineScope(Dispatchers.IO).launch {
            embeddedServer(Netty, 8080) {
                install(ContentNegotiation) {
                    gson {  }
                }
                routing {
                    get("/") {
                        binding.tv.text = "Requesting"
                        call.respond(mapOf("message" to "Hello world"))
                    }
                }
            }.start(true)
        }
    }
}