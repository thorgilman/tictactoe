package com.template.schemas

import com.template.states.Status
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object BoardState
object BoardStateSchemaV1 : MappedSchema(schemaFamily = BoardState.javaClass, version = 1, mappedTypes = listOf(PersistentBoardState::class.java)) {
    @Entity
    @Table(name = "board_state")
    class PersistentBoardState(
            @Column(name = "playerO")
            var playerO: Party,
            @Column(name = "playerX")
            var playerX: Party,
            @Column(name = "isPlayerXTurn")
            var isPlayerXTurn: java.lang.Boolean,
            @Column(name = "board")
            var board: Array<CharArray>,
            @Column(name = "status")
            var status: Status,
            @Column(name = "linearId")
            var linearId: UUID
    ) : PersistentState() {
        //constructor(): this(Party(), Party(), false, Array(3, {charArrayOf('E', 'E', 'E')}, Status.GAME_IN_PROGRESS, UniqueIdentifier()))
    }
}
