package kr.devport.api.repository;

import kr.devport.api.domain.entity.Benchmark;
import kr.devport.api.domain.enums.BenchmarkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BenchmarkRepository extends JpaRepository<Benchmark, BenchmarkType> {

    // Find by benchmark type (inherited from JpaRepository using BenchmarkType as ID)
    // findById(BenchmarkType type) is already available
}
