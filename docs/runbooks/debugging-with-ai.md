# Debugging with AI — log sanitisation (control 4.7)

> Generated from the `log-sanitise` template (displaynote-engineering plugin) by
> `docs-update`. The "Mandatory step" and "Rules" sections are kept verbatim.

## Where this repo's logs live

| Source | Location / command | Typical sensitive content |
|---|---|---|
| Application log | No log file is written by this library. The JNI bridge (`x264-android/src/main/cpp/libx264_jni.cpp`) has a single `LOGE` macro (`__android_log_print(ANDROID_LOG_ERROR, "x264_jni", …)`), used only for three fixed messages in `JNI_OnLoad`. The Java layer (`com.github.bakaoh.x264`) does not log at all (no `android.util.Log`, Timber or `System.out`). The statically linked upstream `libx264` logs through its built-in default handler, which writes `x264 [info\|warning\|error]: …` lines to `stderr` (`stdio_compat.c` only provides the `stderr` symbol for API < 23); the JNI bridge sets neither `pf_log` nor `i_log_level`, so x264's default level (info and above, per upstream) applies. When the AAR runs inside Montage, these lines are part of the Montage process's output, not of a separate log | None from the JNI bridge (fixed strings only). x264's own lines carry encoder settings, CPU capability flags and end-of-stream frame/bitrate statistics — no user, device or network identifiers |
| Live stream | Android: `adb logcat -d -s x264_jni` for the JNI bridge. x264's `stderr` output is normally not routed to logcat for a regular app process (Android platform behaviour, not configured in this repo), so it is usually not visible there | None expected |
| Crash reports | No crash reporter (Sentry or similar) is wired into this repo. Native crashes in `libx264a.so` / `libx264.a` (e.g. a null `EncoderContext` or an undersized frame buffer) surface as `tombstone_*` files and in `adb logcat -d -b crash`; inside Montage they are reported through Montage's own crash reporting. The release `.so` is stripped (`-Wl,-s` in `x264-android/build.gradle`), so symbolised stacks need an unstripped build | Native stack frames, memory addresses, app data paths of the host app |
| CI logs | Azure Pipelines run logs (`displaynote-devops`) | internal paths, hostnames |
| Customer bundles | Zendesk ticket attachments — download them to a file and sanitise before reading; the Zendesk MCP bypasses the Claude Code guard, so never read attachments through it (see `.agents/skills/log-sanitise/SKILL.md`) | **Protected**: end-user identifiers (1.3 §4.2) |

## Mandatory step — sanitise before any AI tool sees the log

Before a log file, a log excerpt or a live log stream reaches Claude Code, Codex,
Copilot, Cursor, ChatGPT, Claude (Cowork) or any custom OpenAI-API integration,
run it through `dn_logscrub`:

```bash
# Work OUTSIDE the repository: a raw log inside it blocks the searches that would read it
mkdir -p ~/dn-tickets/22416 && cd ~/dn-tickets/22416

# Claude Code (plugin installed) — one call, all files, shared placeholders
/log-sanitise montage.log launcher.log --map ticket-22416.dnmap

# Any agent / shell — the portable copy synced into the repo
python3 <repo>/.agents/skills/log-sanitise/scripts/dn_logscrub.py montage.log launcher.log --map ticket-22416.dnmap

# Inputs in a read-only folder, or elsewhere: write the copies into one directory
python3 <repo>/.agents/skills/log-sanitise/scripts/dn_logscrub.py /var/log/omni/*.log --out-dir ~/dn-tickets/22416

# Live streams — pipe, never paste; use a command that ends (`-d`), not a live tail
adb logcat -d | python3 <repo>/.agents/skills/log-sanitise/scripts/dn_logscrub.py - > logcat.scrubbed.txt

# Customer logs — add the customer's domain(s) so their hostnames are pseudonymised too
python3 <repo>/.agents/skills/log-sanitise/scripts/dn_logscrub.py bundle/*.log --domain acme-school.org
```

A run that is cut short (Ctrl-C, a tool timeout) still writes a trailer marked
`interrupted`: the copy is clean but incomplete. The sanitiser processes a few
MB per second; run very large bundles from your own terminal.

The tool writes `<name>.scrubbed.<ext>` next to each input, prints one summary
line per file (`dn_logscrub: montage.log: 41 redactions (EMAIL=3, ID=12, …)`) and
starts every output with a `# dn_logscrub v…` marker header and ends it with a
`# dn_logscrub end | redactions=…` trailer carrying the per-category counts
(output is written as it is produced, so a live stream appears immediately
instead of waiting for the end). Only files that start with that header and
end with that trailer may be opened by, pasted into, or attached to an AI tool.
A file with the header but no trailer as its last non-empty line is raw:
something was appended after scrubbing (`cat raw >> x.scrubbed.log`,
concatenated files, a process still writing). A trailer ending in
`| interrupted` is accepted — the copy is clean, only incomplete. A
`# dn_logscrub-partial` header does not count: it means rules were switched off
for that run.

