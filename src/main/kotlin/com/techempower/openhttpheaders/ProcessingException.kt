package com.techempower.openhttpheaders

// TODO: Add a subclass of this that allows for the specification of a string
//  and an index, such that the error message indicates there was a failure to
//  parse at that location.
class ProcessingException : RuntimeException {
  constructor(message: String?) : super(message)
  constructor(message: String?, cause: Throwable?) : super(message, cause)

  companion object {
    private const val serialVersionUID = 8411477705739130341L
  }
}
