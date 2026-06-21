// Out-ports (persistence) + the cross-domain inbound port (UserDirectory) owned by the auth core.
// Adapters in :auth:repository-jpa implement the persistence ports; :auth:service implements UserDirectory.
dependencies {
    api(project(":auth:model"))
}
