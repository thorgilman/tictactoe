package com.template.webserver

import com.fasterxml.jackson.annotation.JsonProperty
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
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.BufferedReader
import java.io.InputStream
import java.nio.charset.Charset
import java.security.Principal
import java.util.*
import javax.servlet.*
import javax.servlet.http.*
import kotlin.streams.toList

@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }
    private val proxy = rpc.proxy

    @GetMapping(value = ["get-my-turn"])
    fun getMyTurn(): Boolean = proxy.vaultQueryBy<BoardState>().states.single().state.data.getCurrentPlayerParty().name == proxy.nodeInfo().legalIdentities.single().name

    @GetMapping(value = ["get-nodes"])
    fun getNodes(): List<String> = proxy.startTrackedFlow(::AvailableNodesFlow).returnValue.getOrThrow()

    @GetMapping(value = ["get-you-are-text"], produces = ["text/plain"])
    fun getYouAreText(): String {
        val boardState = proxy.vaultQueryBy<BoardState>().states.single().state.data
        if (boardState.playerO == proxy.nodeInfo().legalIdentities.single()) return "You are Player O"
        else if (boardState.playerX == proxy.nodeInfo().legalIdentities.single()) return "You are Player X"
        return "Error determining player"
    }

    @GetMapping(value = ["get-board"])
    fun getBoard(): List<Char>? {
        val states = proxy.vaultQueryBy<BoardState>().states
        if (states.isEmpty()) return emptyList()
        val boardState = states.single().state.data
        return boardState.board.flatMap { it.asList() }
    }

    @GetMapping(value = ["get-is-game-over"])
    fun getIsGameOver(): Boolean {
        val states = proxy.vaultQueryBy<BoardState>().states
        if (states.single().state.data.status == Status.GAME_OVER) return true
        return false
    }

    @GetMapping(value = ["get-winner-text"])
    fun getWinnerText(): String {
        val boardState = proxy.vaultQueryBy<BoardState>().states.single().state.data
        val winningParty = BoardContract.BoardUtils.getWinner(boardState)
        if (winningParty == null) return "No winner!"
        val myParty = proxy.nodeInfo().legalIdentities.single()
        if (myParty == winningParty) return "You win!"
        return "You lose!"
    }


    @RequestMapping(value = ["start-game"], headers = ["Content-Type=application/json"])
    fun startGame(@RequestAttribute otherPlayerParty: String, @RequestAttribute observerParty: String): ResponseEntity<String> {
        return try {
            val wellKnownParty = proxy.wellKnownPartyFromX500Name(parse(otherPlayerParty))!!

            lateinit var signedTx: SignedTransaction
            if (observerParty.length == 0) {
                signedTx = proxy.startTrackedFlow(::StartGameFlow, wellKnownParty).returnValue.getOrThrow()
            }
            else {
                val wellKnownObserver = proxy.wellKnownPartyFromX500Name(parse(observerParty))!!
                signedTx = proxy.startTrackedFlow(::StartGameFlowWithObserver, wellKnownParty, wellKnownObserver).returnValue.getOrThrow()
            }

            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

//    @PostMapping(value = ["submit-turn"], headers = ["Content-Type=application/json"])
//    fun submitTurn(request: HttpServletRequest): ResponseEntity<String> {
//        val index = request.inputStream.readTextAndClose().toInt()

    @PostMapping(value = ["submit-turn"], headers = ["Content-Type=application/json"])
    fun submitTurn(@RequestBody index: Int): ResponseEntity<String> {

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
    fun endGame(): ResponseEntity<String> {
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



class StartGameHttpRequest(content: Object) : HttpServletRequest {

    override fun getInputStream(): ServletInputStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isUserInRole(role: String?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun startAsync(): AsyncContext {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun startAsync(servletRequest: ServletRequest?, servletResponse: ServletResponse?): AsyncContext {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPathInfo(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getProtocol(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCookies(): Array<Cookie> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getParameterMap(): MutableMap<String, Array<String>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRequestURL(): StringBuffer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAttributeNames(): Enumeration<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setCharacterEncoding(env: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getParameterValues(name: String?): Array<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRemoteAddr(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isAsyncStarted(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getContentLengthLong(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLocales(): Enumeration<Locale> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRealPath(path: String?): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun login(username: String?, password: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getContextPath(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isRequestedSessionIdValid(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getServerPort(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAttribute(name: String?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDateHeader(name: String?): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRemoteHost(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRequestedSessionId(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getServletPath(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSession(create: Boolean): HttpSession {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSession(): HttpSession {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getServerName(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLocalAddr(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isSecure(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : HttpUpgradeHandler?> upgrade(httpUpgradeHandlerClass: Class<T>?): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isRequestedSessionIdFromCookie(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPart(name: String?): Part {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRemoteUser(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLocale(): Locale {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getMethod(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isRequestedSessionIdFromURL(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLocalPort(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isRequestedSessionIdFromUrl(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getServletContext(): ServletContext {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getQueryString(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDispatcherType(): DispatcherType {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getHeaders(name: String?): Enumeration<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUserPrincipal(): Principal {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getParts(): MutableCollection<Part> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getReader(): BufferedReader {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getScheme(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun logout() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLocalName(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isAsyncSupported(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAuthType(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCharacterEncoding(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getParameterNames(): Enumeration<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun authenticate(response: HttpServletResponse?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeAttribute(name: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPathTranslated(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getContentLength(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getHeader(name: String?): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getIntHeader(name: String?): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun changeSessionId(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getContentType(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAsyncContext(): AsyncContext {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRequestURI(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRequestDispatcher(path: String?): RequestDispatcher {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getHeaderNames(): Enumeration<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setAttribute(name: String?, o: Any?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getParameter(name: String?): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRemotePort(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }



}
