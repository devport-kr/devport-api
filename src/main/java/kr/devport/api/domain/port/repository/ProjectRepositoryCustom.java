package kr.devport.api.domain.port.repository;

import kr.devport.api.domain.port.entity.Project;

import java.util.List;

public interface ProjectRepositoryCustom {
    List<Project> findHotProjects(int limit);
}
