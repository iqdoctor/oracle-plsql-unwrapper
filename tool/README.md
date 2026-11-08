# Privacy-first PL/SQL V2 web tool

A small Node.js standard-library HTTP service and tool-local Java bridge. The page accepts one pasted script or one `.sql`/`.plb` file; a script containing multiple wrapped objects is directed to the project CLI. The bridge calls the project's public V2 provider only. V1 is recognized by header markers and routed to Andrey Shindarev; this tool does not implement or attempt V1 recovery.

## Local build and run

Requirements: Node.js 20+ and JDK 17 (the bridge targets Java 8 bytecode). From the repository root:

```sh
JAVA_HOME=/path/to/jdk17 tool/backend/build.sh
cd tool/backend && JAVA_BIN=/path/to/jdk17/bin/java npm start
```

`npm` is used only to run scripts; there are no runtime npm dependencies. The build runs the existing Maven package without changing project files, then compiles `ToolBridge.java` into `tool/backend/build`. The service binds to `127.0.0.1:8080` by default. Set `HOST` explicitly only when you need another bind address; the default avoids exposing the development service on external interfaces. Do not expose this development server publicly.

## Configuration

| Variable | Default | Purpose |
| --- | ---: | --- |
| `HOST` | `127.0.0.1` | HTTP bind host; prefer loopback behind a local reverse proxy |
| `PORT` | `8080` | HTTP listen port |
| `MAX_REQUEST_BYTES` | `2097152` | Maximum decoded input bytes |
| `MAX_OUTPUT_BYTES` | `8388608` | Maximum bridge stdout |
| `CHILD_TIMEOUT_MS` | `15000` | Child-process wall-clock deadline |
| `MAX_CONCURRENCY` | `2` | Concurrent Java children |
| `RATE_LIMIT_PER_MINUTE` | `10` | In-memory per-client request limit |
| `JAVA_MAX_HEAP` | `128m` | Fixed-format child JVM heap cap |
| `JAVA_BIN` | `java` | Java executable; set an absolute path if Java is not discoverable as `java` from the service environment |
| `BRIDGE_CLASSPATH` | built module class directories | Bridge runtime classpath |
| `TRUSTED_PROXY_IPS` | empty | Comma-separated exact peer IPs allowed to supply `X-Forwarded-For` |

Pasted content is strict UTF-8. Uploaded bytes are decoded as strict UTF-8 when valid, otherwise ISO-8859-1. The returned source is UTF-8. Unusual encodings should be handled with the CLI.

## Privacy and safety boundaries

The backend accepts source in request memory and sends it only to the bridge child's stdin; the bridge returns a bounded structured response on stdout. No source path, temp file, database, request-body logging, analytics, cache, queue, lead form, or execution path is used. The child is launched with `shell:false`, fixed arguments, and a minimal explicit `LANG`/`LC_ALL`/`PATH` environment; it does not inherit `JAVA_TOOL_OPTIONS`, `JDK_JAVA_OPTIONS`, `_JAVA_OPTIONS`, or the parent environment generally. It uses bounded `-Xmx`, disables heap dumps on OOM, directs HotSpot fatal-error logs to `/dev/null`, and stderr is discarded. Errors are generic and do not return child stderr.

The server clears its request buffer after processing. This is not a promise of physical memory zeroization: garbage collection, runtime copies, kernel buffers, and hardware behavior are outside this application's control. The rights checkbox is the user's attestation; it does not verify ownership or provide legal advice. Do not submit code unless you have lawful rights to it; do not submit Oracle internal packages or unauthorized third-party code.

Recovered output is displayed as inert text and never executed. Comments and original formatting are usually lost. For multiple objects use the open-source CLI: <https://github.com/sftwnd/oracle-plsql-unwrapper>.

## Future reverse-proxy / global-exit planning (not a production deployment)

This repository does not configure or deploy the global-exit server. Before any deployment, perform a security review and validate settings against the selected OS and JDK:

- Put the service behind TLS termination and a reverse proxy; allow forwarding headers only from exact configured proxy peer addresses. The configured proxy MUST overwrite (not append to or pass through) the incoming `X-Forwarded-For` header with its verified client address; this service trusts the first value only for connections from `TRUSTED_PROXY_IPS`. Restrict direct access to the Node port.
- Enforce request body limits at both proxy and app (`MAX_REQUEST_BYTES`), request/response timeouts, connection/concurrency limits, and rate limiting at the edge as appropriate.
- Disable proxy request/response body logging, buffering to persistent storage, caching, analytics, and any access-log fields that capture query/body content. The app itself does not log bodies.
- `-Xmx` limits Java heap, not total JVM/process memory; set a mandatory OS/container process memory ceiling sized for heap + native/runtime overhead, and keep child concurrency low. V2 decompression can expand into heap before output limits apply.
- Disable OS core dumps (for systemd, configure `LimitCORE=0`) and verify this on the selected platform. Verify `-XX:-HeapDumpOnOutOfMemoryError` and the fatal-error `ErrorFile` sink using that exact JDK/OS; ensure diagnostic files cannot be written to a persistent working, temp, or mounted volume. Do not assume `/dev/null` exists on non-Unix systems; adapt and independently verify a non-persistent sink before deployment.
- Test OOM, crash, timeout, and abrupt disconnect behavior in an isolated environment, including that no source-bearing diagnostics are persisted. No production deployment or server changes are performed by this project.

## Tests

After building the bridge from the repository root, run `node --test tool/backend/test/*.test.js`. Existing project tests: `./mvnw test`.
