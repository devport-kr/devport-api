package kr.devport.api.domain.enums;

/** Artificial Analysis API 기준 LLM 벤치마크 타입(총 15개). */
public enum BenchmarkType {

    TERMINAL_BENCH_HARD,
    TAU_BENCH_TELECOM,

    AA_LCR,
    HUMANITYS_LAST_EXAM,
    MMLU_PRO,
    GPQA_DIAMOND,

    LIVECODE_BENCH,
    SCICODE,

    IFBENCH,
    MATH_500,
    AIME,
    AIME_2025,

    AA_INTELLIGENCE_INDEX,
    AA_CODING_INDEX,
    AA_MATH_INDEX
}
