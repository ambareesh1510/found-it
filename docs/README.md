# FindIt Project Documentation

Authoritative specifications for the FindIt (cs3803-lost-and-found) expansion. Anything
that contradicts these files — including `docs/Project/*.docx`, `README.md`, or
`frontend/README.md` — is historical and does not govern implementation.

| File | Answers |
| --- | --- |
| [architecture.md](architecture.md) | What are the components and how do they talk? |
| [data-model.md](data-model.md) | What is stored, with what fields and constraints? |
| [api.md](api.md) | What are the HTTP/WebSocket contracts? |
| [security.md](security.md) | How are auth, authorization, and private data handled? |
| [design/ui-spec.md](design/ui-spec.md) | What does the UI look like and how does it behave? |
| [design/user-flows.md](design/user-flows.md) | What are the end-to-end screen flows? |
| [development.md](development.md) | How do I run the project and verify a change? |
| [tasks/](tasks/) | Scoped implementation tasks and their approved plans. |

## The redesign in one paragraph

The original app had a loser tab and a finder tab that both opened straight onto a
browsable list of items. That let a dishonest user read a found-item listing and then
invent a matching "lost" report. The redesigned flow inverts the order for the loser:
from the home screen they pick **I Lost an Item**, declare **category**, then
**location and time**, and only then are shown found items matching that declaration.
The declared search is recorded, so a claim can always be checked against what the user
said *before* they saw any listing. The finder side is unchanged in spirit — they browse
missing-item reports and contact the owner — because a finder has nothing to gain by
lying.

## Status of the current codebase

The repository already implements auth, item CRUD, PostGIS proximity search, claims, and
chat. It does **not** yet implement the redesigned loser flow, the recorded search, the
reward field, or the location timeline. Specs here describe the target system; gaps are
called out inline as **Gap**.
