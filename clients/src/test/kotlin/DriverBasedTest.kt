import com.template.webserver.Controller
import com.template.webserver.NodeRPCConnection
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import net.corda.testing.node.TestCordapp
import net.corda.testing.node.User
import org.junit.Test
import org.mockito.Mockito.mock
import javax.servlet.http.HttpServletRequest

class DriverBasedTest {

    @Test
    fun test () {

        driver(DriverParameters(startNodesInProcess = true, cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.template.contracts"),
                TestCordapp.findCordapp("com.template.flows")))) {

            val userX = User("playerX", "testPassword1", permissions = setOf("ALL"))
            val userO = User("playerO", "testPassword2", permissions = setOf("ALL"))

            val (playerX: NodeHandle, playerO: NodeHandle) = listOf(
                    startNode(providedName = CordaX500Name("PlayerX", "New York", "US"), rpcUsers = listOf(userX)),
                    startNode(providedName = CordaX500Name("PlayerO", "New York", "US"), rpcUsers = listOf(userO))
            ).map { it.getOrThrow() }



            // Create RPC Connection & Initialize Controller
            val playerXCordaRpcConnection = NodeRPCConnection(host = playerX.rpcAddress.host, username = userX.username, password = userX.password, rpcPort = playerX.rpcAddress.port)
            playerXCordaRpcConnection.initialiseNodeRPCConnection()
            val playerXController = Controller(playerXCordaRpcConnection)

            val playerOCordaRpcConnection = NodeRPCConnection(host = playerO.rpcAddress.host, username = userO.username, password = userO.password, rpcPort = playerO.rpcAddress.port)
            playerOCordaRpcConnection.initialiseNodeRPCConnection()
            val playerOController = Controller(playerXCordaRpcConnection)


            playerXController.startGame("O=PlayerO,L=New York,C=US")

            playerXController.submitTurn(0)


            val board = playerXController.getBoard()!!

            board.forEach{e -> println(e + " ")}








        }

//    // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
//    private fun withDriver(test: DriverDSL.() -> Unit) = driver(DriverParameters(isDebug = true, startNodesInProcess = true)) { test() }
//
//    // Makes an RPC call to retrieve another node's name from the network map.
//    private fun NodeHandle.resolveName(name: CordaX500Name) = rpc.wellKnownPartyFromX500Name(name)!!.name
//
//    // Resolves a list of futures to a list of the promised values.
//    private fun <T> List<Future<T>>.waitForAll(): List<T> = map { it.getOrThrow() }
//
//    // Starts multiple nodes simultaneously, then waits for them all to be ready.
//    private fun DriverDSL.startNodes(vararg identities: TestIdentity) = identities
//            .map { startNode(providedName = it.name) }
//            .waitForAll()
    }
}
