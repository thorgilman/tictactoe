import com.template.states.BoardState
import com.template.webserver.Controller
import com.template.webserver.NodeRPCConnection
import net.corda.client.rpc.RPCConnection
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import net.corda.testing.node.TestCordapp
import net.corda.testing.node.User
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.concurrent.Future
import javax.servlet.http.HttpServletRequest

class DriverBasedTest {

    private val playerXIdentity = CordaX500Name("PlayerX", "New York", "US")
    private val playerOIdentity = CordaX500Name("PlayerO", "New York", "US")
    private val observerIdentity = CordaX500Name("Observer", "New York", "US")

    // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
    private fun withDriver(test: DriverDSL.() -> Unit) = driver(DriverParameters(
            isDebug = true,
            startNodesInProcess = true,
            cordappsForAllNodes = listOf(TestCordapp.findCordapp("com.template.contracts"), TestCordapp.findCordapp("com.template.flows"))
    )) { test() }

    // Starts multiple nodes simultaneously, then waits for them all to be ready.
    private fun DriverDSL.startNodes(vararg identities: CordaX500Name): List<NodeHandle> {
        return identities.map {
            val user = User(it.organisation, "password", setOf("ALL")) // give node a generic user
            startNode(providedName = it, rpcUsers = listOf(user))
        }.map { it.getOrThrow() }
    }

    // Returns a list of initialized RPC Connections
    private fun getRpcConnection(vararg nodes: NodeHandle): List<NodeRPCConnection> {
        val rpcConnectionList = nodes.map {
            val user = it.rpcUsers.single()
            NodeRPCConnection(host = it.rpcAddress.host, username = user.username, password = user.password, rpcPort = it.rpcAddress.port)
        }
        rpcConnectionList.forEach { it.initialiseNodeRPCConnection() }
        return rpcConnectionList
    }



    @Test
    fun test () = withDriver {

        val (playerX: NodeHandle, playerO: NodeHandle, observer: NodeHandle) = startNodes(playerXIdentity, playerOIdentity, observerIdentity)

        val (playerXRpcConnection: NodeRPCConnection, playerORpcConnection: NodeRPCConnection) = getRpcConnection(playerX, playerO)
        val playerXController = Controller(playerXRpcConnection)
        val playerOController = Controller(playerORpcConnection)


        playerOController.startGame(playerXIdentity.toString(), observerIdentity.toString())

        val boardState = playerORpcConnection.proxy.vaultQueryBy<BoardState>().states[0].state.data
        assert(boardState.playerO == playerO.nodeInfo.legalIdentities.single())


        playerOController.submitTurn(0)

        val board = playerXController.getBoard()!!

    }

}
