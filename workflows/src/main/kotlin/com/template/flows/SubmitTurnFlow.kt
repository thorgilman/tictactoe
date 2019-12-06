package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.BoardContract
import com.template.states.BoardState
import com.template.states.Status
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

/*
This flow attempts submit a turn in the game.
It must be the initiating node's turn otherwise this will result in a FlowException.
*/

@InitiatingFlow
@StartableByRPC
class SubmitTurnFlow(private val x: Int, private val y: Int) : FlowLogic<SignedTransaction>() {

    // TODO: progressTracker
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val criteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val results = serviceHub.vaultService.queryBy<BoardState>(criteria)
        val states = results.states

        val inputBoardStateAndRef = states.single()
        val inputBoardState = inputBoardStateAndRef.state.data
        //val opponentParty = (listOf(inputBoardState.playerO, inputBoardState.playerX) - ourIdentity).single()

        // Check that the correct party executed this flow
        if (inputBoardState.getCurrentPlayerParty() != ourIdentity) throw FlowException("It's not your turn!")

        val command = Command(BoardContract.Commands.SubmitTurn(), inputBoardState.participants.map { it.owningKey })
        val outputBoardState = inputBoardState.returnNewBoardAfterMove(Pair(x,y))

        val txBuilder = TransactionBuilder(notary)
                .addInputState(inputBoardStateAndRef)
                .addOutputState(outputBoardState)
                .addCommand(command)

        txBuilder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(txBuilder)
        val targetSessions = (outputBoardState.participants - ourIdentity).map { initiateFlow(it as Party) }
        val stx = subFlow(CollectSignaturesFlow(ptx, targetSessions))
        return subFlow(FinalityFlow(stx, targetSessions))
    }
}

@InitiatedBy(SubmitTurnFlow::class)
class SubmitTurnFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat{}
        }
        val txWeJustSigned = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSigned.id))
    }
}
