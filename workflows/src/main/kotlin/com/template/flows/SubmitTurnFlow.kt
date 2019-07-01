package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.BoardContract
import com.template.states.BoardState
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********

@InitiatingFlow
@StartableByRPC
class SubmitTurnFlow(val x: Int, val y: Int) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val criteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val results = serviceHub.vaultService.queryBy<BoardState>(criteria)
        val states = results.states
        if (states.size != 1) {} // TODO err

        val inputBoardStateAndRef = states.single()
        val inputBoardState = inputBoardStateAndRef.state.data

        if (inputBoardState.getCurrentPlayerParty() != ourIdentity) throw FlowException("It's not your turn!")

        val opponentParty = (inputBoardState.participants - ourIdentity).single() as Party


        // Create Command Object
        val command = Command(BoardContract.Commands.SubmitTurn(), inputBoardState.participants.map { it.owningKey })

        //inputBoardState.printBoard()
        val outputBoardState = inputBoardState.returnNewBoardAfterMove(Pair(x,y))
        //inputBoardState.printBoard()

        //outputBoardState.printBoard() // TODO
        System.out.println("Wait for the other player...")

        // Create TransactionBuilder Object
        val txBuilder = TransactionBuilder(notary)
                .addInputState(inputBoardStateAndRef)
                .addOutputState(outputBoardState)
                .addCommand(command)

        // Verify and sign TransactionBuilder
        txBuilder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(txBuilder)

        // Get session to other party
        val session = initiateFlow(opponentParty)
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(session)))
        val tx = subFlow(FinalityFlow(stx, session))

        // TODO TODO TODO
        // Return this tx if isGameOver?
        if (BoardContract.BoardUtils.isGameOver(outputBoardState)) subFlow(EndGameFlow(outputBoardState.linearId))

        return tx
    }
}

@InitiatedBy(SubmitTurnFlow::class)
class SubmitMoveFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat{}
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)

        println("It's your turn!")
        (txWeJustSignedId.tx.outputs.single().data as BoardState).printBoard()

        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSignedId.id))
    }
}
