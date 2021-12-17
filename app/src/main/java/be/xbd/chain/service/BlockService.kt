package be.xbd.chain.service

import be.xbd.chain.domain.Block
import be.xbd.chain.utils.toSha256
import java.time.Instant
import java.util.*


fun blockHash(block: Block) = "${block.timestamp}:${block.lastHash}:${block.uuid}:${block.data}".toSha256()

fun validBlockHash(block: Block): Boolean {
    return block.hash == blockHash(block)
}

fun validLastHash(previousBlock: Block, currentBlock: Block): Boolean {
    return currentBlock.lastHash == previousBlock.hash
}

fun mineBlockFromLastBlockAndData(lastBlock: Block, data: Any): Block {
    return newBlock(Instant.now(), lastBlock.hash, data, UUID.randomUUID().toString(), lastBlock.uuid)
}

fun genesisBlock(): Block {
    val now = Instant.ofEpochMilli(1_599_909_623_805_627)
    return newBlock(now, "-", "genesis data", "8b02fea6-aa9d-4e53-9ba1-2882a9c3579e", "")
}

fun newBlock(timestamp: Instant, lastHash: String, data: Any, uuid: String, previousUuid: String): Block {
    val block = Block()
    block.timestamp = timestamp.toEpochMilli().toString()
    block.lastHash = lastHash
    block.data = data
    block.uuid = uuid
    block.previousUuid = previousUuid
    block.hash = blockHash(block)
    return block
}