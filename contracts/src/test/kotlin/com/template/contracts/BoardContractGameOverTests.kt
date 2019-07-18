package com.template.contracts

import com.template.states.BoardState
import com.template.states.Status
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.testing.contracts.DummyState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Before
import org.junit.Test
import java.security.PublicKey

class BoardContractGameOverTests {
    private val ledgerServices = MockServices()

    class DummyCommand : TypeOnlyCommandData()

    lateinit var boardState: BoardState
    lateinit var publicKeys: List<PublicKey>
    lateinit var partyA: Party
    lateinit var partyB: Party

    @Before
    fun setup() {
        partyA = TestIdentity(CordaX500Name("PartyA","London","GB")).party
        partyB = TestIdentity(CordaX500Name("PartyB","New York","US")).party
        boardState = BoardState(partyA, partyB)
        publicKeys = boardState.participants.map {it.owningKey}
    }


    @Test
    fun mustIncludeGameOverCommand() {
        ledgerServices.ledger {
            transaction {
                input(BoardContract.ID, boardState)
                output(BoardContract.ID, boardState.copy(status = Status.GAME_OVER))
                command(publicKeys, BoardContract.Commands.GameOver())
                this.verifies()
            }
            transaction {
                input(BoardContract.ID, boardState)
                output(BoardContract.ID, boardState.copy(status = Status.GAME_OVER))
                command(publicKeys, DummyCommand())
                this.fails()
            }
        }
    }

    @Test
    fun gameOverTransactionMustHaveOneInput() {
        ledgerServices.ledger {
            transaction {
                input(BoardContract.ID, boardState)
                output(BoardContract.ID, boardState.copy(status = Status.GAME_OVER))
                command(publicKeys, BoardContract.Commands.GameOver())
                this.verifies()
            }
            transaction {
                output(BoardContract.ID, boardState.copy(status = Status.GAME_OVER))
                command(publicKeys, BoardContract.Commands.GameOver())
                this `fails with` "There should be one input state."
            }
        }
    }

    @Test
    fun gameOverTransactionMustHaveOneOutput() {
        ledgerServices.ledger {
            transaction {
                input(BoardContract.ID, boardState)
                output(BoardContract.ID, boardState.copy(status = Status.GAME_OVER))
                command(publicKeys, BoardContract.Commands.GameOver())
                this.verifies()
            }
            transaction {
                input(BoardContract.ID, boardState)
                command(publicKeys, BoardContract.Commands.GameOver())
                this `fails with` "There should be one output state."
            }
        }
    }

}