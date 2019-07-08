package com.template.webserver

import com.template.flows.StartGameFlow
import com.template.flows.SubmitTurnFlow
import com.template.states.BoardState
import net.corda.core.crypto.internal.providerMap
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import net.corda.nodeapi.internal.config.toConfigValue
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sun.misc.IOUtils
import java.io.InputStream
import java.nio.charset.Charset
import javax.servlet.http.HttpServletRequest

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }
    private val proxy = rpc.proxy

    @GetMapping(value = ["me"], produces = ["text/plain"])
    private fun me() = proxy.nodeInfo().legalIdentities.single().name.toString()

    @GetMapping(value = ["my-turn"])
    private fun myTurn(): Boolean = proxy.vaultQueryBy<BoardState>().states.single().state.data.getCurrentPlayerParty().name == proxy.nodeInfo().legalIdentities.single().name

    @GetMapping(value = ["my-board"], produces = ["text/plain"])
    private fun myBoard(): String {
        val boardState = proxy.vaultQueryBy<BoardState>().states.single().state.data
        var str = ""
        if (boardState.playerO == proxy.nodeInfo().legalIdentities.single()) str += "You are Player O\n"
        else if (boardState.playerX == proxy.nodeInfo().legalIdentities.single()) str += "You are Player X\n"
        for (charArray in boardState.board) {
            for (c in charArray) {
                str += (c + " ")
            }
            str += "\n"
        }
        if (boardState.getCurrentPlayerParty() == proxy.nodeInfo().legalIdentities.single()) str += "It's your turn!\n"
        else str += "It's not your turn!\n"
        return str
    }

    @GetMapping(value = ["you-are"], produces = ["text/plain"])
    private fun youAre(): String {
        val boardState = proxy.vaultQueryBy<BoardState>().states.single().state.data
        if (boardState.playerO == proxy.nodeInfo().legalIdentities.single()) return "You are Player O"
        else if (boardState.playerX == proxy.nodeInfo().legalIdentities.single()) return "You are Player X"
        return "Err"
    }



    @PostMapping(value = [ "start-game" ], produces = [MediaType.TEXT_PLAIN_VALUE], headers = [ "Content-Type=application/x-www-form-urlencoded" ])
    fun startGame(request: HttpServletRequest): ResponseEntity<String> {

        val otherParty = request.getParameter("otherParty") as Party // TODO


        return try {
            val signedTx = proxy.startTrackedFlow(::StartGameFlow, otherParty).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    @GetMapping(value = ["board"])
    private fun board(): List<Char> {
        return proxy.vaultQueryBy<BoardState>().states.single().state.data.board.flatMap { it.asList() }
    }


    @PostMapping(value = [ "submit-turn" ], headers = [ "Content-Type=application/json" ])
    fun submitTurn(request: HttpServletRequest): ResponseEntity<String> {
        val index = request.inputStream.readTextAndClose().toInt()
        var x = -1
        var y = -1
        when(index) {
            0 -> {x=0; y=0}
            1 -> {x=1; y=0}
            2 -> {x=2; y=0}
            3 -> {x=0; y=1}
            4 -> {x=1; y=1}
            5 -> {x=2; y=1}
            6 -> {x=0; y=2}
            7 -> {x=1; y=2}
            8 -> {x=2; y=2}
        }
        return try {
            val signedTx = proxy.startTrackedFlow(::SubmitTurnFlow, x, y).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }


    fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8): String {
        return this.bufferedReader(charset).use { it.readText() }
    }

}