package com.example.authenticationmodule.core.authorization.datasource

import com.example.authenticationmodule.core.authorization.authorizationstate.AuthorizationState

/**
 * Contains the fully assembled [authorizationUrl] the user should be redirected to
 * during the authorization process and an [authorizationState] object that specifically
 * created for the user and the data source to save authorization state in-between calls.
 */
data class AuthorizationRequest(val authorizationUrl: String, val authorizationState: AuthorizationState)