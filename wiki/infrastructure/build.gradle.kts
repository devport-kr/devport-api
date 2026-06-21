// Out-ports owned by the wiki core: persistence (chat/sessions/chunks incl. pgvector), the Redis
// session store + rate-limit counter, and the LLM chat/embedding ports. Adapters implement them.
dependencies {
    api(project(":wiki:model"))
}
