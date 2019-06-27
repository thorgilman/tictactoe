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
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class StartGameFlow(val otherPlayerParty: Party) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val command = Command(BoardContract.Commands.StartGame(), listOf(ourIdentity, otherPlayerParty).map { it.owningKey })

        val initialBoardState = BoardState(ourIdentity, otherPlayerParty)
        val stateAndContract = StateAndContract(initialBoardState, BoardContract.ID)
        val txBuilder = TransactionBuilder(notary).withItems(stateAndContract, command)
        txBuilder.verify(serviceHub)

        val ptx = serviceHub.signInitialTransaction(txBuilder)
        val targetSession = initiateFlow(otherPlayerParty)
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(targetSession)))

        val tx = subFlow(FinalityFlow(stx, targetSession))

        System.out.println("You will be PlayerO")
        System.out.println("It's your turn!")
        initialBoardState.printBoard()

        return tx

    }
}

@InitiatedBy(StartGameFlow::class)
class StartGameFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                // TODO
                System.out.println("You will be PlayerX")
                (output as BoardState).printBoard()
                System.out.println("Wait for the other player...")
            }
        }
        val txWeJustSignedId = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSignedId.id))
    }
}
