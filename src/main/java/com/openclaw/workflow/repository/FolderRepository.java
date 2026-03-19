package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<Folder, String> {

    List<Folder> findByParentIdIsNullOrderBySortOrderAsc();

    List<Folder> findByParentIdOrderBySortOrderAsc(String parentId);

    List<Folder> findAllByOrderBySortOrderAsc();

    long countByParentId(String parentId);
}