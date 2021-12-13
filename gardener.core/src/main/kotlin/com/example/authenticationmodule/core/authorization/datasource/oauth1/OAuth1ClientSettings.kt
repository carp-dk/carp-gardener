package com.example.authenticationmodule.core.authorization.datasource.oauth1

/**
 * Contains OAuth1 client specific information about
 * the third-party vendor.
 */
data class OAuth1ClientSettings(
  /**
   * Consumer key
   */
  val consumerKey: String,

  /**
   * Consumer secret
   */
  val consumerSecret: String,

  /**
   * Signature method
   */
  val signatureMethod: String,

  /**
   * OAuth1 version
   */
  val version: String,

  /**
   * OAuth1 request token endpoint of the third party Web API.
   */
  val requestTokenUri: String,

  /**
   * OAuth1 access token endpoint of the third party Web API.
   */
  val accessTokenUri: String,

  /**
   * OAuth1 authorization page of the vendor.
   */
  val authorizationUri: String,

  /**
   * The base URI of the vendor where the data
   * can be collected from. Not data type specific.
   */
  val dataUrl: String,

  /**
   * The application callback URI the vendor can call
   * during the authorization process.
   */
  val clientCallbackUri: String? = null
)
