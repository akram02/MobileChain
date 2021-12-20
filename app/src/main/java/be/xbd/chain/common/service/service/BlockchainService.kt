package be.xbd.chain.common.service.service

import be.xbd.chain.common.domain.Block
import be.xbd.chain.common.domain.Blockchain
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.lang.Exception

fun getBlockSetFromBlockchain(blockchain: Blockchain): HashSet<Block> {
    val blockSet = HashSet<Block>()
    for (chain in blockchain.blockchainArray) {
        if (chain?.block != null) {
            blockSet.add(chain.block!!)
        }
        else if (chain!=null){
            blockSet.addAll(getBlockSetFromBlockchain(chain))
        }
    }
    return blockSet
}

fun addDataToBlockchain(blockchain: Blockchain, data: Any): Block {
    return addDataToBlockchain(blockchain, data, blockchain.block!!)
}

fun addDataToBlockchain(blockchain: Blockchain, data: Any, lastBlock: Block): Block {
    var previousBlock: Block? = lastBlock
    if (previousBlock==null) {
        previousBlock = blockchain.block
    }
    val currentBlock = mineBlockFromLastBlockAndData(previousBlock!!, data)
    addBlockToBlockchain(blockchain, currentBlock)
    return currentBlock
}

fun newBlockchainWithGenesisBlock(): Blockchain {
    val blockchain = Blockchain()
    addGenesisBlockToBlockchain(blockchain)
    return blockchain
}

fun addRemoteBlockchain(remote: Blockchain, local: Blockchain): Boolean {
    if (!validChain(remote)) return false
    if (!validChain(local)) return false
    if (!isSameNonNullBlockOfTwoBlockchain(remote, local)) return false
    addOneBlockchainToAnother(from = remote, to = local)
    return true
}

fun mergeBlockchain(localServerSet: HashSet<String>, localBlockchain: Blockchain): Boolean {
    val factory = SimpleClientHttpRequestFactory()
    factory.setConnectTimeout(100)
    factory.setReadTimeout(25000)
    val restTemplate = RestTemplate(factory)
    for (server in localServerSet) {
        var remoteBlockchain: Blockchain?
        try {
            remoteBlockchain = restTemplate.getForObject("http://$server/blockchain", Blockchain::class.java)
        } catch (e: Exception) {
            println("Connection timeout for $server")
            continue
        }
        if (remoteBlockchain == null) continue
        if (!validChain(remoteBlockchain)) continue
        if (!isSameNonNullBlockOfTwoBlockchain(remoteBlockchain, localBlockchain)) continue
        addOneBlockchainToAnother(from = remoteBlockchain, to = localBlockchain)
    }

    localServerSet.forEach { server ->
        try {
            restTemplate.postForObject("http://$server/add-blockchain", localBlockchain, Boolean::class.java)
        } catch (e: Exception) {
            println("Connection timeout for $server")
        }
    }

    return true
}

fun isSameNonNullBlockOfTwoBlockchain(firstBlockchain: Blockchain, secondBlockchain: Blockchain): Boolean {
    val firstBlockSet = getBlockSetFromBlockchain(firstBlockchain)
    firstBlockSet.forEach { firstBlock ->
        val uuid = firstBlock.uuid
        val secondBlock = findBlockByUuid(secondBlockchain, uuid.replace("-", ""))
        if (secondBlock != null) {
            if (secondBlock != firstBlock) return false
        }
    }
    return true
}

fun addOneBlockchainToAnother(from: Blockchain, to: Blockchain) {
    val fromBlockSet = getBlockSetFromBlockchain(from)
    fromBlockSet.forEach { fromBlock ->
        val uuid = fromBlock.uuid
        val toBlock = findBlockByUuid(to, uuid.replace("-", ""))
        if (toBlock == null) {
            addBlockToBlockchain(to, fromBlock)
        }
    }
}

fun addGenesisBlockToBlockchain(blockchain: Blockchain) {
    blockchain.block = genesisBlock()
    addBlockToBlockchain(blockchain, blockchain.block!!)
}

fun addBlockToBlockchain(blockchain: Blockchain, block: Block) {
    addBlockToBlockchain(blockchain, block, block.uuid.replace("-", ""), 0)
}

fun addBlockToBlockchain(blockchain: Blockchain, block: Block, uuid: String, index: Int) {
    if (index==uuid.length) {
        blockchain.block = block
    }
    else {
        val chainIndex: Int = findChainArrayIndex(uuid, index)
        var chain =  blockchain.blockchainArray[chainIndex]
        if (chain == null) {
            chain = Blockchain()
            blockchain.blockchainArray[chainIndex] = chain
        }
        addBlockToBlockchain(chain, block, uuid, index+1)
    }
}

fun findBlockByUuid(blockchain: Blockchain, uuid: String): Block? {
    return findBlockByUuid(blockchain, uuid, 0)
}

fun findBlockByUuid(blockchain: Blockchain, uuid: String, index: Int): Block? {
    return if (index==uuid.length) {
        blockchain.block
    } else {
        val chainIndex = findChainArrayIndex(uuid, index)
        val chain = blockchain.blockchainArray[chainIndex]
        if (chain==null) null
        else {
            findBlockByUuid(chain, uuid, index+1)
        }
    }
}

fun findChainArrayIndex(uuid: String, index: Int): Int {
    val char = uuid[index]
    var chainIndex: Int
    if (char.isDigit()) {
        chainIndex = char.code - '0'.code
    } else {
        chainIndex = char.code - 'a'.code
        chainIndex += 10
    }
    return chainIndex
}

fun validChain(rootBlockchain: Blockchain): Boolean {
    return validChain(rootBlockchain, rootBlockchain, true)
}

fun validChain(rootBlockchain: Blockchain, blockchain: Blockchain, start: Boolean): Boolean {
    if (blockchain.block!=null && !start) {
        val previousUuid = blockchain.block!!.previousUuid
        if (previousUuid == "") {
            return blockchain.block == rootBlockchain.block
        }

        val previousBlock = findBlockByUuid(rootBlockchain, previousUuid.replace("-", ""), 0)

        if (previousBlock == null) {
            return false
        }

        val currentBlock: Block = blockchain.block!!

        return if (validLastHash(previousBlock, currentBlock) && validBlockHash(currentBlock)) {
            return true
        } else {
            false
        }
    }

    for (chain in blockchain.blockchainArray) {
        if (chain != null) {
            if (!validChain(rootBlockchain, chain, false)) return false
        }
    }
    return true
}
