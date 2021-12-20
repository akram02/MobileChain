package be.xbd.chain

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import be.xbd.chain.databinding.ActivityMainBinding
import be.xbd.chain.server.KtorBackgroundService
import be.xbd.chain.service.*
import be.xbd.chain.server.KtorBackgroundService.Companion.BLOCKCHAIN
import be.xbd.chain.server.KtorBackgroundService.Companion.MY_SERVER_SET
import be.xbd.chain.server.KtorBackgroundService.Companion.PORT
import be.xbd.chain.server.KtorBackgroundService.Companion.SERVER_SET
import be.xbd.chain.server.KtorBackgroundService.Companion.cleanData
import be.xbd.chain.utils.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    init {
        if (BLOCKCHAIN == null) {
            BLOCKCHAIN = newBlockchainWithGenesisBlock()
            cleanServerSet(SERVER_SET, PORT)
            MY_SERVER_SET = HashSet(SERVER_SET)
        }
    }

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fresh()
        clickHandler()
    }

    private fun actionOnService(action: Actions) {
        if (getServiceState(this) == ServiceState.STOPPED && action == Actions.STOP) return
        Intent(this, KtorBackgroundService::class.java).also {
            it.action = action.name
            log("Starting the service")
            startForegroundService(it)
        }
    }
    private fun clickHandler() {
        with(binding) {

            start.setOnClickListener {
                start.isEnabled = false
                stop.isEnabled = true
                actionOnService(Actions.START)
            }

            stop.setOnClickListener {
                stop.isEnabled = false
                start.isEnabled = true
                actionOnService(Actions.STOP)
            }

            // BlockchainController

            //get("/validate")
            validate.setOnClickListener {
                val result = validChain(BLOCKCHAIN!!)
                fresh()
                if (result) textView.text = "Data is valid"
                else textView.text = "Error!! Invalid data"
            }

            //get("/clean-blockchain")
            clearBlockchain.setOnClickListener {
                cleanData()
                fresh()
                textView.text = "Blockchain cleaned"
            }

            //get("/merge-blockchain")
            mergeData.setOnClickListener {
                mergeData.setTextColor(Color.RED)
                mergeData.text = "Merging"
                CoroutineScope(Dispatchers.IO).launch {
                    val result = mergeBlockchain(SERVER_SET, BLOCKCHAIN!!)
                    runOnUiThread {
                        fresh()
                        if (result)
                            textView.text = "Merge completed"
                        else
                            textView.text = "Error!! something went wrong"
                    }
                }
            }


            // BlockController

            //get("/all-data")
            allData.setOnClickListener {
                val result = getBlockSetFromBlockchain(BLOCKCHAIN!!)
                var str = ""
                result.forEach {
                    str += it.data
                    str += "\n"
                }
                fresh()
                textView.text = str
            }

            //get("/add-data")
            addData.setOnClickListener {
                if (addData.text=="Add Data") {
                    fresh()
                    addData.text = "Submit"
                    addData.setTextColor(Color.RED)
                    editText.visibility = View.VISIBLE
                }
                else {
                    addDataToBlockchain(BLOCKCHAIN!!, editText.text.toString())
                    fresh()
                    textView.text = "Data added to blockchain"
                }
            }

            // ServerController

            //get("/clean-server")
            cleanServer.setOnClickListener {
                cleanServerSet(SERVER_SET, PORT)
                textView.text = "Server cleaned"
            }

            //get("/add-server")
            addServer.setOnClickListener {
                if (addServer.text=="Add Server") {
                    fresh()
                    addServer.text = "Submit"
                    addServer.setTextColor(Color.RED)
                    editText.visibility = View.VISIBLE
                }
                else {
                    val text = editText.text.toString()
                    if (text.isNotEmpty()) {
                        addServer.setTextColor(Color.WHITE)
                        SERVER_SET.add(editText.text.toString())
                        fresh()
                        textView.text = "Server added"
                    }
                }
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
                    collectAndMergeServer(SERVER_SET)
                    runOnUiThread {
                        textView.text = "Merge Completed"
                        fresh()
                    }
                }
            }

            myServer.setOnClickListener {
                MY_SERVER_SET.clear()
                MY_SERVER_SET.addAll(myServerSet(PORT))
                SERVER_SET.addAll(MY_SERVER_SET)
                var str = ""
                MY_SERVER_SET.forEach {
                    str += it + "\n"
                }
                textView.text = str
            }
        }
    }

    private fun fresh() {
        with(binding) {
            arrayOf(validate, clearBlockchain, mergeData, allData, addData, cleanServer, addServer, allServer, mergeServer).forEach {
                it.setTextColor(Color.WHITE)
            }
            addServer.text = "Add Server"
            addData.text = "Add Data"
            mergeServer.text = "Merge Server"
            mergeData.text = "Merge Data"
//            mergeData.visibility = View.GONE
//            mergeServer.visibility = View.GONE
            editText.setText("")
            editText.visibility = View.GONE
            if (getServiceState(this@MainActivity) == ServiceState.STARTED) {
                start.isEnabled = false
                stop.isEnabled = true
            }
            else {
                start.isEnabled = true
                stop.isEnabled = false
            }
        }
    }
}