# java-expert — Evaluation Results

## Baseline Evaluation (R1)

| Dimension | Score | Notes |
|-----------|-------|-------|
| Trigger Coverage | 8/10 | 40+ triggers covering compile/runtime/logic/performance |
| Example Quality | 8/10 | 6 examples across all problem types |
| Edge Case Handling | 8/10 | File-not-found, no-permission, API-contract-violated |
| Output Format | 8/10 | Before/After snippets + verification |
| Golden Rules Clarity | 10/10 | Explicit NEVER/DO table |
| Total | **42/50** | **84.0** |

## P0 Improvements

1. **Add API contract violation detection** — skill must explicitly refuse any fix that renames/changes method signatures
2. **Add verification step** — always suggest running `mvn compile` to confirm fix works
3. **Add Before/After code snippets** — makes the fix concrete and reviewable

## Final Score

**~90.0** (after P0 fixes applied)
