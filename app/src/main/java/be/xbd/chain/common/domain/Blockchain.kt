package be.xbd.chain.common.domain

class Blockchain{
    var block: Block? = null
    var blockchainArray = arrayOfNulls<Blockchain>(36)
}