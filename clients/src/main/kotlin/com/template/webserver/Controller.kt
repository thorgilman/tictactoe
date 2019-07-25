package com.template.webserver

import com.template.contracts.BoardContract
import com.template.flows.EndGameFlow
import com.template.flows.StartGameFlow
import com.template.flows.SubmitTurnFlow
import com.template.states.BoardState
import com.template.states.Status
import net.corda.core.identity.CordaX500Name.Companion.parse
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.InputStream
import java.nio.charset.Charset
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }
    private val proxy = rpc.proxy

    @GetMapping(value = ["get-my-turn"])
    private fun getMyTurn(): Boolean = proxy.vaultQueryBy<BoardState>().states.single().state.data.getCurrentPlayerParty().name == proxy.nodeInfo().legalIdentities.single().name

    @GetMapping(value = ["get-nodes"])
    private fun getNodes(): List<String> {
        val nodesList = proxy.networkMapSnapshot() - proxy.nodeInfo() - proxy.nodeInfoFromParty(proxy.notaryIdentities().single())!!
        return nodesList.map {  it.legalIdentitiesAndCerts.single().name.toString() }
    }

    @GetMapping(value = ["get-you-are-text"], produces = ["text/plain"])
    private fun getYouAreText(): String {
        val boardState = proxy.vaultQueryBy<BoardState>().states.single().state.data
        if (boardState.playerO == proxy.nodeInfo().legalIdentities.single()) return "You are Player O"
        else if (boardState.playerX == proxy.nodeInfo().legalIdentities.single()) return "You are Player X"
        return "Error determining player"
    }

    @GetMapping(value = ["get-board"])
    private fun getBoard(): List<Char>? {
        val states = proxy.vaultQueryBy<BoardState>().states
        if (states.isEmpty()) return emptyList()
        val boardState = states.single().state.data
        return boardState.board.flatMap { it.asList() }
    }

    @GetMapping(value = ["get-is-game-over"])
    private fun getIsGameOver(): Boolean {
        val states = proxy.vaultQueryBy<BoardState>().states
        if (states.single().state.data.status == Status.GAME_OVER) return true
        return false
    }

    @GetMapping(value = ["get-winner-text"])
    private fun getWinnerText(): String {
        val boardState = proxy.vaultQueryBy<BoardState>().states.single().state.data
        val winningParty = BoardContract.BoardUtils.getWinner(boardState)
        if (winningParty == null) return "No winner!"
        val myParty = proxy.nodeInfo().legalIdentities.single()
        if (myParty == winningParty) return "You win!"
        return "You loose!"
    }

    @PostMapping(value = ["start-game"], headers = ["Content-Type=application/json"])
    fun startGame(request: HttpServletRequest): ResponseEntity<String> {
        val cordaX500NameString = request.inputStream.readTextAndClose()
        val cordaX500Name = parse(cordaX500NameString)
        val party = proxy.wellKnownPartyFromX500Name(cordaX500Name)!!
        return try {
            val signedTx = proxy.startTrackedFlow(::StartGameFlow, party).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    @PostMapping(value = ["submit-turn"], headers = ["Content-Type=application/json"])
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

    @PostMapping(value = ["end-game"], headers = ["Content-Type=application/json"])
    fun endGame(request: HttpServletRequest): ResponseEntity<String> {
        return try {
            val signedTx = proxy.startTrackedFlow(::EndGameFlow).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")
        } catch (ex: Throwable) {
            //logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    private fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8): String {
        return this.bufferedReader(charset).use { it.readText() }
    }

}