package com.template.schemas

import com.template.states.Status
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import org.hibernate.annotations.Type
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object BoardStateSchema
object BoardStateSchemaV1 : MappedSchema(schemaFamily = BoardStateSchema.javaClass, version = 1, mappedTypes = listOf(PersistentBoardState::class.java)) {
    @Entity
    @Table(name = "board_states")
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
            @Type(type = "uuid-char")
            var linearId: UUID
    ) : PersistentState()

    override val migrationResource = "board_schema.changelog.init"
}
