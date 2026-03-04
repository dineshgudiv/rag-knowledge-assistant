# Proof E2E

## java -version
```
openjdk version "17.0.18" 2026-01-20
OpenJDK Runtime Environment Temurin-17.0.18+8 (build 17.0.18+8)
OpenJDK 64-Bit Server VM Temurin-17.0.18+8 (build 17.0.18+8, mixed mode, sharing)
```

## mvnw -v
```
Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)
Maven home: C:\Users\91889\.m2\wrapper\dists\apache-maven-3.9.9-bin\4nf9hui3q3djbarqar9g711ggc\apache-maven-3.9.9
Java version: 17.0.18, vendor: Eclipse Adoptium, runtime: C:\Users\91889\Music\projects\rag-knowledge-assistant\.tools\jdk17
Default locale: en_IN, platform encoding: Cp1252
OS name: "windows 11", version: "10.0", arch: "amd64", family: "windows"
`",
        ",
        
```json
{
    "documentId":  1,
    "chunkCount":  1,
    "name":  "sample.txt"
}
```

## ask in-scope
```json
{
    "answer":  "ï»¿RAG [mean]s retrieval augmented generation.",
    "citations":  [
                      {
                          "chunkId":  2,
                          "documentId":  1,
                          "parentId":  1,
                          "chunkIndex":  0,
                          "score":  0.30615377086393963,
                          "scoreVec":  0.6386750463643103,
                          "scoreKw":  0.125,
                          "scoreRerank":  0.0,
                          "snippet":  "ï»¿RAG [mean]s retrieval augmented generation. This system returns citations in answers.",
                          "startOffset":  0,
                          "endOffset":  84
                      }
                  ],
    "bestScore":  0.30615377086393963,
    "queryLogId":  1,
    "latencyMs":  132,
    "mode":  "extractive",
    "requestId":  "99879097-7ce4-482d-b97e-eade8dfc9a17"
}
```

## ask out-of-scope
```json
{
    "answer":  "I don\u0027t know.",
    "citations":  [

                  ],
    "bestScore":  0.0,
    "queryLogId":  2,
    "latencyMs":  39,
    "mode":  "idk",
    "requestId":  "c26b6210-1915-441f-95d1-ee0870b00d25"
}
```

## eval
```json
{
    "runId":  1,
    "total":  5,
    "passed":  1,
    "score":  0.2,
    "avgFaithfulness":  1.0,
    "avgRelevancy":  0.169,
    "avgRetrievalMs":  28,
    "avgGenerationMs":  1,
    "avgLatencyMs":  29
}
```
