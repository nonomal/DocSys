# create_repos — Baseline Evaluation

## Scores (dimension: raw × weight → points / max)

| # | Dimension | Score | Weight | Points |
|---|-----------|-------|--------|--------|
| 1 | Frontmatter quality | 8/10 | 0.8 | 6.4/8 |
| 2 | Workflow clarity | 7/10 | 1.5 | 10.5/15 |
| 3 | Boundary conditions | 7/10 | 1.0 | 7.0/10 |
| 4 | Checkpoint design | 4/10 | 0.7 | 2.8/7 |
| 5 | Instruction specificity | 7/10 | 1.5 | 10.5/15 |
| 6 | Resource integration | 6/10 | 0.5 | 3.0/5 |
| 7 | Overall architecture | 8/10 | 1.5 | 12.0/15 |
| 8 | Actual performance | 8/10 | 2.5 | 20.0/25 |
| **TOTAL** | | | | **72.2/100** |

## Key strengths
- Frontmatter complete: name规范, description含"Create"触发词, all required fields
- Good parameter specificity: name with forbidden char list (no `/ \ : * ? " < > |`), path as required string
- Error handling is thorough: covers empty name, empty path, invalid chars, duplicate name, access denied
- Bilingual triggers and examples
- Clean and consistent structure across all sections

## Key weaknesses
- No confirmation checkpoint before creating a repository — this could overwrite or conflict with existing data
- No validation of the storage path (is it writable? does it exist? will it be created automatically?)
- No numbered workflow steps with input/output per step
- No guidance on what happens if the path already exists (is it an error or OK if valid?)
- references/ section not verified

## Dimension 8 dry-run notes

**Prompt 1:** "create a new repo called MyProject at F:/data/myrepo"
Skill correctly extracts name='MyProject', path='F:/data/myrepo'. Calls `docsys repos add MyProject F:/data/myrepo`. Expected: new VID returned. Skill handles this cleanly. Score: 8/10.

**Prompt 2:** "新建仓库 项目A 路径 E:/projects/A"
Skill correctly parses Chinese format: name='项目A', path='E:/projects/A'. Calls `docsys repos add 项目A E:/projects/A`. Chinese confirmation expected. Score: 8/10.

**Prompt 3:** "add a repository named Reports at /mnt/storage/reports"
Skill correctly extracts name='Reports', path='/mnt/storage/reports'. Cross-platform Unix-style path handled. Score: 8/10.

## Suggested priority improvements (P0-P3)
1. P0: Checkpoint design — add confirmation step before repository creation (show name + path, warn about data implications, confirm intent)
2. P1: Boundary conditions — add storage path validation guidance (does path need to pre-exist? will it be created? what are write permissions?)
3. P2: Workflow clarity — add numbered steps with input/output per step
4. P3: Resource integration — verify references/ directory content
