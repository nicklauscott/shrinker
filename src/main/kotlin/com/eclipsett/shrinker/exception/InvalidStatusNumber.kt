package com.eclipsett.shrinker.exception

class InvalidStatusNumber(override val message: String, val error: String): Exception(message)