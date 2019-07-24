package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.BoardContract
import com.template.states.BoardState
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
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
class StartGameFlow(val otherPlayerParty: Party) : FlowLogic<SignedTransaction>() {

    // TODO: progressTracker
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        // If this node is already participating in an active game, decline the request to start a new one
        val criteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val results = serviceHub.vaultService.queryBy<BoardState>(criteria)
        if (results.states.isNotEmpty()) throw FlowException("A node can only play one game at a time!")

        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val command = Command(BoardContract.Commands.StartGame(), listOf(ourIdentity, otherPlayerParty).map { it.owningKey })

        val initialBoardState = BoardState(ourIdentity, otherPlayerParty)
        val stateAndContract = StateAndContract(initialBoardState, BoardContract.ID)
        val txBuilder = TransactionBuilder(notary).withItems(stateAndContract, command)
        txBuilder.verify(serviceHub)

        val ptx = serviceHub.signInitialTransaction(txBuilder)
        val targetSession = initiateFlow(otherPlayerParty)
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(targetSession)))
        return subFlow(FinalityFlow(stx, targetSession))
    }
}

@InitiatedBy(StartGameFlow::class)
class StartGameFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

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
