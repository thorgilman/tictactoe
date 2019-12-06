package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.BoardContract
import com.template.states.BoardState
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/*
This flow starts a game with another node by creating an new BoardState.
The responding node cannot decline the request to start a game.
The request is only denied if the responding node is already participating in a game.
*/

@InitiatingFlow
@StartableByRPC
class StartGameFlowWithObserver(val otherPlayerParty: Party, val observerParty: Party) : FlowLogic<SignedTransaction>() {

    // TODO: progressTracker
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // If this node is already participating in an active game, decline the request to start a new one
        val criteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val results = serviceHub.vaultService.queryBy<BoardState>(criteria)
        if (results.states.isNotEmpty()) throw FlowException("A node can only play one game at a time!")

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val command = Command(BoardContract.Commands.StartGame(), listOf(ourIdentity, otherPlayerParty, observerParty).map { it.owningKey })

        val initialBoardState = BoardState(ourIdentity, otherPlayerParty, observer = observerParty)
        val stateAndContract = StateAndContract(initialBoardState, BoardContract.ID)
        val txBuilder = TransactionBuilder(notary).withItems(stateAndContract, command)
        txBuilder.verify(serviceHub)

        val ptx = serviceHub.signInitialTransaction(txBuilder)
        val targetSessions = (initialBoardState.participants - ourIdentity).map { initiateFlow(it as Party) }
        val stx = subFlow(CollectSignaturesFlow(ptx, targetSessions))
        return subFlow(FinalityFlow(stx, targetSessions))
    }
}

@InitiatedBy(StartGameFlowWithObserver::class)
class StartGameFlowWithObserverResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                // If this node is already participating in an active game, decline the request to start a new one
                val criteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                val results = serviceHub.vaultService.queryBy<BoardState>(criteria)
                if (results.states.isNotEmpty()) throw FlowException("A node can only play one game at a time!")
            }
        }
        val txWeJustSigned = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSigned.id))
    }
}
