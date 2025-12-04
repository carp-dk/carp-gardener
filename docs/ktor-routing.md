# Ktor Routing (gardener-caws-implementation)

File: `gardener-caws-implementation/src/main/kotlin/dk/carp/gardener/authentication/ktor/Routing.kt`

This Ktor module exposes the public HTTP surface for authorization callbacks and data-collection webhooks. It wires Ktor `Application` routing to the Gardener `IDataSourceRegistry` and `IEventBus` so the OAuth and collection flows can hand off into the core services.

## Dependencies
- `IDataSourceRegistry` supplies data-source implementations (OAuth1/2 capabilities and payload parsing).
- `IEventBus` receives authorization and collection events emitted from the routes.
- `PropertiesConfig` provides Fitbit subscriber verification code (`fitbit.client.subscription.code`).
- `Logger` is used for tracing requests and decisions.

## Endpoint Reference

- `GET /wearables/api/authorize/{dataSourceId}/{userId}`  
  Starts OAuth authorization and redirects to the vendor.
  - Path: `dataSourceId` (fitbit|garmin|withings|dexcom…), `userId` (internal user id). Required.
  - Query:  
    - `deploymentId` (optional): stored in application data; also placed under `deploymentId` in the JSON passed to transformers.  
    - `deviceRoleName` (optional): stored in application data; forwarded to `CarpDataStreamBuilder` as `deviceRoleName`.  
    - `scopes` (optional, OAuth2 only): comma-separated scopes to request. If omitted, defaults per data source.
  - Response: `302` redirect to provider authorization URL. `400` when path params missing.

- `GET /wearables/api/oauth/{dataSourceId}/callback`  
  Handles OAuth callbacks from vendors.
  - OAuth2 expects query `code`, `state`; publishes `OAuth2Event.AuthorizationCodeAcquired`.
  - OAuth1 expects query `state`, `oauth_token`, `oauth_verifier`; publishes `OAuth1Event.AuthorizedTokenAcquired`.
  - Response: `200 OK` always.

- `POST /wearables/api/collection/{dataSourceId}`  
  Vendor webhook receiver for collection pings.
  - Path: `dataSourceId` required.
  - Body: raw payload (varies by vendor). Blank bodies are logged and ignored.
  - Response: `204 No Content` for Fitbit, `200 OK` for others (before processing).
  - Sample payloads:
    - Fitbit: JSON array of notifications, e.g.  
      ```json
      [
        { "collectionType": "activities", "date": "2024-05-20", "ownerId": "fitbit-user-123" }
      ]
      ```
    - Garmin: JSON array (outer) of arrays (inner) with `callbackURL` and `userAccessToken`, e.g.  
      ```json
      [
        [
          {
            "callbackURL": "https://apis.garmin.com/wellness-api/rest/dailies?userAccessToken=user-123&uploadStartTimeInSeconds=1716182400&uploadEndTimeInSeconds=1716268799",
            "userAccessToken": "user-123"
          }
        ]
      ]
      ```
    - Withings: URL-encoded key/value string (not JSON), e.g.  
      ```
      appli=16&userid=12345&date=1719248100
      ```
      where `appli` is 16 (daily activity), 54 (heart list), or 44 (sleep), plus `date` or `startdate`/`enddate` as required.
  - Processing: payload is passed to `dataSource.getDataCollectionPreparationEventFromPing` to emit `DataCollectionPreparationEvent`s onto the event bus.

- `GET /wearables/api/collection/fitbit`  
  Fitbit subscriber verification endpoint.
  - Query: `verify` required; must match `fitbit.client.subscription.code` in properties.
  - Response: `204 No Content` when correct, `400` when missing, `404` otherwise.

## Behavior Notes
- `dataSourceId` and `userId` path parameters are mandatory on the authorization route; missing values short-circuit with `400 Bad Request`.
- When `scopes` is absent or blank, the authorization request uses the defaults already present on the data source's established parameters.
- The routing function is marked `@Suppress("LongMethod", "CyclomaticComplexMethod")` because it intentionally centralizes the HTTP entrypoints in one place.
