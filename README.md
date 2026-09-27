# opencode-walkthrough

A walkthrough of [opencode](https://opencode.ai) — an open source AI coding agent for
the terminal, desktop, and IDE. This repo documents how opencode is configured and
used, and keeps the projects built with it as worked examples.

**Everything at the repo root is about opencode.** Each project built with it lives
in its own root-level folder and is entirely self-contained, so a new project never
touches an existing one. See [Projects](#projects).

---

## Contents

- [Install and first run](#install-and-first-run)
- [Projects](#projects)
- [`opencode.json`](#opencodejson)
- [Permissions](#permissions)
- [Skills](#skills)
- [MCP servers](#mcp-servers)
- [Agents](#agents)
- [Workflow](#workflow)
- [Reference](#reference)

---

## Install and first run

```bash
curl -fsSL https://opencode.ai/install | bash
```

Or via a package manager: `npm install -g opencode-ai`, `bun install -g opencode-ai`,
`pnpm add -g opencode-ai`, `brew install anomalyco/tap/opencode`, or
`docker run -it --rm ghcr.io/anomalyco/opencode`. Windows works best under WSL.

Then connect a model provider and initialize the project:

```
/connect          # pick a provider, paste an API key
cd /path/to/project && opencode
/init             # analyzes the project, writes AGENTS.md
```

`/init` generates an `AGENTS.md` at the project root. Commit it — it is how opencode
learns your structure and conventions across sessions.

## Projects

One folder per project, each holding its whole codebase and its own README. The root
stays opencode-only, so adding a project never means editing shared app code.

```
opencode-walkthrough/
├── .opencode/
│   └── skills/               # skills opencode loads on demand
│       ├── spring-api/SKILL.md
│       ├── angular-feature/SKILL.md
│       └── debug-backend/SKILL.md
├── opencode.json             # permissions, MCP servers, agent models
├── user-onboarding/          # project 1 — its own README
│   ├── backend/              #   Spring Boot 4.1 API
│   ├── frontend/             #   Angular 20 SPA
│   └── README.md
├── .gitignore
└── README.md                 # this file — opencode only
```

| Project         | Built with                    | Docs                        |
|-----------------|-------------------------------|-----------------------------|
| `user-onboarding` | `spring-api`, `angular-feature` | [user-onboarding/README.md](user-onboarding/README.md) |

## `opencode.json`

Project config at the repo root. This one is deliberately small:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "permission": {
    "edit": "allow",
    "bash": "ask",
    "webfetch": "allow",
    "websearch": "allow",
    "skill": "allow",
    "todowrite": "allow",
    "question": "allow"
  },
  "mcp": {
    "weather": {
      "type": "local",
      "command": ["npx", "-y", "open-meteo-mcp-server"]
    },
    "docs": {
      "type": "remote",
      "url": "https://mcp.context7.com/mcp"
    }
  },
  "agent": {
    "plan": { "model": "opencode/gpt-5.1-codex" }
  }
}
```

Config is read from the project root and merged with your global
`~/.config/opencode/opencode.json`. The `$schema` line gives you editor completion
and inline validation.

## Permissions

Every action resolves to one of three values:

| Value     | Behaviour                            |
|-----------|--------------------------------------|
| `allow`   | Runs without approval                |
| `ask`     | Prompts you — `once`, `always`, or `reject` |
| `deny`    | Blocked outright                     |

### This repo's policy

Reads, globs, greps, and subagent tasks are **not** listed, so they fall back to the
defaults, which are permissive (`allow`). The rules here are the ones that matter:

- `bash: ask` — the agent still cannot run a shell command unsupervised. This is the
  main safety rail in a repo where every tool can write files.
- `edit: allow` — file changes go through without a prompt per edit.
- `skill: allow` — skills load on their own initiative.

Two documented keys are worth knowing about and are deliberately left at their
defaults, which are `"ask"`: `external_directory` (any tool touching a path outside
the working directory) and `doom_loop` (the same call repeating three times).

> `todowrite` is not in the current documented permission list
> (`read`, `edit`, `glob`, `grep`, `bash`, `task`, `skill`, `lsp`, `question`,
> `webfetch`, `websearch`, `external_directory`, `doom_loop`), so that line is
> probably inert. It is harmless, but do not rely on it.

### Granular rules

An object applies different actions per input. Rules match by pattern, and **the last
match wins** — so put the catch-all first and specifics after:

```json
{
  "permission": {
    "bash": {
      "*": "ask",
      "git *": "allow",
      "npm run build*": "allow",
      "rm *": "deny"
    },
    "edit": {
      "*": "deny",
      "docs/*.mdx": "allow"
    }
  }
}
```

`read` denies `.env` by default, which you rarely want to override:

```json
{ "permission": { "read": { "*": "allow", "*.env": "deny", "*.env.*": "deny", "*.env.example": "allow" } } }
```

Auto-approve anything that would otherwise ask, while still honouring explicit
`deny` rules:

```bash
opencode --auto
```

## Skills

A skill is a folder of reusable instructions at `.opencode/skills/<name>/SKILL.md`.
opencode lists them to the agent by name and description, and the agent loads the
body only when a task looks relevant — so they cost almost nothing until used.

### Rules

- Only `name` and `description` are required. `license`, `compatibility`, and
  `metadata` are optional; unknown fields are ignored.
- `name` must be 1–64 chars, match `^[a-z0-9]+(-[a-z0-9]+)*$`, and equal the
  containing folder name.
- `description` must be 1–1024 chars, and should be specific enough for the agent to
  choose correctly.

Project skills live in `.opencode/skills/`; global ones in
`~/.config/opencode/skills/`. opencode also reads `.claude/skills/` and
`.agents/skills/`, walking up from the working directory to the git worktree root —
so a project folder can carry its own skills.

### The skills in this repo

| Skill            | Loaded when                       | Checklist it enforces                                          |
|------------------|-----------------------------------|----------------------------------------------------------------|
| `spring-api`     | creating a Spring Boot API        | Controller → Service → DTOs → validation → logging → error handling |
| `angular-feature`| building an Angular feature       | Component → service → API wiring → error toast → UI logic       |
| `debug-backend`  | debugging a backend issue         | Logs → failing layer → reproduce → minimal fix → test → validate |

They are intentionally short — the detail belongs in the code they produce, not the
checklist. One honest weakness: the descriptions are terse ("Use when creating Spring
Boot API"). opencode's guidance is that a sharper description routes better, so these
are worth tightening as the repo grows.

### Adding one

```bash
mkdir -p .opencode/skills/git-release
```

`.opencode/skills/git-release/SKILL.md`:

```markdown
---
name: git-release
description: Create consistent releases and changelogs
license: MIT
compatibility: opencode
metadata:
  audience: maintainers
  workflow: github
---

## What I do
- Draft release notes from merged PRs
- Propose a version bump
- Provide a copy-pasteable `gh release create` command

## When to use me
Use this when you are preparing a tagged release.
```

### Restricting skills

```json
{
  "permission": {
    "skill": { "*": "allow", "internal-*": "deny", "experimental-*": "ask" }
  }
}
```

`deny` hides the skill from the agent entirely. Per-agent overrides go under
`agent.<name>.permission.skill`, or in the agent's own frontmatter.

## MCP servers

[MCP](https://modelcontextprotocol.io) lets opencode call tools it does not ship
with. Both **local** (a command opencode runs) and **remote** (an HTTP endpoint)
servers are supported, and their tools appear alongside the built-ins.

> Each server adds to the context. A handful is fine; a dozen will bloat it and can
> exhaust the context limit — the GitHub server is the usual offender.

### The two configured here

**`docs` — Context7, remote.** Current library documentation, version-accurate:

```json
{ "mcp": { "docs": { "type": "remote", "url": "https://mcp.context7.com/mcp" } } }
```

Refer to it by name in a prompt: *"Configure a Cloudflare Worker to cache JSON for
five minutes. use docs"*. With a free account, add your key for higher rate limits:

```json
{
  "mcp": {
    "docs": {
      "type": "remote",
      "url": "https://mcp.context7.com/mcp",
      "headers": { "CONTEXT7_API_KEY": "{env:CONTEXT7_API_KEY}" }
    }
  }
}
```

**`weather` — Open-Meteo, local.** Started on demand through `npx`:

```json
{ "mcp": { "weather": { "type": "local", "command": ["npx", "-y", "open-meteo-mcp-server"] } } }
```

### Local server options

| Option        | Type    | Notes                                                    |
|---------------|---------|----------------------------------------------------------|
| `type`        | string  | Required, `"local"`                                       |
| `command`     | array   | Required, command and arguments                           |
| `cwd`         | string  | Working directory; relative paths resolve from the workspace |
| `environment` | object  | Environment variables for the process                    |
| `enabled`     | boolean | Start it up or not                                        |
| `timeout`     | number  | Tool-fetch timeout in ms, default `5000`                  |

### Remote server options

| Option     | Type             | Notes                                    |
|------------|------------------|------------------------------------------|
| `type`     | string           | Required, `"remote"`                     |
| `url`      | string           | Required                                  |
| `enabled`  | boolean          | Start it up or not                        |
| `headers`  | object           | Sent with each request                    |
| `oauth`    | object \| false  | OAuth config, or `false` to disable detection |
| `timeout`  | number           | Tool-fetch timeout in ms                  |

### Auth

OAuth is handled automatically: opencode spots the 401, registers as a client (RFC
7591) where supported, and stores the token. You can pre-register instead:

```json
{
  "mcp": {
    "docs": {
      "type": "remote",
      "url": "https://mcp.example.com/mcp",
      "oauth": {
        "clientId": "{env:MCP_CLIENT_ID}",
        "clientSecret": "{env:MCP_CLIENT_SECRET}",
        "scope": "tools:read tools:execute"
      }
    }
  }
}
```

```bash
opencode mcp list              # servers and auth status
opencode mcp auth docs         # trigger the browser flow
opencode mcp logout docs       # drop stored credentials
opencode mcp debug docs        # auth status, connectivity, OAuth discovery
```

### Turning servers off

Set `enabled: false` to keep a definition but skip it, or disable the tools
globally. Tool names are prefixed with the server name:

```json
{
  "mcp": {
    "weather": { "type": "local", "command": ["npx", "-y", "open-meteo-mcp-server"] }
  },
  "tools": { "weather_*": false }
}
```

Re-enable per agent to keep the global context small:

```json
{ "tools": { "weather_*": false }, "agent": { "build": { "tools": { "weather_*": true } } } }
```

### More examples

```json
{
  "mcp": {
    "sentry":   { "type": "remote", "url": "https://mcp.sentry.dev/mcp", "oauth": {} },
    "context7": { "type": "remote", "url": "https://mcp.context7.com/mcp" },
    "gh_grep":  { "type": "remote", "url": "https://mcp.grep.app" },
    "everything": {
      "type": "local",
      "command": ["npx", "-y", "@modelcontextprotocol/server-everything"],
      "environment": { "MY_ENV_VAR": "value" }
    }
  }
}
```

`gh_grep` searches code snippets on GitHub, which is the fastest way out of "I'm not
sure how this library does X": *"What's the right way to set a custom domain in an SST
Astro component? use the gh_grep tool"*.

## Agents

`agent` pins behaviour per mode. Here only the built-in `plan` agent is configured:

```json
{ "agent": { "plan": { "model": "opencode/gpt-5.1-codex" } } }
```

Agents also take `tools` and `permission` blocks, and a custom agent is just a
Markdown file in `~/.config/opencode/agents/`:

```markdown
---
description: Code review without edits
mode: subagent
permission:
  edit: deny
  bash: ask
  webfetch: deny
---

Only analyze code and suggest changes.
```

## Workflow

| Key / command | Effect                                                    |
|---------------|-----------------------------------------------------------|
| `Tab`         | Toggle **plan** mode (no edits, proposes how) ↔ **build** mode |
| `/undo`       | Revert the last change; run repeatedly to unwind further   |
| `/redo`       | Reapply what `/undo` removed                               |
| `/share`      | Get a link to the conversation (not shared until you do)  |
| `/connect`    | Add or change a model provider                            |
| `/init`       | Generate `AGENTS.md` for the project                       |
| `@`           | Fuzzy-search files to reference in a prompt                |

For anything beyond a one-line change, plan first, iterate on the plan, then switch
to build mode. Give it the same context you would give a junior developer — details,
examples, and the file paths that matter via `@`. Drag images into the terminal to
use them as visual references.

## Reference

- [Intro](https://opencode.ai/docs/) · [Config](https://opencode.ai/docs/config/) · [Providers](https://opencode.ai/docs/providers/)
- [Skills](https://opencode.ai/docs/skills/) · [MCP servers](https://opencode.ai/docs/mcp-servers/) · [Permissions](https://opencode.ai/docs/permissions/)
- [Agents](https://opencode.ai/docs/agents/) · [Rules (`AGENTS.md`)](https://opencode.ai/docs/rules/) · [Commands](https://opencode.ai/docs/commands/) · [Custom tools](https://opencode.ai/docs/custom-tools/)
- [CLI](https://opencode.ai/docs/cli/) · [CLI reference](https://opencode.ai/docs/cli/reference/) · [Source](https://github.com/anomalyco/opencode) · [Issues](https://github.com/anomalyco/opencode/issues)
