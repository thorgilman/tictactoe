package com.template.contracts

import com.template.states.BoardState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.CordaX500Name
import net.corda.testing.contracts.DummyState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Before
import org.junit.Test
import java.security.PublicKey

class BoardContractSubmitTurnTests {
    private val ledgerServices = MockServices()

    class DummyCommand : TypeOnlyCommandData()

    lateinit var boardState: BoardState
    lateinit var publicKeys: List<PublicKey>

    @Before
    fun setup() {
        val partyA = TestIdentity(CordaX500Name("PartyA","London","GB")).party
        val partyB = TestIdentity(CordaX500Name("PartyB","New York","US")).party
        boardState = BoardState(partyA, partyB)
        publicKeys = boardState.participants.map {it.owningKey}
    }


    @Test
    fun mustIncludeMakeMoveCommand() {
        ledgerServices.ledger {
            transaction {
                input(BoardContract.ID, boardState)
                output(BoardContract.ID, boardState.returnNewBoardAfterMove(Pair(0,0)))
                command(publicKeys, BoardContract.Commands.SubmitTurn())
                this.verifies()
            }
            transaction {
                input(BoardContract.ID, boardState)
                output(BoardContract.ID, boardState.returnNewBoardAfterMove(Pair(0,0)))
                command(publicKeys, DummyCommand())
                this.fails()
            }
        }
    }


    @Test
    fun makeMoveTransactionMustHaveOneInput() {
        ledgerServices.ledger {
            transaction {
                input(BoardContract.ID, boardState)
                output(BoardContract.ID, boardState.returnNewBoardAfterMove(Pair(0,0)))
                command(publicKeys, BoardContract.Commands.SubmitTurn())
                this.verifies()
            }
            transaction {
                input(BoardContract.ID, boardState)
                input(BoardContract.ID, DummyState())
                output(BoardContract.ID, boardState.returnNewBoardAfterMove(Pair(0,0)))
                command(publicKeys, BoardContract.Commands.SubmitTurn())
                this `fails with` "There should be one input state."
            }
        }
    }

    @Test
    fun makeMoveTransactionMustHaveOneOutput() {
        ledgerServices.ledger {
            transaction {
                input(BoardContract.ID, boardState)
                output(BoardContract.ID, boardState.returnNewBoardAfterMove(Pair(0,0)))
                command(publicKeys, BoardContract.Commands.SubmitTurn())
                this.verifies()
            }
            transaction {
                input(BoardContract.ID, boardState)
                output(BoardContract.ID, boardState.returnNewBoardAfterMove(Pair(0,0)))
                output(BoardContract.ID, DummyState())
                command(publicKeys, BoardContract.Commands.SubmitTurn())
                this `fails with` "There should be one output state."
            }
        }
    }

    @Test
    fun makeMoveTransactionMustHaveOneInputOfTypeBoardState() {
        ledgerServices.ledger {
            transaction {
                input(BoardContract.ID, boardState)
                output(BoardContract.ID, boardState.returnNewBoardAfterMove(Pair(0,0)))
                command(publicKeys, BoardContract.Commands.SubmitTurn())
                this.verifies()
            }
            transaction {
                input(BoardContract.ID, DummyState())
                output(BoardContract.ID, boardState.returnNewBoardAfterMove(Pair(0,0)))
                command(publicKeys, BoardContract.Commands.SubmitTurn())
                this `fails with` "The input state should be of type BoardState."
            }
        }
    }

    @Test
    fun makeMoveTransactionMustHaveOneOutputOfTypeBoardState() {
        ledgerServices.ledger {
            transaction {
                input(BoardContract.ID, boardState)
                output(BoardContract.ID, boardState.returnNewBoardAfterMove(Pair(0,0)))
                command(publicKeys, BoardContract.Commands.SubmitTurn())
                this.verifies()
            }
            transaction {
                input(BoardContract.ID, boardState)
                output(BoardContract.ID, DummyState())
                command(publicKeys, BoardContract.Commands.SubmitTurn())
                this `fails with` "The output state should be of type BoardState."
            }
        }
    }

    @Test
    fun cannotBeTheSamePlayersTurnInInputAndOutputState() {
        ledgerServices.ledger {
            transaction {
                input(BoardContract.ID, boardState)
                output(BoardContract.ID, boardState.returnNewBoardAfterMove(Pair(0,0)))
                command(publicKeys, BoardContract.Commands.SubmitTurn())
                this.verifies()
            }
            transaction {
                input(BoardContract.ID, boardState)
                output(BoardContract.ID, boardState)
                command(publicKeys, BoardContract.Commands.SubmitTurn())
                this `fails with` "It cannot be the same players turn both in the input board and the output board."
            }
        }
    }


    @Test
    fun mustBeValidBoardUpdate() {
        ledgerServices.ledger {
            transaction {
                input(BoardContract.ID, boardState)
                output(BoardContract.ID, boardState.returnNewBoardAfterMove(Pair(0,0)))
                command(publicKeys, BoardContract.Commands.SubmitTurn())
                this.verifies()
            }
            transaction {
                val boardState2 = boardState.returnNewBoardAfterMove(Pair(0,0))
                val boardState3 = boardState2.returnNewBoardAfterMove(Pair(0,0))
                input(BoardContract.ID, boardState2)
                output(BoardContract.ID, boardState3)
                command(publicKeys, BoardContract.Commands.SubmitTurn())
                this `fails with` "Not valid board update."
            }
        }
    }

}