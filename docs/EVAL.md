# Evaluation

## Endpoints
- `POST /v1/eval/run?topK=&minScore=`
- `GET /v1/eval/runs?limit=`
- `GET /v1/eval/runs/{id}`

## Eval Case Inputs
Each `eval_case` includes:
- `question`
- `expected_answer`
- `must_answer`
- `min_score`
- `category`

## Scoring Logic
Per case:
1. Ask RAG with case-level min score
2. Pass condition:
   - expected answer containment OR token-overlap threshold
   - and if `must_answer=true`, answer must not be `I don't know.` and citations must exist

## Metrics Stored in `eval_runs`
- `score`
- `avg_faithfulness`
- `avg_relevancy`
- `avg_retrieval_ms`
- `avg_generation_ms`
- `avg_latency_ms`
- `details_json`

## Definitions
- Faithfulness: overlap between answer sentences and citation snippets
- Relevancy: normalized token overlap between question and answer

## Interpretation
- Higher faithfulness means answer is grounded in retrieved evidence
- Higher relevancy means answer is aligned to question intent
- Latency metrics help identify retrieval/generation bottlenecks