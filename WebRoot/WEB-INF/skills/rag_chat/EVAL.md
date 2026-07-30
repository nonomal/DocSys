# rag_chat — Baseline Evaluation

## Scores

| # | Dimension | Score | Weight | Points |
|---|-----------|-------|--------|--------|
| 1 | Frontmatter quality | 8/10 | 0.8 | 6.4/8 |
| 2 | Workflow clarity | 9/10 | 1.5 | 13.5/15 |
| 3 | Boundary conditions | 7/10 | 1.0 | 7.0/10 |
| 4 | Checkpoint design | 2/10 | 0.7 | 1.4/7 |
| 5 | Instruction specificity | 8/10 | 1.5 | 12.0/15 |
| 6 | Resource integration | 3/10 | 0.5 | 1.5/5 |
| 7 | Overall architecture | 8/10 | 1.5 | 12.0/15 |
| 8 | Actual performance | 19/25 | 2.5 | 19.0/25 |
| **TOTAL** | | | | **72.8/100** |

## Key strengths
- Frontmatter is complete with a rich set of Chinese and English triggers.
- Workflow clarity is excellent: two equivalent CLI commands (`docsys rag` and `docsys ask`), clear parameter definitions, three strong examples covering both languages, and explicit output/error formats.
- Error handling table covers the four primary failure modes.
- Actual performance is the highest among the eight skills — the RAG query pass-through model is simple and reliable.
- Supports optional model parameter for flexibility.

## Key weaknesses
- **Checkpoint design is critically weak (2/10):** No confirmation, no preview, no guard against the AI producing harmful or unintended output. RAG answers can cite wrong documents or hallucinate; the skill provides no mitigation.
- **No query refinement guidance:** If no documents are found, the skill does not suggest broadening the query or trying synonyms.
- **No citation format guidance:** The skill says "with source citations" but does not specify the format (URL, document ID, paragraph number, etc.).
- **References section is a stub** with no actual content.
- **No confidence/quality signal:** No guidance on what to do when the AI returns low-confidence or contradictory answers.

## Dimension 8 dry-run notes

**Test 1 — "what does the Q4 report say about revenue?"**
Skill correctly calls `docsys rag what does the Q4 report say about revenue?` and would return an AI answer with citations. Score: 5/5 — fully correct.

**Test 2 — "这份文档的主要观点是什么？帮我总结一下"**
Skill correctly calls `docsys rag` with the Chinese query. Chinese output with citations expected. Score: 5/5 — fully correct.

**Test 3 — "compare the security architecture described in our docs with zero-trust best practices"**
This comparative query requires the AI to synthesize information from multiple documents and evaluate alignment against an external framework. The skill's pass-through model correctly forwards this to the AI, but there is no guidance on how to handle a response that lacks document citations or produces vague comparisons. Score: 9/15 — skill correctly triggers and forwards, but lacks post-processing quality checks.

## Suggested priority improvements (P0-P3)

1. **P0: Checkpoint / output guardrails** — Add a note requiring the agent to surface the answer with cited document IDs and warn if no citations are returned. Consider requiring user confirmation of the answer before acting on it.
2. **P1: Query refinement fallback** — Add guidance for the "No relevant documents found" case: suggest broadening keywords, removing filters, or falling back to `docsys search` first.
3. **P2: Citation format spec** — Specify exactly what citation format the AI should return (e.g., `[DocID] paragraph N` or URL).
4. **P3: Model selection guidance** — Add a note on when to specify a different model (e.g., use a larger model for comparative/analytical queries).
5. **P3: Resource integration** — Fill references/ with RAG system architecture docs and citation guidelines.
