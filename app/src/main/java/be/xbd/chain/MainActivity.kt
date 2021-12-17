package be.xbd.chain

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import be.xbd.chain.databinding.ActivityMainBinding
import be.xbd.chain.domain.Blockchain
import be.xbd.chain.service.*
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
import java.util.*
import kotlin.collections.HashSet

class MainActivity : AppCompatActivity() {
    init {
        BLOCKCHAIN = newBlockchainWithGenesisBlock()
        cleanServerSet(SERVER_SET, PORT)
    }
    companion object {
        lateinit var BLOCKCHAIN: Blockchain
        var SERVER_SET: HashSet<String> = HashSet()
        val PORT = "8080"
        fun cleanData() {
            BLOCKCHAIN = newBlockchainWithGenesisBlock()
        }
    }

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        server()
        clickHandler()
    }

    private fun clickHandler() {
        with(binding) {

            // BlockchainController

            //get("/validate")
            validate.setOnClickListener {
                val result = validChain(BLOCKCHAIN)
                if (result) textView.text = "Data is valid"
                else textView.text = "Error!! Invalid data"
            }

            //get("/clean-blockchain")
            clearBlockchain.setOnClickListener {
                cleanData()
            }

            //get("/merge-blockchain")
            mergeData.setOnClickListener {
                mergeData.setTextColor(Color.RED)
                mergeData.text = "Merging"
                CoroutineScope(Dispatchers.IO).launch {
                    val result = mergeBlockchain(SERVER_SET, BLOCKCHAIN)
                    runOnUiThread {
                        mergeData.text = "Merge Completed"
                    }
                }
            }


            // BlockController

            //get("/all-data")
            allData.setOnClickListener {
                val result = getBlockSetFromBlockchain(BLOCKCHAIN)
                var str = ""
                result.forEach {
                    str += it.data
                    str += " " + Date(it.timestamp.toLong())
                }
                textView.text = str
            }

            //get("/add-data")
            addData.setOnClickListener {
                fresh()
                if (addData.text=="ADD") {
                    addData.text = "Submit"
                    addData.setTextColor(Color.RED)
                    editText.visibility = View.VISIBLE
                }
                else {
                    addDataToBlockchain(BLOCKCHAIN, editText.text.toString())
                }
                fresh()
            }

            // ServerController

            //get("/clean-server")
            cleanServer.setOnClickListener {
                cleanServerSet(SERVER_SET, PORT)
                textView.text = ""
            }

            //get("/add-server")
            addServer.setOnClickListener {
                fresh()
                if (addServer.text=="ADD SERVER") {
                    addServer.text = "Submit"
                    addServer.setTextColor(Color.RED)
                    editText.visibility = View.VISIBLE
                }
                else {
                    SERVER_SET.add(editText.text.toString())
                }
                fresh()
            }

            //get("/all-server")
            allServer.setOnClickListener {
                var str = ""
                SERVER_SET.forEach {
                    str += it + "\n"
                }
                textView.text = str
            }

            //get("/merge-server")
            mergeServer.setOnClickListener {
                mergeServer.setTextColor(Color.RED)
                mergeServer.text = "Merging"
                CoroutineScope(Dispatchers.IO).launch {
                    val result = collectAndMergeServer(SERVER_SET)
                    runOnUiThread {
                        mergeServer.text = "Merge Completed"
                    }
                }
            }
        }
    }

    private fun fresh() {
        with(binding) {
            arrayOf(validate, clearBlockchain, mergeData, allData, addData, cleanServer, addServer, allServer, mergeServer).forEach {
                it.setTextColor(Color.WHITE)
            }
            editText.visibility = View.GONE
        }
    }

    private fun server() {
        CoroutineScope(Dispatchers.IO).launch {
            embeddedServer(Netty, 8080) {
                install(ContentNegotiation) {
                    gson { }
                }
                routing {
                    get("/") {
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
                        cleanServerSet(SERVER_SET, PORT)
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
                        call.respond(collectAndMergeServer(SERVER_SET))
                    }
                }
            }.start(true)
        }
    }
}