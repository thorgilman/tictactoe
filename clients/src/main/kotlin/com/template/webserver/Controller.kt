package com.template.webserver

import com.template.contracts.BoardContract
import com.template.flows.*
import com.template.states.BoardState
import com.template.states.Status
import net.corda.core.identity.CordaX500Name.Companion.parse
import net.corda.core.identity.Party
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
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
        return proxy.startTrackedFlow(::AvailableNodesFlow).returnValue.getOrThrow()
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

    @RequestMapping(value = ["start-game"], headers = ["Content-Type=application/json"])
    fun startGame(@RequestParam(value = "party", defaultValue = "") party: String,
                  @RequestParam(value = "observer", defaultValue = "") observer: String): ResponseEntity<String> {
        return try {
            val wellKnownParty = proxy.wellKnownPartyFromX500Name(parse(party))!!
            val wellKnownObserver = proxy.wellKnownPartyFromX500Name(parse(observer))

            lateinit var signedTx: SignedTransaction
            if (wellKnownObserver == null) signedTx = proxy.startTrackedFlow(::StartGameFlow, wellKnownParty).returnValue.getOrThrow()
            else signedTx = proxy.startTrackedFlow(::StartGameFlowWithObserver, wellKnownParty, wellKnownObserver).returnValue.getOrThrow()

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

    @PostMapping(value = ["end-game"])
    fun endGame(request: HttpServletRequest): ResponseEntity<String> {
        return try {
            val signedTx = proxy.startTrackedFlow(::EndGameFlow).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    private fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8): String {
        return this.bufferedReader(charset).use { it.readText() }
    }

}