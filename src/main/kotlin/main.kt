//@file:JvmName("sk.ktk.MainKt")
package sk.ktk

import kotlinx.cli.*
import com.fazecast.jSerialComm.SerialPort
import sun.misc.Signal
import java.nio.charset.Charset


enum class Speed(val value: Int) {
    S1200(1200),
    S2400(2400),
    S4800(4800),
    S9600(9600),
    S19200(19200),
    S38400(38400),
    S57600(57600)
}

fun convertByteArray(byteArray: ByteArray): ByteArray {

    var me: Int
    val outputArray: ByteArray = ByteArray(byteArray.size * 2)
    var index = 0
    val iteratorOut = outputArray.iterator()
    val iteratorSrc = byteArray.iterator()
    while (iteratorSrc.hasNext()) {
        var b = iteratorSrc.nextByte();
        for (i in 0 until 2) {
            me = 0 // manchester encoded txbyte
            for (j in 0 until 4) {
                me = me ushr 2
                if ((b.toInt() and 1) == 1)
                    me = me or 0b01000000 // 1->0
                else
                    me = me or 0b10000000 // 0->1
                b = (b.toInt() ushr 1).toByte()
            }
            outputArray.set(index, me.toByte())
            index++
//            putc(me);
        }
    }
    return outputArray
}

fun main(args: Array<String>) {
    val parser = ArgParser("ManTerminal")
    val port by parser.option(
        ArgType.String,
        shortName = "p",
        fullName = "port",
        description = "Port address e.g. (Win: COM1, Linux,Mac: /dev/ACM0)"
    ).required()
    val speed by parser.option(
        ArgType.Choice<Speed>(), shortName = "s",
        description = "Comunication speed"
    ).default(Speed.S9600)

    val autoSpeed by parser.option(ArgType.Boolean, shortName = "d", description = "Turn on speed detection mode")
        .default(false)
//    val eps by parser.option(ArgType.Double, description = "Observational error").default(0.01)

    parser.parse(args)
    println("Port: ${port}")
    println("Speed: ${speed.value} Bd")

    val comPort = SerialPort.getCommPort(port)

    if (comPort.openPort()) {
        print("${comPort.systemPortName} was opened!")
    } else {
        print("Canno't open ${comPort.systemPortName}!")
    }

    Signal.handle(Signal("INT")) {
        println("Closing port")
        comPort.closePort()
        kotlin.system.exitProcess(0)
    }

    with(comPort) {
        baudRate = speed.value
        numDataBits = 8
        numStopBits = SerialPort.ONE_STOP_BIT
        parity = SerialPort.NO_PARITY
    }
    while (true) {
        val readLine = readLine()?.toByteArray(Charsets.US_ASCII)
        readLine?.let {
            val manchester = convertByteArray(it)
            var i = 0
            val iterator = manchester.iterator()
            if (autoSpeed) {
                comPort.writeBytes(byteArrayOf(0.toByte()), 1)
                Thread.sleep(1)
            }
            while (iterator.hasNext()) {
                comPort.writeBytes(byteArrayOf(iterator.nextByte()), 1L/*manchester.size.toLong()*/)
                Thread.sleep(0, 100)
                i++
                if (i % 2 == 0) Thread.sleep(100)

            }

            if (autoSpeed) {
                comPort.writeBytes(byteArrayOf(0.toByte()), 1)
            }


        }
    }

}