package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.BoardContract
import com.template.states.BoardState
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
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
class EndGameFlow : FlowLogic<SignedTransaction>() {

    // TODO: progressTracker
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        //val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val queryCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val boardStateRefToEnd = serviceHub.vaultService.queryBy<BoardState>(queryCriteria).states.single()

        val command = Command(BoardContract.Commands.EndGame(), boardStateRefToEnd.state.data.participants.map { it.owningKey })
        val txBuilder = TransactionBuilder(notary)
                .addInputState(boardStateRefToEnd)
                .addCommand(command)
        txBuilder.verify(serviceHub)

        val otherPlayerParty = (boardStateRefToEnd.state.data.participants.map {it as Party} - ourIdentity).single()

        val ptx = serviceHub.signInitialTransaction(txBuilder)
        val targetSession = initiateFlow(otherPlayerParty)
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(targetSession)))

        return subFlow(FinalityFlow(stx, targetSession))
    }
}

@InitiatedBy(EndGameFlow::class)
class EndGameFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {}
        }
        val txWeJustSigned = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSigned.id))
    }
}
