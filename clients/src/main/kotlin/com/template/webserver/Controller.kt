package com.template.webserver

import com.template.flows.StartGameFlow
import com.template.flows.SubmitTurnFlow
import com.template.states.BoardState
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
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


    @GetMapping(value = ["turn"], produces = arrayOf("text/plain"))
    private fun turn() = proxy.vaultQueryBy<BoardState>().states.single().state.data.getCurrentPlayerParty().toString()


    @GetMapping(value = ["me"], produces = arrayOf("text/plain"))
    private fun me() = proxy.nodeInfo().legalIdentities.single().name.toString()


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


    @PostMapping(value = [ "submit-turn" ], produces = [MediaType.TEXT_PLAIN_VALUE], headers = [ "Content-Type=application/x-www-form-urlencoded" ])
    fun submitTurn(request: HttpServletRequest): ResponseEntity<String> {

        val makeMoveX = request.getParameter("makeMoveX").toInt()
        val makeMoveY = request.getParameter("makeMoveY").toInt()

        if (makeMoveX !in 0..2) return ResponseEntity.badRequest().body("Query parameter 'makeMoveX' must an integer 0-2.\n")
        if (makeMoveY !in 0..2) return ResponseEntity.badRequest().body("Query parameter 'makeMoveY' must an integer 0-2.\n")

        return try {
            val signedTx = proxy.startTrackedFlow(::SubmitTurnFlow, makeMoveX, makeMoveY).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }





}