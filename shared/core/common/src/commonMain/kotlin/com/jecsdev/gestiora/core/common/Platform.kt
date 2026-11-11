package com.jecsdev.gestiora.core.common

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
