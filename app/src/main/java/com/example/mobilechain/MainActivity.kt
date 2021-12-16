package com.example.mobilechain

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.mobilechain.databinding.ActivityMainBinding
import com.example.mobilechain.domain.Blockchain
import com.example.mobilechain.service.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    init {
        BLOCKCHAIN = newBlockchainWithGenesisBlock()
        cleanServerSet()
    }
    companion object {
        lateinit var BLOCKCHAIN: Blockchain
        var SERVER_SET: HashSet<String> = HashSet()

        fun cleanServerSet() {
            SERVER_SET.clear()
            SERVER_SET.add("192.168.1.163:8080")
        }

        fun cleanData() {
            BLOCKCHAIN = newBlockchainWithGenesisBlock()
        }
    }

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


                    // BlockchainController

                    get("/validate") {
                        call.respond(validChain(BLOCKCHAIN))
                    }
                    get("/clean-blockchain") {
                        cleanData()
                        call.respond(true)
                    }
                    get("/blockchain") {
                        call.respond(BLOCKCHAIN)
                    }
                    post("/add-blockchain") {
                        val remoteBlockchain = call.receive<Blockchain>()
                        call.respond(addRemoteBlockchain(remoteBlockchain, BLOCKCHAIN))
                    }
                    get("/merge-blockchain") {
                        call.respond(mergeBlockchain(SERVER_SET, BLOCKCHAIN))
                    }




                    // BlockController

                    get("/all-data") {
                        call.respond(getBlockSetFromBlockchain(BLOCKCHAIN))
                    }
                    get("/add-data") {
                        val data = call.request.queryParameters["data"]
                        call.respond(addDataToBlockchain(BLOCKCHAIN, data!!))
                    }




                    // ServerController

                    get("/clean-server") {
                        cleanServerSet()
                        call.respond(true)
                    }
                    get("/add-server") {
                        val server = call.request.queryParameters["server"]
                        SERVER_SET.add(server!!)
                        call.respond(SERVER_SET)
                    }
                    get("/all-server") {
                        call.respond(SERVER_SET)
                    }
                    post("/add-all-server") {
                        val remoteServerSet = call.receive<Set<String>>()
                        SERVER_SET.addAll(remoteServerSet)
                        call.respond(SERVER_SET)
                    }
                    get("/merge-server") {
                        call.respond(collectAndMergeServer(HashSet(SERVER_SET), SERVER_SET))
                    }
                }
            }.start(true)
        }
    }
}