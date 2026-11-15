package io.github.vad4nus.sample

var logCount: Int = 0

fun Address.isNear(other: Address): Boolean = zip == other.zip

fun Address.log() {
    logCount++
}

fun Address.zipOrNull(): Int? = zip.takeIf { it != 0 }

fun Address.next(): Address? = if (zip < 1000) Address(city, zip + 1) else null

fun Address.formatter(): () -> String = { "$city/$zip" }

fun Address.tag(kotlin: String): String = "$kotlin:$city"

fun Address.kotlin(): String = city

suspend fun Address.refresh(): String = city

fun <T> Address.echo(value: T): T = value

@Throws(IllegalStateException::class)
fun Address.validate() {
    check(zip >= 0)
}

fun Address.near(other: Address?): Boolean = other?.zip == zip

fun Address.inScreen(screen: ProfileScreenState): Boolean = screen is ProfileScreenState.Idle
