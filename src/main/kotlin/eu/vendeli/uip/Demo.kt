package eu.vendeli.uip

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Usage: kotlin UniqueIpv4CounterKt <path-to-ip-file>")
        return
    }
    val counter = UniqueIpv4Counter()
    println("Processing file: ${args[0]}")
    counter.processFile(args[0])
    println("Unique IPv4 count: ${counter.getUniqueCount()}")
}