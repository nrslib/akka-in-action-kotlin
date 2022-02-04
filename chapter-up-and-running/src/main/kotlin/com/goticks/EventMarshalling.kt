package com.goticks

data class EventDescription(val tickets: Int) {
    init {
        require(tickets > 0)
    }
}

data class TicketRequest(val tickets: Int) {
    init {
        require(tickets > 0)
    }
}

data class Error(val message: String)

interface EventMarshalling {

}