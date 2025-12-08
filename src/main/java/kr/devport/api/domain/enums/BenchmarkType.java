package kr.devport.api.domain.enums;

/**
 * LLM Benchmark Types - 15 categories total
 * Source: Artificial Analysis API
 */
public enum BenchmarkType {

    // Agentic Capabilities (2)
    TERMINAL_BENCH_HARD,    // Agentic Coding & Terminal Use
    TAU_BENCH_TELECOM,      // Agentic Tool Use

    // Reasoning & Knowledge (4)
    AA_LCR,                 // Long Context Reasoning
    HUMANITYS_LAST_EXAM,    // Reasoning & Knowledge
    MMLU_PRO,               // Reasoning & Knowledge (Advanced)
    GPQA_DIAMOND,           // Scientific Reasoning

    // Coding (2)
    LIVECODE_BENCH,         // Coding (Real-world problems)
    SCICODE,                // Scientific Computing & Code

    // Specialized Skills (4)
    IFBENCH,                // Instruction Following
    MATH_500,               // Math 500
    AIME,                   // AIME (Legacy version)
    AIME_2025,              // AIME 2025 (Competition Math)

    // Composite Indices (3)
    AA_INTELLIGENCE_INDEX,  // Overall Intelligence Score
    AA_CODING_INDEX,        // Coding Index (composite)
    AA_MATH_INDEX           // Math Index (composite)
}
