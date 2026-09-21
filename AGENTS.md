# Project Instructions

## Frontend Design

- Any frontend page, component, or layout task must first read `shared_docs/19-ui-design-system.md` and inspect the existing `frontend/src/components/Beautiful*.vue` components.
- Use the local Beautiful UI adaptations as the default visual and interaction language. Prefer, in order: reuse an existing component, compose existing components, extend an existing shared component, then create a new shared `Beautiful*.vue` adaptation.
- Do not create page-local buttons, status badges, loading states, task rows, or context cards when an existing Beautiful UI adaptation already covers the need.
- Keep Vue 3, TypeScript, existing semantic tokens, API contracts, and routes. Do not add Beautiful UI's React/Tailwind runtime or copy registry TSX directly into Vue templates.
- If a new visual pattern is necessary, implement it as a reusable local adaptation backed by `frontend/src/styles/theme.css`, and document the mapping in `shared_docs/19-ui-design-system.md`.
- Frontend delivery notes must name the Beautiful UI adaptations reused or added and explain any exception to reuse.
