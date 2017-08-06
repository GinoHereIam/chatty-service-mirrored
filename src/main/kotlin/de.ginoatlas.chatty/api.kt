package de.ginoatlas.chatty

// For JSON
import com.beust.klaxon.JsonArray
import com.beust.klaxon.json
import org.eclipse.jetty.websocket.api.Session
import org.joda.time.DateTime
import java.util.*

/**
 * Created by gino on 30.06.17.
 *
 */

//
// PROTOCOL DEFINITION
//

/*
    TODO Create own library which the client/service can use the protocol
*/

data class User(
        // Add more user properties here
        val conn: Session,
        val sessionID: UUID
) {
    // Unique user name | it's for login
    var username: String = ""
        get
        set

    // Display name
    var name: String = ""
        get
        set

    // Credential token
    var token: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
        get
        set

    // Probably wrong type?
    var photo: Byte? = null
        get
        set
}

enum class ResponseType {
    SUCCESS,
    FAILED,
    NONE
}

enum class ActionType {
    // Actions to friends
    USER_ADD_FRIEND,
    USER_DELETE_FRIEND,

    // Actions to chat
    USER_CREATE_CHAT,
    USER_SEND_MESSAGE,
    USER_DELETE_MESSAGE,

    // Actions to account
    USER_REGISTER_ACCOUNT,
    USER_LOGIN_ACCOUNT,
    USER_DELETE_ACCOUNT,

    // Actions to connection
    USER_CONNECT,
    USER_DISCONNECT,

    // Default
    NONE
}

data class Message(
        val timestamp: DateTime,
        val content: String
)

data class Header(
        var additionalText: String = "") {
    var setAdditionalText: String = ""
        set(value) {
            additionalText = value
        }
}

data class CPoW(
        val action: ActionType,
        val response: ResponseType,
        val participant: User,
        val header: Header,
        // For verification compatibility?
        val version: String = "v1") {

    // Getter / Setter
    var actionType: ActionType = action
        get
        set

    var responseType: ResponseType = response
        get
        set

    var user: User = participant
        get
        set

    var messageHeader: Header = header
        get
        set

    lateinit var message: Message
        get
        set

    lateinit var contacts: MutableList<User>
        get
        set

    lateinit var chats: MutableList<Chat>
        get
        set

}

suspend fun parseCPOW(protocol: CPoW): JsonArray<Any?> {

    // FIXME this is not generic enough for me
    val cpow = json {
        array(
                obj(
                        "version" to protocol.version
                ),
                obj(
                        "actionType" to protocol.actionType.name
                ),
                array(
                        obj(
                                "content" to protocol.message.content
                        ),
                        obj(
                                "timestamp" to protocol.message.timestamp.toLocalDateTime().toString()
                        )
                ),
                array(
                        obj(
                                "additionalText" to protocol.header.additionalText
                        )
                ),
                obj(
                        "responseType" to protocol.responseType.name
                ),
                array(
                        obj(
                                "sessionID" to protocol.user.sessionID.toString()
                        ),
                        obj(
                                "username" to protocol.user.username
                        ),
                        obj(
                                "name" to protocol.user.name
                        ),
                        obj(
                                "token" to protocol.user.token.toString()
                        )
                ),
                array(
                        // Contacts related information
                        if (protocol.contacts.size > 0) {
                            protocol.contacts.forEach {
                                obj("contacts" to arrayListOf(it.name))
                                obj("sessions" to arrayListOf(it.sessionID.toString()))
                            }
                        } else {
                            obj("contacts" to array())
                            obj("sessions" to array())
                        }
                ),
                array(
                        // Get each unique chat id
                        if (protocol.chats.size > 0) {
                            protocol.chats.forEach {
                                // Chat related information
                                obj("chats" to protocol.chats.size)
                                obj("chatIDs" to arrayListOf(it.chatID.toString()))
                            }
                        } else {
                            obj("chats" to 0)
                            obj("chatIDs" to array())
                        }
                )
        )
    }

    // TODO just for debugging
    // println(cpow.toJsonString(true))

    return cpow
}