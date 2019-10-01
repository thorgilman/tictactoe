package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.BoardContract
import com.template.states.BoardState
import javafx.beans.property.SimpleStringProperty
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
import net.corda.core.utilities.UntrustworthyData
import net.corda.core.utilities.unwrap

/*
This flow simply pings the other nodes on the network to see who is available to start a game with.
This list of available nodes is displayed in the chooseOpponentsWindow on the frontend.
*/

@InitiatingFlow
@StartableByRPC
class AvailableNodesFlow : FlowLogic<List<String>>() {

    @Suspendable
    override fun call(): List<String> {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val nodesList = serviceHub.networkMapCache.allNodes.map { it.legalIdentities.first() } - ourIdentity - notary
        val sessions = nodesList.map { initiateFlow(it) }.toSet()

        val partyList: MutableList<String> = mutableListOf()
        sessions.forEach {
            val partyName = it.counterparty.name.toString()
            val response = it.receive<java.lang.Boolean>()
            response.unwrap { if (it.booleanValue()) partyList.add(partyName) }
        }
        return partyList.toList()
    }
}

@InitiatedBy(AvailableNodesFlow::class)
class AvailableNodesFlowResponder(val counterpartySession: FlowSession): FlowLogic<Unit>()  {
    @Suspendable
    override fun call() {
        val criteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val results = serviceHub.vaultService.queryBy<BoardState>(criteria)
        counterpartySession.send(if(results.states.isEmpty()) java.lang.Boolean.TRUE else java.lang.Boolean.FALSE)
    }
}