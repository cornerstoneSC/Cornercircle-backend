package com.cornercircle.backend.homepage.repository;

import com.cornercircle.backend.homepage.model.HomepageContent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomepageRepository extends JpaRepository<HomepageContent, Long> {
}
