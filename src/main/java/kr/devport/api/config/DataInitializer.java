package kr.devport.api.config;

import kr.devport.api.domain.entity.Article;
import kr.devport.api.domain.entity.ArticleMetadata;
import kr.devport.api.domain.entity.Benchmark;
import kr.devport.api.domain.entity.LLMBenchmarkScore;
import kr.devport.api.domain.entity.LLMModel;
import kr.devport.api.domain.enums.BenchmarkType;
import kr.devport.api.domain.enums.Category;
import kr.devport.api.domain.enums.ItemType;
import kr.devport.api.domain.enums.Source;
import kr.devport.api.repository.ArticleRepository;
import kr.devport.api.repository.BenchmarkRepository;
import kr.devport.api.repository.LLMBenchmarkScoreRepository;
import kr.devport.api.repository.LLMModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

//@Component //개발시
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final BenchmarkRepository benchmarkRepository;
    private final LLMModelRepository llmModelRepository;
    private final LLMBenchmarkScoreRepository llmBenchmarkScoreRepository;
    private final ArticleRepository articleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting data initialization...");

        initializeBenchmarks();
        initializeLLMModels();
        initializeLLMBenchmarkScores();
        initializeArticles();

        log.info("Data initialization completed!");
    }

    private void initializeBenchmarks() {
        log.info("Initializing benchmarks...");

        List<Benchmark> benchmarks = Arrays.asList(
            Benchmark.builder()
                .type(BenchmarkType.AGENTIC_CODING)
                .labelEn("Agentic Coding")
                .labelKo("에이전틱 코딩")
                .descriptionEn("Data from the SWE Benchmark that evaluates if LLMs can resolve GitHub Issues. It measures agentic reasoning.")
                .descriptionKo("LLM이 GitHub 이슈를 해결할 수 있는지 평가하는 SWE 벤치마크 데이터입니다. 에이전틱 추론 능력을 측정합니다.")
                .icon("💻")
                .build(),

            Benchmark.builder()
                .type(BenchmarkType.REASONING)
                .labelEn("Reasoning")
                .labelKo("추론 능력")
                .descriptionEn("Data from the GPQA Diamond benchmark that evaluates expert-level reasoning in biology, physics, and chemistry.")
                .descriptionKo("생물학, 물리학, 화학 분야의 전문가 수준 추론 능력을 평가하는 GPQA Diamond 벤치마크입니다.")
                .icon("🧠")
                .build(),

            Benchmark.builder()
                .type(BenchmarkType.MATH)
                .labelEn("High School Math")
                .labelKo("고등 수학")
                .descriptionEn("Data from AIME 2024 that evaluates advanced high school mathematics problem-solving.")
                .descriptionKo("고급 고등학교 수학 문제 해결 능력을 평가하는 AIME 2024 벤치마크입니다.")
                .icon("🔢")
                .build(),

            Benchmark.builder()
                .type(BenchmarkType.VISUAL)
                .labelEn("Visual Reasoning")
                .labelKo("시각적 추론")
                .descriptionEn("Data from ARC-AGI 2 that evaluates visual pattern recognition and abstract reasoning.")
                .descriptionKo("시각적 패턴 인식과 추상적 추론 능력을 평가하는 ARC-AGI 2 벤치마크입니다.")
                .icon("👁️")
                .build(),

            Benchmark.builder()
                .type(BenchmarkType.MULTILINGUAL)
                .labelEn("Multilingual")
                .labelKo("다국어")
                .descriptionEn("Data from MMMLU that evaluates multilingual knowledge across various domains and languages.")
                .descriptionKo("다양한 분야와 언어에 걸친 다국어 지식을 평가하는 MMMLU 벤치마크입니다.")
                .icon("🌍")
                .build()
        );

        benchmarkRepository.saveAll(benchmarks);
        log.info("Initialized {} benchmarks", benchmarks.size());
    }

    private void initializeLLMModels() {
        log.info("Initializing LLM models...");

        LocalDateTime now = LocalDateTime.now();

        List<LLMModel> models = Arrays.asList(
            LLMModel.builder()
                .name("Claude Sonnet 4.5")
                .provider("Anthropic")
                .contextWindow("200K")
                .pricing("$3 / $15")
                .createdAt(now)
                .updatedAt(now)
                .build(),

            LLMModel.builder()
                .name("GPT-5")
                .provider("OpenAI")
                .contextWindow("200K")
                .pricing("$2.50 / $10")
                .createdAt(now)
                .updatedAt(now)
                .build(),

            LLMModel.builder()
                .name("Gemini 3 Pro")
                .provider("Google")
                .contextWindow("2M")
                .pricing("$1.25 / $5")
                .createdAt(now)
                .updatedAt(now)
                .build(),

            LLMModel.builder()
                .name("Grok 4")
                .provider("xAI")
                .contextWindow("128K")
                .pricing("$2 / $8")
                .createdAt(now)
                .updatedAt(now)
                .build(),

            LLMModel.builder()
                .name("Claude Opus 4")
                .provider("Anthropic")
                .contextWindow("200K")
                .pricing("$15 / $75")
                .createdAt(now)
                .updatedAt(now)
                .build(),

            LLMModel.builder()
                .name("GPT-4.5 Turbo")
                .provider("OpenAI")
                .contextWindow("128K")
                .pricing("$1 / $3")
                .createdAt(now)
                .updatedAt(now)
                .build(),

            LLMModel.builder()
                .name("Gemini 2.5 Flash")
                .provider("Google")
                .contextWindow("1M")
                .pricing("$0.50 / $1.50")
                .createdAt(now)
                .updatedAt(now)
                .build(),

            LLMModel.builder()
                .name("DeepSeek V3")
                .provider("DeepSeek")
                .contextWindow("64K")
                .pricing("$0.30 / $0.60")
                .createdAt(now)
                .updatedAt(now)
                .build()
        );

        llmModelRepository.saveAll(models);
        log.info("Initialized {} LLM models", models.size());
    }

    private void initializeLLMBenchmarkScores() {
        log.info("Initializing LLM benchmark scores...");

        List<LLMModel> models = llmModelRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        // Agentic Coding scores
        llmBenchmarkScoreRepository.saveAll(Arrays.asList(
            createScore(models.get(0), BenchmarkType.AGENTIC_CODING, 82.0, 1, now),
            createScore(models.get(1), BenchmarkType.AGENTIC_CODING, 76.3, 2, now),
            createScore(models.get(2), BenchmarkType.AGENTIC_CODING, 73.5, 3, now),
            createScore(models.get(3), BenchmarkType.AGENTIC_CODING, 68.2, 4, now),
            createScore(models.get(4), BenchmarkType.AGENTIC_CODING, 79.8, 5, now),
            createScore(models.get(5), BenchmarkType.AGENTIC_CODING, 65.4, 6, now),
            createScore(models.get(6), BenchmarkType.AGENTIC_CODING, 58.9, 7, now),
            createScore(models.get(7), BenchmarkType.AGENTIC_CODING, 54.1, 8, now)
        ));

        // Reasoning scores
        llmBenchmarkScoreRepository.saveAll(Arrays.asList(
            createScore(models.get(4), BenchmarkType.REASONING, 88.5, 1, now),
            createScore(models.get(0), BenchmarkType.REASONING, 85.2, 2, now),
            createScore(models.get(1), BenchmarkType.REASONING, 82.7, 3, now),
            createScore(models.get(2), BenchmarkType.REASONING, 79.3, 4, now),
            createScore(models.get(3), BenchmarkType.REASONING, 74.6, 5, now),
            createScore(models.get(5), BenchmarkType.REASONING, 71.8, 6, now),
            createScore(models.get(7), BenchmarkType.REASONING, 68.4, 7, now),
            createScore(models.get(6), BenchmarkType.REASONING, 65.1, 8, now)
        ));

        // Math scores
        llmBenchmarkScoreRepository.saveAll(Arrays.asList(
            createScore(models.get(1), BenchmarkType.MATH, 91.2, 1, now),
            createScore(models.get(4), BenchmarkType.MATH, 87.9, 2, now),
            createScore(models.get(0), BenchmarkType.MATH, 84.6, 3, now),
            createScore(models.get(2), BenchmarkType.MATH, 78.3, 4, now),
            createScore(models.get(3), BenchmarkType.MATH, 73.5, 5, now),
            createScore(models.get(7), BenchmarkType.MATH, 69.7, 6, now),
            createScore(models.get(5), BenchmarkType.MATH, 66.2, 7, now),
            createScore(models.get(6), BenchmarkType.MATH, 61.8, 8, now)
        ));

        // Visual scores
        llmBenchmarkScoreRepository.saveAll(Arrays.asList(
            createScore(models.get(2), BenchmarkType.VISUAL, 86.4, 1, now),
            createScore(models.get(1), BenchmarkType.VISUAL, 82.1, 2, now),
            createScore(models.get(0), BenchmarkType.VISUAL, 78.7, 3, now),
            createScore(models.get(4), BenchmarkType.VISUAL, 75.3, 4, now),
            createScore(models.get(6), BenchmarkType.VISUAL, 71.9, 5, now),
            createScore(models.get(3), BenchmarkType.VISUAL, 68.5, 6, now),
            createScore(models.get(5), BenchmarkType.VISUAL, 64.2, 7, now),
            createScore(models.get(7), BenchmarkType.VISUAL, 59.8, 8, now)
        ));

        // Multilingual scores
        llmBenchmarkScoreRepository.saveAll(Arrays.asList(
            createScore(models.get(2), BenchmarkType.MULTILINGUAL, 89.3, 1, now),
            createScore(models.get(0), BenchmarkType.MULTILINGUAL, 85.7, 2, now),
            createScore(models.get(1), BenchmarkType.MULTILINGUAL, 82.4, 3, now),
            createScore(models.get(4), BenchmarkType.MULTILINGUAL, 78.9, 4, now),
            createScore(models.get(7), BenchmarkType.MULTILINGUAL, 75.2, 5, now),
            createScore(models.get(3), BenchmarkType.MULTILINGUAL, 71.6, 6, now),
            createScore(models.get(5), BenchmarkType.MULTILINGUAL, 67.8, 7, now),
            createScore(models.get(6), BenchmarkType.MULTILINGUAL, 63.4, 8, now)
        ));

        log.info("Initialized LLM benchmark scores");
    }

    private LLMBenchmarkScore createScore(LLMModel model, BenchmarkType type, Double score, Integer rank, LocalDateTime now) {
        return LLMBenchmarkScore.builder()
            .model(model)
            .benchmarkType(type)
            .score(score)
            .rank(rank)
            .updatedAt(now)
            .build();
    }

    private void initializeArticles() {
        log.info("Initializing articles...");

        LocalDateTime now = LocalDateTime.now();

        List<Article> articles = Arrays.asList(
            // GitHub Repos - AI/LLM
            createArticle(
                ItemType.REPO, Source.github, Category.AI_LLM,
                "로컬에서 실행 가능한 초경량 LLM 프레임워크, 90% 메모리 절감 달성",
                "엣지 디바이스에서도 대규모 언어 모델을 구동할 수 있는 혁신적인 최적화 기술을 적용했습니다.",
                "Lightweight LLM Framework for Edge Devices - Achieves 90% Memory Reduction",
                "https://github.com/example/llm-edge",
                1250,
                List.of("llm", "edge-computing", "optimization"),
                now.minusHours(2),
                now.minusHours(2),
                ArticleMetadata.builder().stars(8900).language("English").build()
            ),

            createArticle(
                ItemType.REPO, Source.github, Category.AI_LLM,
                "중국발 오픈소스 멀티모달 LLM, GPT-4V 수준 달성",
                "시각, 텍스트, 오디오를 통합 처리하는 멀티모달 AI 모델로 오픈소스 커뮤니티에 공개되었습니다.",
                "Open Source Multimodal LLM Achieving GPT-4V Level Performance",
                "https://github.com/example/multimodal-llm",
                1350,
                List.of("multimodal", "llm", "vision"),
                now.minusHours(5),
                now.minusHours(5),
                ArticleMetadata.builder().stars(12400).language("Chinese").build()
            ),

            createArticle(
                ItemType.REPO, Source.github, Category.AI_LLM,
                "로컬 LLM을 위한 초고속 추론 엔진, CUDA 최적화로 10배 성능 향상",
                null,
                "Ultra-Fast Inference Engine for Local LLMs with 10x Performance Boost",
                "https://github.com/example/fast-inference",
                980,
                List.of("inference", "cuda", "performance"),
                now.minusHours(12),
                now.minusHours(12),
                ArticleMetadata.builder().stars(6700).language("English").build()
            ),

            // GitHub Repos - DevOps/SRE
            createArticle(
                ItemType.REPO, Source.github, Category.DEVOPS_SRE,
                "쿠버네티스 클러스터 자동 스케일링 도구, AWS 비용 50% 절감 사례",
                "실시간 트래픽 패턴을 분석하여 최적의 노드 수를 자동으로 조정합니다.",
                "Kubernetes Auto-Scaling Tool - 50% AWS Cost Reduction Case Study",
                "https://github.com/example/k8s-autoscaler",
                1120,
                List.of("kubernetes", "aws", "cost-optimization"),
                now.minusHours(8),
                now.minusHours(8),
                ArticleMetadata.builder().stars(5600).language("English").build()
            ),

            createArticle(
                ItemType.REPO, Source.github, Category.DEVOPS_SRE,
                "오픈소스 옵저버빌리티 플랫폼, Datadog 대체 가능",
                null,
                "Open Source Observability Platform - A Datadog Alternative",
                "https://github.com/example/observability",
                890,
                List.of("monitoring", "observability", "open-source"),
                now.minusHours(18),
                now.minusHours(18),
                ArticleMetadata.builder().stars(4200).language("English").build()
            ),

            // GitHub Repos - Backend
            createArticle(
                ItemType.REPO, Source.github, Category.BACKEND,
                "Rust로 작성된 초고속 웹 프레임워크, Node.js 대비 100배 빠른 처리 속도",
                "메모리 안전성과 동시성을 모두 보장하면서도 C++ 수준의 성능을 제공합니다.",
                "Ultra-Fast Web Framework in Rust - 100x Faster than Node.js",
                "https://github.com/example/rust-web",
                1450,
                List.of("rust", "web-framework", "performance"),
                now.minusHours(3),
                now.minusHours(3),
                ArticleMetadata.builder().stars(15300).language("English").build()
            ),

            createArticle(
                ItemType.REPO, Source.github, Category.BACKEND,
                "GraphQL 자동 코드 생성 도구, TypeScript 완벽 지원",
                null,
                "GraphQL Auto Code Generator with Full TypeScript Support",
                "https://github.com/example/graphql-gen",
                760,
                List.of("graphql", "typescript", "codegen"),
                now.minusHours(24),
                now.minusHours(24),
                ArticleMetadata.builder().stars(3400).language("English").build()
            ),

            // GitHub Repos - Infra/Cloud
            createArticle(
                ItemType.REPO, Source.github, Category.INFRA_CLOUD,
                "Terraform 모듈 라이브러리, AWS 인프라 구축 시간 90% 단축",
                "검증된 베스트 프랙티스를 적용한 재사용 가능한 모듈 모음입니다.",
                "Terraform Module Library - 90% Faster AWS Infrastructure Setup",
                "https://github.com/example/terraform-modules",
                1050,
                List.of("terraform", "aws", "infrastructure-as-code"),
                now.minusHours(6),
                now.minusHours(6),
                ArticleMetadata.builder().stars(7800).language("English").build()
            ),

            // Blog Posts - AI/LLM
            createArticle(
                ItemType.BLOG, Source.medium, Category.AI_LLM,
                "LLM 파인튜닝 완벽 가이드: 적은 데이터로 최대 성능 끌어내기",
                "LoRA와 QLoRA 기법을 활용하여 제한된 GPU 메모리 환경에서도 효율적인 파인튜닝이 가능합니다.",
                "Complete Guide to LLM Fine-Tuning: Maximum Performance with Minimal Data",
                "https://medium.com/example/llm-finetuning-guide",
                650,
                List.of("llm", "fine-tuning", "lora"),
                now.minusHours(10),
                now.minusHours(10),
                ArticleMetadata.builder().readTime("12분").build()
            ),

            createArticle(
                ItemType.BLOG, Source.devto, Category.AI_LLM,
                "RAG 시스템 구축 실전 가이드: 벡터 DB 선택부터 프롬프트 엔지니어링까지",
                null,
                "Practical Guide to Building RAG Systems: From Vector DB Selection to Prompt Engineering",
                "https://dev.to/example/rag-system-guide",
                580,
                List.of("rag", "vector-db", "prompt-engineering"),
                now.minusHours(15),
                now.minusHours(15),
                ArticleMetadata.builder().readTime("15분").build()
            ),

            // Blog Posts - DevOps/SRE
            createArticle(
                ItemType.BLOG, Source.hashnode, Category.DEVOPS_SRE,
                "GitOps로 멀티 클라우드 관리하기: ArgoCD + Flux 실전 적용기",
                "여러 클라우드 환경을 단일 Git 저장소로 관리하는 실제 사례를 소개합니다.",
                "Multi-Cloud Management with GitOps: ArgoCD + Flux in Production",
                "https://hashnode.com/example/gitops-multi-cloud",
                720,
                List.of("gitops", "argocd", "multi-cloud"),
                now.minusHours(7),
                now.minusHours(7),
                ArticleMetadata.builder().readTime("10분").build()
            ),

            // Blog Posts - Backend
            createArticle(
                ItemType.BLOG, Source.medium, Category.BACKEND,
                "마이크로서비스 아키텍처의 함정: 분산 트랜잭션 문제 해결하기",
                null,
                "Microservices Pitfalls: Solving Distributed Transaction Problems",
                "https://medium.com/example/microservices-transactions",
                490,
                List.of("microservices", "distributed-systems", "saga-pattern"),
                now.minusHours(20),
                now.minusHours(20),
                ArticleMetadata.builder().readTime("8분").build()
            ),

            // Discussions - Hacker News
            createArticle(
                ItemType.DISCUSSION, Source.hackernews, Category.AI_LLM,
                "OpenAI의 새로운 추론 모델, 코딩 벤치마크에서 인간 개발자 능가",
                null,
                "OpenAI's New Reasoning Model Surpasses Human Developers in Coding Benchmarks",
                "https://news.ycombinator.com/item?id=123456",
                1580,
                List.of("openai", "reasoning", "benchmarks"),
                now.minusHours(1),
                now.minusHours(1),
                ArticleMetadata.builder().comments(342).build()
            ),

            createArticle(
                ItemType.DISCUSSION, Source.hackernews, Category.BACKEND,
                "Rust가 시스템 프로그래밍의 미래인 이유: C++ 개발자의 전환 후기",
                null,
                "Why Rust is the Future of Systems Programming: A C++ Developer's Perspective",
                "https://news.ycombinator.com/item?id=123457",
                1240,
                List.of("rust", "systems-programming", "c++"),
                now.minusHours(4),
                now.minusHours(4),
                ArticleMetadata.builder().comments(287).build()
            ),

            createArticle(
                ItemType.DISCUSSION, Source.hackernews, Category.DEVOPS_SRE,
                "AWS 장애 포스트모템: 단일 리전 의존성의 위험성",
                null,
                "AWS Outage Postmortem: The Dangers of Single-Region Dependency",
                "https://news.ycombinator.com/item?id=123458",
                980,
                List.of("aws", "outage", "reliability"),
                now.minusHours(9),
                now.minusHours(9),
                ArticleMetadata.builder().comments(198).build()
            ),

            // Discussions - Reddit
            createArticle(
                ItemType.DISCUSSION, Source.reddit, Category.AI_LLM,
                "로컬 LLM 성능 비교: Llama 3 vs Mistral vs Gemma",
                "실제 환경에서 3가지 모델을 테스트한 결과, 각각의 장단점을 공유합니다.",
                "Local LLM Performance Comparison: Llama 3 vs Mistral vs Gemma",
                "https://reddit.com/r/LocalLLaMA/comments/example",
                850,
                List.of("llama", "mistral", "gemma"),
                now.minusHours(11),
                now.minusHours(11),
                ArticleMetadata.builder().upvotes(1240).comments(156).build()
            ),

            createArticle(
                ItemType.DISCUSSION, Source.reddit, Category.BACKEND,
                "PostgreSQL vs MySQL 2025년 버전 성능 벤치마크",
                null,
                "PostgreSQL vs MySQL 2025 Performance Benchmarks",
                "https://reddit.com/r/programming/comments/example2",
                720,
                List.of("postgresql", "mysql", "benchmark"),
                now.minusHours(16),
                now.minusHours(16),
                ArticleMetadata.builder().upvotes(890).comments(132).build()
            ),

            createArticle(
                ItemType.DISCUSSION, Source.reddit, Category.INFRA_CLOUD,
                "Kubernetes 운영 1년 회고: 배운 교훈과 실수들",
                null,
                "One Year of Kubernetes in Production: Lessons Learned and Mistakes Made",
                "https://reddit.com/r/devops/comments/example3",
                670,
                List.of("kubernetes", "production", "lessons-learned"),
                now.minusHours(22),
                now.minusHours(22),
                ArticleMetadata.builder().upvotes(780).comments(94).build()
            ),

            // Database
            createArticle(
                ItemType.REPO, Source.github, Category.DATABASE,
                "분산 SQL 데이터베이스, PostgreSQL 호환으로 무중단 스케일링 지원",
                "클라우드 네이티브 아키텍처로 설계된 분산 데이터베이스로 수평 확장이 자유롭습니다.",
                "Distributed SQL Database with PostgreSQL Compatibility and Zero-Downtime Scaling",
                "https://github.com/example/distributed-db",
                920,
                List.of("database", "distributed-systems", "postgresql"),
                now.minusHours(14),
                now.minusHours(14),
                ArticleMetadata.builder().stars(8500).language("English").build()
            ),

            createArticle(
                ItemType.BLOG, Source.devto, Category.DATABASE,
                "PostgreSQL 인덱싱 최적화 가이드: 쿼리 성능 10배 향상시키기",
                null,
                "PostgreSQL Indexing Optimization Guide: 10x Query Performance Improvement",
                "https://dev.to/example/postgres-indexing",
                540,
                List.of("postgresql", "indexing", "performance"),
                now.minusHours(13),
                now.minusHours(13),
                ArticleMetadata.builder().readTime("9분").build()
            ),

            // Blockchain
            createArticle(
                ItemType.REPO, Source.github, Category.BLOCKCHAIN,
                "Rust 기반 고성능 블록체인 노드, 이더리움 대비 50배 빠른 트랜잭션 처리",
                null,
                "High-Performance Blockchain Node in Rust - 50x Faster Than Ethereum",
                "https://github.com/example/fast-blockchain",
                1100,
                List.of("blockchain", "rust", "web3"),
                now.minusHours(19),
                now.minusHours(19),
                ArticleMetadata.builder().stars(9200).language("English").build()
            ),

            createArticle(
                ItemType.BLOG, Source.medium, Category.BLOCKCHAIN,
                "스마트 컨트랙트 보안 감사 체크리스트: 해킹 사례로 배우는 취약점",
                "실제 해킹 사례를 분석하여 스마트 컨트랙트 개발 시 주의해야 할 보안 취약점을 정리했습니다.",
                "Smart Contract Security Audit Checklist: Learning from Hacking Cases",
                "https://medium.com/example/smart-contract-security",
                630,
                List.of("blockchain", "security", "smart-contracts"),
                now.minusHours(17),
                now.minusHours(17),
                ArticleMetadata.builder().readTime("11분").build()
            ),

            // Security
            createArticle(
                ItemType.REPO, Source.github, Category.SECURITY,
                "오픈소스 취약점 스캐너, 코드 커밋 전 자동 보안 검사",
                "CI/CD 파이프라인에 통합 가능한 종합 보안 취약점 검사 도구입니다.",
                "Open Source Vulnerability Scanner for Automated Security Checks Before Commit",
                "https://github.com/example/security-scanner",
                1030,
                List.of("security", "devops", "vulnerability-scanning"),
                now.minusHours(21),
                now.minusHours(21),
                ArticleMetadata.builder().stars(7600).language("English").build()
            ),

            createArticle(
                ItemType.DISCUSSION, Source.hackernews, Category.SECURITY,
                "제로 트러스트 아키텍처 실전 도입기: 기존 시스템 마이그레이션 전략",
                null,
                "Zero Trust Architecture in Production: Migration Strategy from Legacy Systems",
                "https://news.ycombinator.com/item?id=123459",
                870,
                List.of("zero-trust", "security", "architecture"),
                now.minusHours(25),
                now.minusHours(25),
                ArticleMetadata.builder().comments(167).build()
            ),

            // Data Science
            createArticle(
                ItemType.REPO, Source.github, Category.DATA_SCIENCE,
                "Python 데이터 파이프라인 프레임워크, Airflow보다 가볍고 빠른 대안",
                null,
                "Lightweight Python Data Pipeline Framework - A Faster Alternative to Airflow",
                "https://github.com/example/data-pipeline",
                810,
                List.of("data-engineering", "python", "etl"),
                now.minusHours(27),
                now.minusHours(27),
                ArticleMetadata.builder().stars(5900).language("English").build()
            ),

            createArticle(
                ItemType.BLOG, Source.hashnode, Category.DATA_SCIENCE,
                "대규모 데이터셋 전처리 최적화: Pandas vs Polars 성능 비교",
                "10GB 이상의 대용량 데이터 처리 시 Pandas와 Polars의 성능을 실측했습니다.",
                "Large-Scale Data Preprocessing Optimization: Pandas vs Polars Performance",
                "https://hashnode.com/example/pandas-vs-polars",
                690,
                List.of("data-science", "pandas", "polars"),
                now.minusHours(23),
                now.minusHours(23),
                ArticleMetadata.builder().readTime("13분").build()
            ),

            // Architecture
            createArticle(
                ItemType.BLOG, Source.medium, Category.ARCHITECTURE,
                "마이크로서비스에서 모놀리스로: 역전환 사례 연구",
                "스타트업이 과도한 마이크로서비스 아키텍처에서 모놀리스로 돌아온 과정을 소개합니다.",
                "From Microservices Back to Monolith: A Case Study of Reverse Migration",
                "https://medium.com/example/microservices-to-monolith",
                1190,
                List.of("architecture", "microservices", "monolith"),
                now.minusHours(5),
                now.minusHours(5),
                ArticleMetadata.builder().readTime("14분").build()
            ),

            createArticle(
                ItemType.DISCUSSION, Source.hackernews, Category.ARCHITECTURE,
                "이벤트 드리븐 아키텍처의 복잡성: 언제 사용하고 언제 피해야 할까",
                null,
                "The Complexity of Event-Driven Architecture: When to Use and When to Avoid",
                "https://news.ycombinator.com/item?id=123460",
                950,
                List.of("event-driven", "architecture", "system-design"),
                now.minusHours(28),
                now.minusHours(28),
                ArticleMetadata.builder().comments(223).build()
            ),

            // Mobile
            createArticle(
                ItemType.REPO, Source.github, Category.MOBILE,
                "크로스 플랫폼 모바일 프레임워크, 네이티브 수준 성능 달성",
                "Flutter와 React Native의 단점을 극복한 새로운 크로스 플랫폼 프레임워크입니다.",
                "Cross-Platform Mobile Framework with Native-Level Performance",
                "https://github.com/example/mobile-framework",
                1070,
                List.of("mobile", "cross-platform", "flutter"),
                now.minusHours(30),
                now.minusHours(30),
                ArticleMetadata.builder().stars(11200).language("English").build()
            ),

            createArticle(
                ItemType.BLOG, Source.devto, Category.MOBILE,
                "iOS 앱 메모리 최적화 테크닉: 크래시 없는 안정적인 앱 만들기",
                null,
                "iOS App Memory Optimization Techniques: Building Crash-Free Stable Apps",
                "https://dev.to/example/ios-memory-optimization",
                580,
                List.of("ios", "swift", "performance"),
                now.minusHours(32),
                now.minusHours(32),
                ArticleMetadata.builder().readTime("10분").build()
            ),

            // Frontend
            createArticle(
                ItemType.REPO, Source.github, Category.FRONTEND,
                "Next.js 15 기반 대시보드 템플릿, 실시간 데이터 시각화 지원",
                null,
                "Next.js 15 Dashboard Template with Real-Time Data Visualization",
                "https://github.com/example/nextjs-dashboard",
                940,
                List.of("nextjs", "react", "dashboard"),
                now.minusHours(26),
                now.minusHours(26),
                ArticleMetadata.builder().stars(6800).language("English").build()
            ),

            createArticle(
                ItemType.BLOG, Source.medium, Category.FRONTEND,
                "React 성능 최적화 완벽 가이드: 렌더링 최소화 전략",
                "불필요한 리렌더링을 방지하고 초기 로딩 속도를 개선하는 실전 테크닉을 소개합니다.",
                "Complete React Performance Optimization Guide: Minimizing Render Strategies",
                "https://medium.com/example/react-performance",
                770,
                List.of("react", "performance", "optimization"),
                now.minusHours(29),
                now.minusHours(29),
                ArticleMetadata.builder().readTime("16분").build()
            ),

            createArticle(
                ItemType.DISCUSSION, Source.reddit, Category.FRONTEND,
                "Tailwind CSS vs CSS Modules: 2025년 실전 비교",
                null,
                "Tailwind CSS vs CSS Modules: A Practical Comparison in 2025",
                "https://reddit.com/r/webdev/comments/example4",
                820,
                List.of("tailwind", "css", "frontend"),
                now.minusHours(31),
                now.minusHours(31),
                ArticleMetadata.builder().upvotes(920).comments(178).build()
            )
        );

        articleRepository.saveAll(articles);
        log.info("Initialized {} articles", articles.size());
    }

    private Article createArticle(
            ItemType itemType,
            Source source,
            Category category,
            String summaryKoTitle,
            String summaryKoBody,
            String titleEn,
            String url,
            Integer score,
            List<String> tags,
            LocalDateTime createdAtSource,
            LocalDateTime createdAt,
            ArticleMetadata metadata
    ) {
        return Article.builder()
            .itemType(itemType)
            .source(source)
            .category(category)
            .summaryKoTitle(summaryKoTitle)
            .summaryKoBody(summaryKoBody)
            .titleEn(titleEn)
            .url(url)
            .score(score)
            .tags(tags)
            .createdAtSource(createdAtSource)
            .createdAt(createdAt)
            .updatedAt(createdAt)
            .metadata(metadata)
            .build();
    }
}
