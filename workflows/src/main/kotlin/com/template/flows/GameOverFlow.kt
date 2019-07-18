package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.BoardContract
import com.template.states.BoardState
import com.template.states.Status
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
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
class GameOverFlow(val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val results = serviceHub.vaultService.queryBy<BoardState>(queryCriteria)
        val states = results.states
        if (states.size != 1) {} // TODO err

        val inputBoardStateAndRef = states.single()
        val inputBoardState = inputBoardStateAndRef.state.data

        val opponentParty = (inputBoardState.participants - ourIdentity).single() as Party

        // Create Command Object
        val command = Command(BoardContract.Commands.GameOver(), inputBoardState.participants.map { it.owningKey })

        // Create an output state where the only update is the Status
        val outputBoardState = inputBoardState.copy(status = Status.GAME_OVER)

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

        return subFlow(FinalityFlow(stx, session))
    }
}

@InitiatedBy(GameOverFlow::class)
class GameOverFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat{}
        }

        val txWeJustSigned = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSigned.id))
    }
}
