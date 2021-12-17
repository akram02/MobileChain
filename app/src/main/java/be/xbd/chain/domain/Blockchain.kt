package be.xbd.chain.domain

class Blockchain{
    var block: Block? = null
    var blockchainArray = arrayOfNulls<Blockchain>(36)
}