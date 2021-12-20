package be.xbd.chain.server

import be.xbd.chain.domain.Blockchain
import be.xbd.chain.service.*
import be.xbd.chain.server.KtorBackgroundService.Companion.BLOCKCHAIN
import be.xbd.chain.server.KtorBackgroundService.Companion.PORT
import be.xbd.chain.server.KtorBackgroundService.Companion.SERVER_SET
import be.xbd.chain.server.KtorBackgroundService.Companion.cleanData
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.util.concurrent.TimeUnit

class KtorServer {
    companion object {
        var ktorServer: NettyApplicationEngine? = null
        private const val SERVER_PORT = 8080

        fun start() {
            ktorServer = getServer()
            getInstance().start(true)
        }

        fun stop() {
            getInstance().stop(0, 0, TimeUnit.MINUTES)
        }

        fun getInstance(): NettyApplicationEngine {
            if (ktorServer==null) {
                ktorServer = getServer()
            }
            return ktorServer!!
        }
        private fun getServer(): NettyApplicationEngine {
            return  embeddedServer(Netty, SERVER_PORT) {
                install(ContentNegotiation) {
                    gson { }
                }
                routing {
                    get("/") {
                        call.respond(mapOf("message" to "Hello world"))
                    }


                    // BlockchainController

                    get("/validate") {
                        call.respond(validChain(BLOCKCHAIN!!))
                    }
                    get("/clean-blockchain") {
                        cleanData()
                        call.respond(true)
                    }
                    get("/blockchain") {
                        call.respond(BLOCKCHAIN!!)
                    }
                    post("/add-blockchain") {
                        val remoteBlockchain = call.receive<Blockchain>()
                        call.respond(addRemoteBlockchain(remoteBlockchain, BLOCKCHAIN!!))
                    }
                    get("/merge-blockchain") {
                        call.respond(mergeBlockchain(SERVER_SET, BLOCKCHAIN!!))
                    }


                    // BlockController

                    get("/all-data") {
                        call.respond(getBlockSetFromBlockchain(BLOCKCHAIN!!))
                    }
                    get("/add-data") {
                        val data = call.request.queryParameters["data"]
                        call.respond(addDataToBlockchain(BLOCKCHAIN!!, data!!))
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
            }
        }
    }
}