## Rules

1. Only `*.scrubbed.*` files (marker header on the first line and the
   `# dn_logscrub end` trailer as the last non-empty line) go to an AI tool.
   Raw logs never do — the Claude Code hook blocks them; for other tools the
   rule is on you.
2. Placeholders are stable within a run: `<EMAIL_1>` is the same person in every
   file of that run. Keep placeholders in PR descriptions, ticket comments and
   Slack messages; never expand them there.
3. The `--map` file (`*.dnmap`) holds the originals for your own reverse lookup.
   It stays on your machine; never attach it, commit it or paste from it.
4. Customer logs are **Protected** data by default (AI Governance Policy 1.3 §4.2).
   Sanitised customer logs may go to Green-List tools with a corporate account.
   Raw customer logs may go to an AI tool only with AI Lead + CEO approval
   (1.3 §4.1) — signal an approved exception with `DN_LOGSCRUB_ALLOW_RAW=1`.
5. Residual check: skim the scrubbed file for anything the patterns missed
   (a person's name in free text, a customer hostname without `--domain`, an
   unusual token format). Fix with `--domain`, or open an issue on
   `displaynote-engineering` with the *category and shape* of the miss — never
   with the value itself.
6. Automated flows (Sentry fixer, Release Management Agent, n8n) call the same
   library (`Scrubber().scrub_text(...)`) before every external model call. A
   flow that writes its result to a file uses `Scrubber().finalize(text, name)`,
   which adds the header and trailer the Claude Code hook and `--check` require.

## What is redacted vs. kept

Redacted irreversibly: private keys, JWTs, auth headers, URL credentials,
cloud/API keys, any `password= / token= / secret= / *_key=` value, OAuth
`code=`/`state=`/`nonce=` in URLs.
Pseudonymised consistently: e-mails, quoted/keyed names (people, devices,
computers), keyed ids (serial, deviceId, session, meetingId, roomPin, tenantId,
licence…), Android `getprop` serials, Wi-Fi SSIDs, IPv4/IPv6, MACs, UUIDs, phone
numbers, the user-home part of any path (Windows with either slash, JSON-escaped,
`file:///`, WSL), Windows `DOMAIN\user` accounts and UNC servers, `DESKTOP-…`
machine names, `.local/.lan/.internal` hosts, `--domain` domains, long hex and
high-entropy strings.
Kept: timestamps, version numbers, stack traces, package/class/method names and
JNI symbols, the rest of the path, file hashes and git commit ids, loopback
addresses, and keys that only describe a secret (`token_expires_in=3600`).
Not caught: a person's name in free text, a customer hostname without `--domain`.

Repo-specific note: no notable log-sanitisation risk in this repo's own code.
The only lines this library emits itself are the three fixed `LOGE` strings in
`JNI_OnLoad` (`x264-android/src/main/cpp/libx264_jni.cpp`, lines 178, 184 and
190), tag `x264_jni`; they contain no runtime values and survive release builds
(`LOGE` is not compiled out under `NDEBUG`, and the Java layer has no `Log`
calls for R8/ProGuard to strip — `minifyEnabled` is `false` anyway). Frame
data, SPS/PPS and encoded NAL payloads are passed back to Java and never logged.
The upstream `libx264` messages go to `stderr` at x264's default info level in
every build type (the JNI bridge does not lower it or install a callback); they
describe encoder configuration and statistics only, and their content and
routing depend on upstream x264 and Android behaviour that is not visible in
this repo (`libx264` is cloned at build time, not committed). Outside the
library: `build_x264.sh` echoes the NDK paths it uses and upstream
`./configure` / `make V=1` print full local toolchain paths (developer home
directories) to the terminal, and the docs-sync GitHub Actions workflows
(`.github/workflows/docs-sync.yml`, `.github/workflows/docs-catchup.yml`) run
`tools/docs-sync/run_docs_agent.sh`, which writes the full AI-agent transcript
to `docs-agent.log` in the job workspace (or the local checkout when run by
hand; gitignored) and prints its last 50 lines to the job log on failure —
those runs are GitHub Actions jobs, not Azure Pipelines, and the transcript is
not uploaded as an artifact (only `docs-freshness.json` is). Sanitise any of
these before handing them to an AI tool. No repo-specific sanitiser patterns have been shipped yet; raise any needed as an issue on `displaynote-engineering` and list them here once shipped.
